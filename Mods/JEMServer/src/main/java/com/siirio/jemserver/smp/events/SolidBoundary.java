package com.siirio.jemserver.smp.events;

import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.List;

public final class SolidBoundary {
    private static final double WALL_THICKNESS = .25D;
    private static volatile List<EventBoundary> clientZones = List.of();

    public static void clientZones(List<EventBoundary> zones) {
        clientZones = zones.stream().filter(zone -> zone.type().equals("BOSS_FIGHT")
                || zone.type().equals("BLOOD_MOON") && zone.active()).toList();
    }

    static List<ServerPlayer> players(Entity entity) {
        return entity.getSelfAndPassengers().filter(ServerPlayer.class::isInstance).map(ServerPlayer.class::cast).toList();
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

    public static Vec3 collide(Entity entity, Vec3 movement) {
        if (movement.lengthSqr() == 0) return movement;
        Vec3 constrained = movement;
        for (EventBoundary boundary : boundaries(entity)) {
            Wall wall = new Wall(boundary);
            constrained = boundary.type().equals("RESOURCE_RUSH")
                    ? wall.collideResourceRush(entity.getBoundingBox(), constrained)
                    : wall.collide(entity.getBoundingBox(), constrained);
            if (constrained.lengthSqr() == 0) break;
        }
        return constrained;
    }

    public static List<VoxelShape> collisionShapes(Entity entity, AABB query) {
        return boundaries(entity).stream().flatMap(boundary -> new Wall(boundary).shapes().stream())
                .filter(shape -> shape.bounds().intersects(query)).toList();
    }

    public static boolean intersectsWall(AABB box, EventBoundary boundary) {
        return new Wall(boundary).shapes().stream().anyMatch(shape -> shape.bounds().intersects(box));
    }

    public static boolean permitsTeleport(Entity entity, Vec3 destination) {
        AABB origin = entity.getBoundingBox();
        AABB target = origin.move(destination.subtract(entity.position()));
        return boundaries(entity).stream().filter(boundary -> !boundary.type().equals("RESOURCE_RUSH"))
                .allMatch(boundary -> !intersectsWall(target, boundary)
                && contains(origin, boundary) == contains(target, boundary));
    }

    private static boolean contains(AABB box, EventBoundary boundary) {
        return box.minX >= boundary.minX() && box.maxX <= boundary.maxX() + 1D
                && box.minZ >= boundary.minZ() && box.maxZ <= boundary.maxZ() + 1D;
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
            List<VoxelShape> result = new ArrayList<>(4);
            result.add(Shapes.create(new AABB(minX - WALL_THICKNESS, minY, minZ - WALL_THICKNESS, minX, maxY, maxZ + WALL_THICKNESS)));
            result.add(Shapes.create(new AABB(maxX, minY, minZ - WALL_THICKNESS, maxX + WALL_THICKNESS, maxY, maxZ + WALL_THICKNESS)));
            result.add(Shapes.create(new AABB(minX, minY, minZ - WALL_THICKNESS, maxX, maxY, minZ)));
            result.add(Shapes.create(new AABB(minX, minY, maxZ, maxX, maxY, maxZ + WALL_THICKNESS)));
            return result;
        }

        private Vec3 collideResourceRush(AABB box, Vec3 movement) {
            List<VoxelShape> shapes = shapes();
            double x = Shapes.collide(Direction.Axis.X, box, shapes, movement.x);
            if (x != 0) box = box.move(x, 0, 0);
            double z = Shapes.collide(Direction.Axis.Z, box, shapes, movement.z);
            return new Vec3(x, movement.y, z);
        }

        private Vec3 collide(AABB box, Vec3 movement) {
            List<VoxelShape> shapes = shapes();
            AABB swept = box.expandTowards(movement).inflate(WALL_THICKNESS);
            if (shapes.stream().noneMatch(shape -> shape.bounds().intersects(swept))) return movement;
            double y = Shapes.collide(Direction.Axis.Y, box, shapes, movement.y);
            if (y != 0) box = box.move(0, y, 0);
            boolean zFirst = Math.abs(movement.x) < Math.abs(movement.z);
            double first = Shapes.collide(zFirst ? Direction.Axis.Z : Direction.Axis.X, box, shapes, zFirst ? movement.z : movement.x);
            if (first != 0) box = zFirst ? box.move(0, 0, first) : box.move(first, 0, 0);
            double second = Shapes.collide(zFirst ? Direction.Axis.X : Direction.Axis.Z, box, shapes, zFirst ? movement.x : movement.z);
            return zFirst ? new Vec3(second, y, first) : new Vec3(first, y, second);
        }
    }

    private SolidBoundary() {}
}
