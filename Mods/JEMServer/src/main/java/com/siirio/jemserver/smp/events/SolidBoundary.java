package com.siirio.jemserver.smp.events;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.List;

public final class SolidBoundary {
    static final double WALL_THICKNESS = .25D;
    private static volatile List<EventBoundary> clientZones = List.of();

    public static void clientZones(List<EventBoundary> zones) {
        clientZones = zones.stream().filter(zone -> zone.type().equals("BOSS_FIGHT") || zone.type().equals("BLOOD_MOON") && zone.active()).toList();
    }

    static List<ServerPlayer> players(Entity entity) {
        return entity.getRootVehicle().getSelfAndPassengers().filter(ServerPlayer.class::isInstance).map(ServerPlayer.class::cast).toList();
    }

    private static List<EventBoundary> boundaries(Entity entity) {
        if (entity.isSpectator()) return List.of();
        if (entity.level().isClientSide) {
            if (!Boolean.TRUE.equals(net.minecraftforge.fml.DistExecutor.unsafeCallWhenOn(
                    net.minecraftforge.api.distmarker.Dist.CLIENT,
                    () -> () -> localMovementEntity(entity)))) return List.of();
            return clientZones.stream().filter(zone -> !zone.passable())
                    .filter(zone -> zone.dimension().equals(entity.level().dimension().location().toString())).toList();
        }
        List<EventBoundary> result = new ArrayList<>(StructureStaging.solidBoundaries(entity));
        result.addAll(BloodMoonSolidBoundary.boundaries(entity));
        return result;
    }

    private static boolean localMovementEntity(Entity entity) {
        net.minecraft.client.player.LocalPlayer player = net.minecraft.client.Minecraft.getInstance().player;
        return player != null && (player == entity || player.getRootVehicle() == entity);
    }

    public static List<VoxelShape> collisionShapes(Entity entity, AABB query) {
        List<EventBoundary> boundaries;
        if (entity.level().isClientSide) boundaries = boundaries(entity);
        else {
            boundaries = new ArrayList<>(StructureStaging.solidBoundaries(entity, query));
            boundaries.addAll(BloodMoonSolidBoundary.boundaries(entity));
        }
        return boundaries.stream().filter(boundary -> query.maxX >= boundary.minX() - WALL_THICKNESS
                        && query.minX <= boundary.maxX() + 1D + WALL_THICKNESS
                        && query.maxZ >= boundary.minZ() - WALL_THICKNESS
                        && query.minZ <= boundary.maxZ() + 1D + WALL_THICKNESS)
                .flatMap(boundary -> admissionShapes(boundary, movementBox(entity)).stream())
                .filter(shape -> shape.bounds().intersects(query)).toList();
    }

    public static boolean intersectsWall(AABB box, EventBoundary boundary) {
        return new Wall(boundary).shapes().stream().anyMatch(shape -> shape.bounds().intersects(box));
    }

    public static boolean permitsTeleport(Entity entity, net.minecraft.server.level.ServerLevel level, Vec3 destination) {
        List<EventBoundary> boundaries = new ArrayList<>(boundaries(entity));
        boundaries.addAll(StructureStaging.destinationBoundaries(entity, level, destination));
        boundaries = boundaries.stream().filter(boundary -> !boundary.type().equals("RESOURCE_RUSH")).toList();
        if (boundaries.isEmpty()) return true;
        AABB origin = movementBox(entity);
        AABB target = origin.move(destination.subtract(entity.position()));
        for (EventBoundary boundary : boundaries) {
            boolean sourceDimension = boundary.dimension().equals(entity.level().dimension().location().toString());
            boolean targetDimension = boundary.dimension().equals(level.dimension().location().toString());
            if (targetDimension && intersectsWall(target, boundary)
                    || (sourceDimension && contains(origin, boundary)) != (targetDimension && contains(target, boundary))) return false;
        }
        return true;
    }

    public static boolean permitsMovement(Entity entity, AABB origin, AABB target) {
        List<EventBoundary> relevant = entity.level().isClientSide ? boundaries(entity)
                : new ArrayList<>(StructureStaging.solidBoundaries(entity, origin.minmax(target)));
        if (!entity.level().isClientSide) relevant.addAll(BloodMoonSolidBoundary.boundaries(entity));
        return relevant.stream().allMatch(boundary -> {
            List<VoxelShape> walls = admissionShapes(boundary, origin);
            return walls.stream().noneMatch(shape -> shape.bounds().intersects(target))
                    && (!boundary.type().equals("BOSS_FIGHT") || !crossesShapes(origin, target, walls))
                    && (boundary.passable() || boundary.type().equals("RESOURCE_RUSH") || contains(origin, boundary) == contains(target, boundary));
        });
    }

    /** Slab intersection of a moving full box against each face, not an endpoint-only test. */
    static boolean crossesWall(AABB origin, AABB target, EventBoundary boundary) {
        return crossesShapes(origin, target, new Wall(boundary).shapes());
    }

    private static boolean crossesShapes(AABB origin, AABB target, List<VoxelShape> walls) {
        Vec3 start = origin.getCenter(), end = target.getCenter();
        double halfX = Math.max(origin.getXsize(), target.getXsize()) / 2;
        double halfY = Math.max(origin.getYsize(), target.getYsize()) / 2;
        double halfZ = Math.max(origin.getZsize(), target.getZsize()) / 2;
        return walls.stream().anyMatch(shape -> {
            AABB swept = shape.bounds().inflate(halfX - 1.0E-7, halfY - 1.0E-7, halfZ - 1.0E-7);
            return swept.contains(start) || swept.clip(start, end).isPresent();
        });
    }

