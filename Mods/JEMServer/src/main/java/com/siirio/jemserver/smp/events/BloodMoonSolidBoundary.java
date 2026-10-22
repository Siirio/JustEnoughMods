package com.siirio.jemserver.smp.events;

import com.siirio.jemserver.smp.*;

import com.siirio.jemserver.smp.SmpData;
import com.siirio.jemserver.smp.SmpRecords;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public final class BloodMoonSolidBoundary {
    public static boolean locked(Entity entity,EventState row) {
        if(!row.combatStarted()) return false;
        EventSession session=new EventSession(row);
        List<ServerPlayer> players=SolidBoundary.players(entity);
        if(!players.isEmpty()) return players.stream().anyMatch(player->session.accepted(player.getUUID())
                &&!row.memberOrEmpty(player.getUUID()).awaitingReturn());
        return entity instanceof Mob mob&&BloodMoon.belongs(row,mob);
    }

    static List<EventBoundary> boundaries(Entity entity) {
        if(!(entity.level() instanceof ServerLevel level)||!EventRuntime.enabled(level.getServer())) return List.of();
        List<EventBoundary> boundaries=new ArrayList<>();
        for(var row:SmpData.get(level.getServer()).events().snapshots()) {
            if(!row.state().equals("ACTIVE")||!row.dimension().equals(level.dimension().location().toString())) continue;
            String type=row.activity();
            if(type.equals("BLOOD_MOON")) {
                if(!locked(entity,row)) continue;
            } else if(type.equals("RESOURCE_RUSH")) {
                if(!(entity instanceof Mob)||level.players().stream().noneMatch(player->EventRegions.contains(row,level,player.blockPosition()))) continue;
            } else continue;
            boundaries.add(boundary(row,level));
        }
        return boundaries;
    }

    public static BossTeleportSafety.Decision validateTeleport(ServerPlayer player,ServerLevel level,Vec3 requested) {
        var serverForMutation = player.server;
        if (serverForMutation == null || serverForMutation.overworld() == null) { return validateTeleportInScope(player, level, requested); }
        var store = com.siirio.jemserver.smp.SmpData.get(serverForMutation);
        return store.edit(() -> validateTeleportInScope(player, level, requested));
    }

    private static BossTeleportSafety.Decision validateTeleportInScope(ServerPlayer player,ServerLevel level,Vec3 requested) {
        if (!EventRuntime.enabled(player.server)) return BossTeleportSafety.Decision.unmatched();
        for(var row:SmpData.get(player.server).events().currentRows()) {
            if(!row.state().equals("ACTIVE")||!row.activity().equals("BLOOD_MOON")
                    ||!locked(player,row)) continue;
            if(validDestination(player,level,row,requested)) return BossTeleportSafety.Decision.allow(requested);
            return BossTeleportSafety.Decision.redirect(safeDestination(player,row));
        }
        return BossTeleportSafety.Decision.unmatched();
    }

    private static Vec3 safeDestination(ServerPlayer player,EventState row) {
        ServerLevel level=EventRegions.level(player.server,row);
        if(level==null) return null;
        Vec3 current=player.position();
        if(player.serverLevel()==level&&validDestination(player,level,row,current)) return current;
        var member=row.memberOrEmpty(player.getUUID());
        if(member.hasSafeX()) {
            Vec3 saved=new Vec3(member.safeX(),member.safeY(),member.safeZ());
            if(validDestination(player,level,row,saved)) return saved;
        }
        BlockPos respawn=new EncounterContext(row).respawnPoint(level);
        Vec3 saved=respawn==null?null:Vec3.atBottomCenterOf(respawn);
        return saved!=null&&validDestination(player,level,row,saved)?saved:null;
    }

    private static boolean validDestination(ServerPlayer player,ServerLevel level,EventState row,Vec3 destination) {
        if(level!=EventRegions.level(player.server,row)) return false;
        BlockPos position=BlockPos.containing(destination);
        if(!level.hasChunkAt(position)||!level.getWorldBorder().isWithinBounds(position)) return false;
        AABB box=SolidBoundary.destinationBox(player,destination);
        EventBoundary boundary=boundary(row,level);
        return EventRegions.contains(row,level,box)&&!SolidBoundary.intersectsWall(box,boundary)&&level.noCollision(player,box);
    }

    private static EventBoundary boundary(EventState row,ServerLevel level) {
        return new EventBoundary(row.id(),row.dimension(),row.activity(),
                EventRegions.minX(row),level.getMinBuildHeight(),EventRegions.minZ(row),EventRegions.maxX(row),
                level.getMaxBuildHeight()-1,EventRegions.maxZ(row),EventRules.BLOOD_COLOR.get(),true,false);
    }

    private BloodMoonSolidBoundary() {}
}
