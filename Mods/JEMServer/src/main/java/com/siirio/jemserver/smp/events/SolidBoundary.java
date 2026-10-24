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
                .flatMap(boundary -> new Wall(boundary).shapes().stream())
                .filter(shape -> shape.bounds().intersects(query)).toList();
    }

    public static boolean intersectsWall(AABB box, EventBoundary boundary) {
        return new Wall(boundary).shapes().stream().anyMatch(shape -> shape.bounds().intersects(box));
    }

    public static boolean permitsTeleport(Entity entity, net.minecraft.server.level.ServerLevel level, Vec3 destination) {
        List<EventBoundary> boundaries = boundaries(entity).stream().filter(boundary -> !boundary.type().equals("RESOURCE_RUSH")).toList();
        if (boundaries.isEmpty()) return true;
        AABB origin = movementBox(entity);
        if (entity.level() != level) return boundaries.stream().noneMatch(boundary -> contains(origin, boundary));
        AABB target = origin.move(destination.subtract(entity.position()));
        return boundaries.stream().allMatch(boundary -> !intersectsWall(target, boundary)
                && contains(origin, boundary) == contains(target, boundary));
    }

    public static boolean permitsMovement(Entity entity, AABB origin, AABB target) {
        return boundaries(entity).stream().allMatch(boundary -> !intersectsWall(target, boundary)
                && (boundary.type().equals("RESOURCE_RUSH") || contains(origin, boundary) == contains(target, boundary)));
    }

    static boolean contained(Entity entity) {
        AABB box = movementBox(entity);
        return boundaries(entity).stream().filter(boundary -> !boundary.type().equals("RESOURCE_RUSH"))
                .anyMatch(boundary -> contains(box, boundary));
    }

    static AABB movementBox(Entity entity) {
        return entity.getRootVehicle().getBoundingBox().minmax(entity.getBoundingBox());
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