    /** Clamp the complete mount/passenger family before vanilla resolves individual block collisions. */
    public static Vec3 limitMovement(Entity entity, Vec3 movement) {
        if (movement.lengthSqr() == 0 || entity.isPassenger()) return movement;
        AABB box = movementBox(entity);
        AABB admissionBox = box;
        List<EventBoundary> structural = entity.level().isClientSide ? boundaries(entity).stream().filter(boundary -> boundary.type().equals("BOSS_FIGHT")).toList()
                : StructureStaging.solidBoundaries(entity, box.expandTowards(movement));
        List<VoxelShape> walls = structural.stream().flatMap(boundary -> admissionShapes(boundary, admissionBox).stream()).toList();
        if (walls.isEmpty()) return movement;
        double y = Shapes.collide(net.minecraft.core.Direction.Axis.Y, box, walls, movement.y);
        box = box.move(0, y, 0);
        double x = Shapes.collide(net.minecraft.core.Direction.Axis.X, box, walls, movement.x);
        box = box.move(x, 0, 0);
        double z = Shapes.collide(net.minecraft.core.Direction.Axis.Z, box, walls, movement.z);
        return new Vec3(x, y, z);
    }

    /** During entry omit only the approached face. The far face still stops a fast entrant. */
    static List<VoxelShape> admissionShapes(EventBoundary boundary, AABB source) {
        List<VoxelShape> walls = new Wall(boundary).shapes();
        if (!boundary.passable() || !boundary.type().equals("BOSS_FIGHT")) return walls;
        List<VoxelShape> result = new ArrayList<>(6);
        if (source.minX >= boundary.minX()) result.add(walls.get(0));
        if (source.maxX <= boundary.maxX()+1D) result.add(walls.get(1));
        if (source.minZ >= boundary.minZ()) result.add(walls.get(2));
        if (source.maxZ <= boundary.maxZ()+1D) result.add(walls.get(3));
        if (source.minY >= boundary.minY()+WALL_THICKNESS) result.add(walls.get(4));
        if (source.maxY <= boundary.maxY()+1D-WALL_THICKNESS) result.add(walls.get(5));
        return result;
    }

    static boolean contained(Entity entity) {
        AABB box = movementBox(entity);
        return boundaries(entity).stream().filter(boundary -> !boundary.type().equals("RESOURCE_RUSH"))
                .anyMatch(boundary -> contains(box, boundary));
    }

    static AABB movementBox(Entity entity) {
        Entity root = entity.getRootVehicle();
        return root.getSelfAndPassengers().map(Entity::getBoundingBox).reduce(root.getBoundingBox(), AABB::minmax);
    }

    static AABB destinationBox(Entity entity, Vec3 destination) {
        return movementBox(entity).move(destination.subtract(entity.position()));
    }

    static boolean contains(AABB box, EventBoundary boundary) {
        return box.minX >= boundary.minX() && box.maxX <= boundary.maxX() + 1D
                && box.minZ >= boundary.minZ() && box.maxZ <= boundary.maxZ() + 1D
                && (!boundary.type().equals("BOSS_FIGHT")
                    || box.minY >= boundary.minY() && box.maxY <= boundary.maxY() + 1D);
    }

    static EventBoundary boundary(java.util.UUID id, String dimension, int minX, int minZ, int maxX, int maxZ,
                                  int minY, int maxY, int color, boolean passable) {
        return new EventBoundary(id, dimension, "BOSS_FIGHT", minX, minY, minZ, maxX, maxY, maxZ, color, true, passable);
    }

    private record Wall(EventBoundary boundary) {
        private List<VoxelShape> shapes() {
            double minX = boundary.minX();
            double minZ = boundary.minZ();
            double maxX = boundary.maxX() + 1D;
            double maxZ = boundary.maxZ() + 1D;
            double minY = boundary.minY();
            double maxY = boundary.maxY() + 1D;
            List<VoxelShape> result = new ArrayList<>(6);
            result.add(Shapes.create(new AABB(minX - WALL_THICKNESS, minY, minZ - WALL_THICKNESS, minX, maxY, maxZ + WALL_THICKNESS)));
            result.add(Shapes.create(new AABB(maxX, minY, minZ - WALL_THICKNESS, maxX + WALL_THICKNESS, maxY, maxZ + WALL_THICKNESS)));
            result.add(Shapes.create(new AABB(minX, minY, minZ - WALL_THICKNESS, maxX, maxY, minZ)));
            result.add(Shapes.create(new AABB(minX, minY, maxZ, maxX, maxY, maxZ + WALL_THICKNESS)));
            if (boundary.type().equals("BOSS_FIGHT")) {
                result.add(Shapes.create(new AABB(minX - WALL_THICKNESS, minY, minZ - WALL_THICKNESS,
                        maxX + WALL_THICKNESS, minY + WALL_THICKNESS, maxZ + WALL_THICKNESS)));
                result.add(Shapes.create(new AABB(minX - WALL_THICKNESS, maxY - WALL_THICKNESS, minZ - WALL_THICKNESS,
                        maxX + WALL_THICKNESS, maxY, maxZ + WALL_THICKNESS)));
            }
            return result;
        }

    }

    private SolidBoundary() {}
}
