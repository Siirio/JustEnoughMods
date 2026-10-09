package com.siirio.jemserver.smp;

import com.siirio.jemworldbosstiers.api.*;
import com.siirio.jemworldbosstiers.event.HostedEncounterClosedEvent;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import java.util.*;

public final class HostedParties {
    public static void start(ServerPlayer host, CompoundTag row) {
        SmpRecords.owner(host, row);
        if (row.getString("state").equals("ACTIVE")) return;
        String reason = startReason(host, row);
        SmpRecords.require(reason.isEmpty(), reason);
        if (row.hasUUID("eventId")) {
            var event = SmpData.get(host.server).find("events", row.getUUID("eventId"));
            row.putString("dimension", event.getString("dimension"));
            row.putLong("position", event.getLong("position"));
        }
        SmpRecords.require(row.getString("activity").equals("BOSS"),"unavailable");
        var ids =
                SmpRecords.memberIds(row).stream()
                        .filter(id -> Parties.accepted(row, id))
                        .filter(
                                id -> {
                                    var player = host.server.getPlayerList().getPlayer(id);
                                    return player != null;
                                })
                        .toList();
        SmpRecords.require(ids.contains(host.getUUID()), "host_must_be_present");
        String arenaReason = com.siirio.jemserver.smp.events.StructureStaging.startReason(host, row, ids);
        SmpRecords.require(arenaReason.isEmpty(), arenaReason);
        com.siirio.jemserver.smp.events.PartyTeleportFlow.begin(host,row,ids);
        if(row.getBoolean("bloodMoon")) {
            com.siirio.jemserver.smp.events.EventParty.start(host,row,ids);
            return;
        }
        com.siirio.jemserver.smp.events.HostedBoundary.captureEntrances(host.server,row,ids);
        if (row.getBoolean("raid")) {
            com.siirio.jemserver.smp.events.RaidEvent.start(host, row, ids);
            row.putString("state", "ACTIVE");
            Profiles.count(host.server, host.getUUID(), "bossesHosted");
            SmpData.get(host.server).changed(row);
            return;
        }
        if(com.siirio.jemserver.smp.events.StructureStaging.start(host,row,ids)) return;
        var level = host.serverLevel();
        LivingEntity boss = null;
        if (row.hasUUID("bossEntity")
                && level.getEntity(row.getUUID("bossEntity")) instanceof LivingEntity found)
            boss = found;
        if (boss == null) {
            var center = BlockPos.of(row.getLong("position"));
            var candidates =
                    level.getEntitiesOfClass(
                            LivingEntity.class,
                            new AABB(center).inflate(SmpConfig.ENCOUNTER_RADIUS.get()),
                            entity -> entity.isAlive() && WorldTierApi.profile(entity).isPresent());
            SmpRecords.require(candidates.size() == 1, "one_awakened_boss_required");
            boss = candidates.get(0);
        }
        boss.getPersistentData().putBoolean("jem_solo", row.getBoolean("solo"));
        var bounds=com.siirio.jemserver.smp.events.StructureStaging.prepareCombat(level,row,boss);
        SmpRecords.require(HostedEncounterApi.start(boss, ids, false, bounds), "unavailable");
        row.putUUID("bossEntity", boss.getUUID());
        row.putString("state", "ACTIVE");
        Profiles.count(host.server, host.getUUID(), "bossesHosted");
        SmpData.get(host.server).changed(row);
    }

    public static void closed(HostedEncounterClosedEvent event) {
        var server = event.boss().getServer();
        if (server == null) return;
        var data = SmpData.get(server);
        for (var row : data.all("parties"))
            if (row.hasUUID("bossEntity")
                    && row.getUUID("bossEntity").equals(event.boss().getUUID())
                    && !SmpData.closed(row)) {
                if(!event.success()) notifyDefeat(server,row,event.raid());
                row.putString("state", event.success() ? "COMPLETED" : "FAILED");
                data.changed(row);
                com.siirio.jemserver.smp.events.StructureStaging.finish(server, row);
                if(!event.success()&&!event.raid()&&row.hasUUID("structureId")) server.execute(()->{
                    if(event.boss().level() instanceof net.minecraft.server.level.ServerLevel level)
                        com.siirio.jemserver.smp.events.StructureStaging.stageBoss(level,row,event.boss());
                });
            }
        if (event.success())
            for (UUID id : event.eligibleParticipants()) {
                Profiles.count(server, id, event.raid() ? "raidsCompleted" : "bossAssists");
                Profiles.countEvent(server, id, event.raid() ? "BOSS_RAID" : "BOSS");
            }
        for (var row : data.all("events"))
            if (row.hasUUID("bossEntity")
                    && row.getUUID("bossEntity").equals(event.boss().getUUID())
                    && row.getString("state").equals("ACTIVE"))
                if(event.success()) com.siirio.jemserver.smp.events.EventScheduler.finish(server,row,"COMPLETED");
                else com.siirio.jemserver.smp.events.EventScheduler.failAttempt(server,row);
    }

