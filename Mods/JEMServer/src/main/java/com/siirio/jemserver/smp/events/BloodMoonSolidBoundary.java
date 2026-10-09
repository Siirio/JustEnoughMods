package com.siirio.jemserver.smp.events;

import com.siirio.jemserver.smp.Parties;
import com.siirio.jemserver.smp.SmpData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;

import java.util.List;

public final class BloodMoonSolidBoundary {
    private static final double THICKNESS = .25;
    private static volatile List<EventNetwork.Boundary> clientZones = List.of();

    public static void clientZones(List<EventNetwork.Boundary> zones) {
        clientZones = List.copyOf(zones);
    }

    public static Vec3 collide(Entity entity, Vec3 movement) {
        if (!(entity instanceof net.minecraft.world.entity.player.Player) || entity.isSpectator()) return movement;
        if (entity instanceof ServerPlayer player) {
            for (var row : SmpData.get(player.server).all("events")) {
                if (!solid(player, row)) continue;
                Vec3 constrained = collide(entity, movement, EventRegions.minX(row), player.serverLevel().getMinBuildHeight(),
                        EventRegions.minZ(row), EventRegions.maxX(row), player.serverLevel().getMaxBuildHeight() - 1, EventRegions.maxZ(row));
                if (!constrained.equals(movement)) return constrained;
            }
            return movement;
        }
        String dimension = entity.level().dimension().location().toString();
        for (var zone : clientZones) {
            if (!zone.type().equals("BLOOD_MOON") || zone.passable() || !zone.dimension().equals(dimension)) continue;
            Vec3 constrained = collide(entity, movement, zone.minX(), zone.minY(), zone.minZ(), zone.maxX(), zone.maxY(), zone.maxZ());
            if (!constrained.equals(movement)) return constrained;
        }
        return movement;
    }

    private static boolean solid(ServerPlayer player, CompoundTag row) {
        if (!row.getString("state").equals("ACTIVE") || !row.getString("activity").equals("BLOOD_MOON")
                || !row.getString("dimension").equals(player.level().dimension().location().toString())) return false;
        if (row.getBoolean("combatStarted")) return false;
        CompoundTag party = row.hasUUID("party") ? SmpData.get(player.server).find("parties", row.getUUID("party")) : null;
        boolean participant = party != null && party.getString("state").equals("ACTIVE") && Parties.accepted(party, player.getUUID());
        return !participant;
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
        AABB box=entity.getBoundingBox();
        double x=Shapes.collide(Direction.Axis.X,box,walls,movement.x);
        if(x!=0) box=box.move(x,0,0);
        double z=Shapes.collide(Direction.Axis.Z,box,walls,movement.z);
        return new Vec3(x,movement.y,z);
    }

    private BloodMoonSolidBoundary() {}
}
