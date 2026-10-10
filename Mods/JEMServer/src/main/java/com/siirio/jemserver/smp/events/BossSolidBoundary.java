package com.siirio.jemserver.smp.events;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.List;

public final class BossSolidBoundary {
    private static final double WALL_THICKNESS = .25D;
    private static volatile List<EventBoundary> clientZones = List.of();

    public static void clientZones(List<EventBoundary> zones) {
        clientZones = zones.stream().filter(zone -> zone.type().equals("BOSS_FIGHT")).toList();
    }

    public static Vec3 collide(Entity entity, Vec3 movement) {
        if (entity.isSpectator() || movement.lengthSqr() == 0) return movement;
        if (entity.level().isClientSide && !Boolean.TRUE.equals(net.minecraftforge.fml.DistExecutor.unsafeCallWhenOn(
                net.minecraftforge.api.distmarker.Dist.CLIENT,
                () -> () -> net.minecraft.client.Minecraft.getInstance().player == entity))) return movement;
        List<Wall> walls = entity.level().isClientSide
                ? clientZones.stream().filter(zone -> !zone.passable())
                .filter(zone -> zone.dimension().equals(entity.level().dimension().location().toString()))
                .map(Wall::new).toList()
                : StructureStaging.solidBoundaries(entity).stream().map(Wall::new).toList();
        Vec3 constrained = movement;
        for (Wall wall : walls) {
            constrained = wall.collide(entity.getBoundingBox(), constrained);
            if (constrained.lengthSqr() == 0) break;
        }
        return constrained;
    }

    public static boolean intersectsWall(AABB box, EventBoundary boundary) {
        return new Wall(boundary).shapes().stream().anyMatch(shape -> shape.bounds().intersects(box));
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

    private BossSolidBoundary() {}
}
