package com.siirio.jemserver.smp.events;

import com.siirio.jemserver.smp.SmpData;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;

import java.util.List;

public final class BloodMoonSolidBoundary {
    private static final double THICKNESS = .25D;

    public static void clientZones(List<EventNetwork.Boundary> zones) {}

    public static Vec3 collide(Entity entity, Vec3 movement) {
        if (!(entity instanceof Mob) || !(entity.level() instanceof ServerLevel level) || entity.isSpectator()) return movement;
        for (var row : SmpData.get(level.getServer()).all("events")) {
            if (!row.getString("state").equals("ACTIVE") || !row.getString("dimension").equals(level.dimension().location().toString())) continue;
            String type = row.getString("activity");
            if (type.equals("BLOOD_MOON") && !row.getBoolean("combatStarted")) continue;
            if (type.equals("RESOURCE_RUSH") && level.players().stream().noneMatch(player -> EventRegions.contains(row, level, player.blockPosition()))) continue;
            if (!type.equals("BLOOD_MOON") && !type.equals("RESOURCE_RUSH")) continue;
            Vec3 constrained = collide(entity, movement, EventRegions.minX(row), level.getMinBuildHeight(), EventRegions.minZ(row),
                    EventRegions.maxX(row), level.getMaxBuildHeight() - 1, EventRegions.maxZ(row));
            if (!constrained.equals(movement)) return constrained;
        }
        return movement;
    }

    private static Vec3 collide(Entity entity, Vec3 movement, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        double outerMaxX = maxX + 1D;
        double outerMaxZ = maxZ + 1D;
        double top = maxY + 1D;
        var walls = List.of(
                Shapes.create(new AABB(minX - THICKNESS, minY, minZ - THICKNESS, minX, top, outerMaxZ + THICKNESS)),
                Shapes.create(new AABB(outerMaxX, minY, minZ - THICKNESS, outerMaxX + THICKNESS, top, outerMaxZ + THICKNESS)),
                Shapes.create(new AABB(minX, minY, minZ - THICKNESS, outerMaxX, top, minZ)),
                Shapes.create(new AABB(minX, minY, outerMaxZ, outerMaxX, top, outerMaxZ + THICKNESS)));
        AABB box = entity.getBoundingBox();
        double x = Shapes.collide(Direction.Axis.X, box, walls, movement.x);
        if (x != 0) box = box.move(x, 0, 0);
        double z = Shapes.collide(Direction.Axis.Z, box, walls, movement.z);
        return new Vec3(x, movement.y, z);
    }

    private BloodMoonSolidBoundary() {}
}