    private static void notifyDefeat(net.minecraft.server.MinecraftServer server,CompoundTag party,boolean raid) {
        for(UUID id:SmpRecords.memberIds(party)) {
            if(!Parties.accepted(party,id)) continue;
            var player=server.getPlayerList().getPlayer(id);
            if(player==null) continue;
            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("jem.event.defeat_title")
                    .withStyle(net.minecraft.ChatFormatting.RED,net.minecraft.ChatFormatting.BOLD));
            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(raid?"jem.event.defeat_retry.raid":"jem.event.defeat_retry.boss")
                    .withStyle(net.minecraft.ChatFormatting.GOLD));
        }
    }

    public static void tick(net.minecraft.server.MinecraftServer server) {
        if (server.getTickCount() % 20 != 0) return;
        var data = SmpData.get(server);
        long now = System.currentTimeMillis();
        for (var row : data.all("parties")) {
            if (SmpData.closed(row) || row.getString("state").equals("ACTIVE") || !row.hasUUID("bossEntity")) continue;
            var level = server.getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, new net.minecraft.resources.ResourceLocation(row.getString("dimension"))));
            if (level != null && level.getEntity(row.getUUID("bossEntity")) instanceof LivingEntity boss && boss.isAlive()) HostedEncounterApi.hold(boss);
        }

        for (var row : data.all("parties"))
            if (!SmpData.closed(row)
                    && !row.getString("state").equals("ACTIVE")
                    && now - row.getLong("created") > SmpConfig.PARTY_WAIT_MINUTES.get() * 60000L) {
                release(server, row);
                row.putString("state", "CLOSED");
                data.changed(row);
            }
    }

    public static void arrive(ServerPlayer player, CompoundTag row) {
        SmpRecords.require(!row.getString("state").equals("ACTIVE") && !SmpData.closed(row), "combat_locked");
        SmpRecords.require(Parties.accepted(row,player.getUUID()),"not_member");
        if (row.getString("activity").equals("BOSS")&&com.siirio.jemserver.smp.events.StructureStaging.arrive(player, row)) return;
        if(row.getBoolean("bloodMoon")&&row.hasUUID("eventId")) {
            var event=SmpData.get(player.server).find("events",row.getUUID("eventId"));
            SmpRecords.require(event!=null,"unavailable");
            var targetDimension=net.minecraft.resources.ResourceLocation.tryParse(event.getString("dimension"));
            var targetLevel=targetDimension==null?null:player.server.getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,targetDimension));
            SmpRecords.require(targetLevel!=null,"unavailable");
            var outside=com.siirio.jemserver.smp.events.EventRegions.safeOutside(targetLevel,event,BlockPos.of(event.getLong("position"))).orElseThrow(()->new IllegalArgumentException("no_safe_arrival"));
            player.teleportTo(targetLevel,outside.getX()+.5,outside.getY(),outside.getZ()+.5,player.getYRot(),player.getXRot());
            player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
            player.fallDistance=0;
            return;
        }
        var host=player.server.getPlayerList().getPlayer(row.getUUID("owner"));
        var dimension=net.minecraft.resources.ResourceLocation.tryParse(row.getString("activity").equals("BOSS")?row.getString("dimension")
                :host==null?"":host.serverLevel().dimension().location().toString());
        var level=dimension==null?null:player.server.getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,dimension));
        SmpRecords.require(level!=null,"unavailable");
        var origin = row.getString("activity").equals("BOSS")?BlockPos.of(row.getLong("position")):host.blockPosition();
        for (var pos : BlockPos.betweenClosed(origin.offset(-2, -1, -2), origin.offset(2, 2, 2))) {
            double x = pos.getX() + 0.5, y = pos.getY(), z = pos.getZ() + 0.5;
            var box = player.getDimensions(player.getPose()).makeBoundingBox(x, y, z);
            if (!level.hasChunkAt(pos) || !level.getWorldBorder().isWithinBounds(pos)
                    || !level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), net.minecraft.core.Direction.UP)
                    || !level.getFluidState(pos).isEmpty() || !level.getFluidState(pos.above()).isEmpty()
                    || !level.noCollision(player, box)) continue;
            player.teleportTo(level, x, y, z, player.getYRot(), player.getXRot());
            player.fallDistance = 0;
            return;
        }
        throw new IllegalArgumentException("no_safe_arrival");
    }

    public static void release(net.minecraft.server.MinecraftServer server, CompoundTag row) {
        if (!row.hasUUID("bossEntity")) return;
        var level = server.getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, new net.minecraft.resources.ResourceLocation(row.getString("dimension"))));
        if (level != null && level.getEntity(row.getUUID("bossEntity")) instanceof LivingEntity boss) HostedEncounterApi.release(boss);
    }

    public static void forfeit(ServerPlayer player,UUID partyId) {
        CompoundTag row=SmpData.get(player.server).find("parties",partyId);
        SmpRecords.require(row!=null&&!SmpData.closed(row)&&row.getString("state").equals("ACTIVE")
                &&row.getString("activity").equals("BOSS"),"unavailable");
        SmpRecords.owner(player,row);
        SmpRecords.require(com.siirio.jemserver.smp.events.HostedBoundary.canForfeit(player,row),"unavailable");
        if(row.hasUUID("eventId")) {
            var event=SmpData.get(player.server).find("events",row.getUUID("eventId"));
            SmpRecords.require(event!=null&&event.getString("state").equals("ACTIVE"),"unavailable");
            com.siirio.jemserver.smp.events.EventScheduler.failAttempt(player.server,event);
            return;
        }
        var dimension=net.minecraft.resources.ResourceLocation.tryParse(row.getString("dimension"));
        var level=dimension==null?null:player.server.getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,dimension));
        row.putBoolean("boundaryForfeit",true);
        SmpData.get(player.server).changed(row);
        LivingEntity boss=level!=null&&row.hasUUID("bossEntity")&&level.getEntity(row.getUUID("bossEntity")) instanceof LivingEntity living?living:null;
        if(boss!=null&&HostedEncounterApi.cancel(boss)) return;
        if(boss!=null) HostedEncounterApi.release(boss);
        notifyDefeat(player.server,row,row.getBoolean("raid"));
        row.putString("state","FAILED");
        SmpData.get(player.server).changed(row);
        com.siirio.jemserver.smp.events.StructureStaging.finish(player.server,row);
    }

    public static void leaveEncounter(ServerPlayer player,UUID partyId) {
        CompoundTag row=SmpData.get(player.server).find("parties",partyId);
        SmpRecords.require(row!=null&&!SmpData.closed(row)&&row.getString("state").equals("ACTIVE")
                &&row.getString("activity").equals("BOSS"),"unavailable");
        SmpRecords.require(Parties.accepted(row,player.getUUID()),"not_member");
        SmpRecords.require(!row.getUUID("owner").equals(player.getUUID()),"host_only");
        SmpRecords.require(com.siirio.jemserver.smp.events.HostedBoundary.canForfeit(player,row),"unavailable");
        if(row.hasUUID("eventId")) {
            var event=SmpData.get(player.server).find("events",row.getUUID("eventId"));
            SmpRecords.require(event!=null&&event.getString("state").equals("ACTIVE"),"unavailable");
            var member=SmpRecords.members(event).getCompound(player.getStringUUID());
            member.putBoolean("eliminated",true);
            com.siirio.jemserver.smp.events.PartyTeleportFlow.returnNow(player,row);
            SmpData.get(player.server).changed(event);
            boolean remains=new com.siirio.jemserver.smp.events.EncounterContext(event).participantIds().stream()
                    .anyMatch(id->!SmpRecords.members(event).getCompound(id.toString()).getBoolean("eliminated"));
            if(!remains) com.siirio.jemserver.smp.events.EventScheduler.failAttempt(player.server,event);
            return;
        }
        SmpRecords.require(HostedEncounterApi.leave(player),"unavailable");
    }

    public static String startReason(ServerPlayer viewer, CompoundTag row) {
        if (!row.getUUID("owner").equals(viewer.getUUID())) return "host_only";
        if (SmpData.closed(row)) return "closed";
        if (row.getString("state").equals("ACTIVE")) return "combat_locked";
        var host = viewer.server.getPlayerList().getPlayer(row.getUUID("owner"));
        if (host == null) return "host_must_be_present";
        if (row.hasUUID("eventId")) {
            var event = SmpData.get(viewer.server).find("events", row.getUUID("eventId"));
            if (event == null || SmpData.closed(event)) return "unavailable";
            if (!event.getString("state").equals("ACTIVE")) return "event_not_started";
            if (event.getBoolean("combatStarted") || event.hasUUID("bossEntity")) return "combat_locked";
        }
        for (UUID id : SmpRecords.memberIds(row)) {
            if (!Parties.accepted(row, id)) continue;
            var member = viewer.server.getPlayerList().getPlayer(id);
            if (member != null && (!member.isAlive() || member.isSpectator()
                    || !SmpRecords.members(row).getCompound(id.toString()).getBoolean("ready"))) return "party_not_ready";
        }
        if (row.getString("activity").equals("BOSS") && !row.getBoolean("raid") && !row.getBoolean("bloodMoon") && !row.getBoolean("nativePending")) {
            if (row.hasUUID("bossEntity")) {
                if (!(host.serverLevel().getEntity(row.getUUID("bossEntity")) instanceof LivingEntity boss) || !boss.isAlive()) return "unavailable";
            } else {
                var candidates = host.serverLevel().getEntitiesOfClass(LivingEntity.class,
                        new AABB(BlockPos.of(row.getLong("position"))).inflate(SmpConfig.ENCOUNTER_RADIUS.get()),
                        entity -> entity.isAlive() && WorldTierApi.profile(entity).isPresent());
                if (candidates.size() != 1) return "one_awakened_boss_required";
            }
        }
        if (row.getString("activity").equals("BOSS") && !row.getBoolean("raid") && row.getBoolean("nativePending")) {
            String reason=com.siirio.jemserver.smp.events.StructureStaging.nativeBossStartReason(host,row);
            if(!reason.isEmpty()) return reason;
        }
        return "";
    }

    private HostedParties() {}
}
