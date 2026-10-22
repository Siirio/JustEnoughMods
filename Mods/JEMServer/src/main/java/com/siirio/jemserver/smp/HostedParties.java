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
    public static void start(ServerPlayer host, PartyState row) {
        var data = SmpData.get(host.server);
        data.edit(() -> startOwned(host, data.parties().bind(row)));
    }

    private static void startOwned(ServerPlayer host, PartyState row) {
        SmpRecords.require(SmpEnvironment.active(host), "unavailable");
        SmpRecords.owner(host, row);
        if (row.state().equals("ACTIVE")) return;
        String reason = startReason(host, row);
        SmpRecords.require(reason.isEmpty(), reason);
        if (row.hasEventId()) {
            var event = SmpData.get(host.server).events().current(row.eventId());
            row.dimension(event.dimension());
            row.position(event.position());
        }
        SmpRecords.require(row.activity().equals("BOSS"),"unavailable");
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
        if(row.bloodMoon()) {
            EventParties.start(host,row,ids);
            return;
        }
        if (row.raid()) {
            com.siirio.jemserver.smp.events.RaidEvent.start(host, row, ids);
            row.state("ACTIVE");
            Profiles.count(host.server, host.getUUID(), com.siirio.jemserver.smp.ProfileCounter.BOSSES_HOSTED);
            
            return;
        }
        if(com.siirio.jemserver.smp.events.StructureStaging.start(host,row,ids)) return;
        var level = host.serverLevel();
        LivingEntity boss = null;
        if (row.hasBossEntity()
                && level.getEntity(row.bossEntity()) instanceof LivingEntity found)
            boss = found;
        if (boss == null) {
            var center = BlockPos.of(row.position());
            var candidates =
                    level.getEntitiesOfClass(
                            LivingEntity.class,
                            new AABB(center).inflate(SmpConfig.ENCOUNTER_RADIUS.get()),
                            entity -> entity.isAlive() && HostedEncounterApi.canHost(entity));
            SmpRecords.require(candidates.size() == 1, "one_awakened_boss_required");
            boss = candidates.get(0);
        }
        if (net.minecraftforge.fml.ModList.get().isLoaded("jem_twelve_eyes"))
            SmpRecords.require(com.siirio.jemtwelveeyes.api.CampaignApi.unlocked(host,
                    net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(boss.getType())), "unavailable");
        boss.getPersistentData().putBoolean("jem_solo", row.solo());
        var bounds=com.siirio.jemserver.smp.events.StructureStaging.prepareCombat(level,row,boss);
        SmpRecords.require(HostedEncounterApi.start(boss, ids, false, bounds), "unavailable");
        row.bossEntity(boss.getUUID());
        row.state("ACTIVE");
        Profiles.count(host.server, host.getUUID(), com.siirio.jemserver.smp.ProfileCounter.BOSSES_HOSTED);
        
    }

    public static void closed(HostedEncounterClosedEvent event) {
        var data = SmpData.get(event.boss().getServer());
        data.edit(() -> closedOwned(event));
    }

    private static void closedOwned(HostedEncounterClosedEvent event) {
        var server = event.boss().getServer();
        if (server == null) return;
        var data = SmpData.get(server);
        for (var row : data.parties().currentRows())
            if (row.hasBossEntity()
                    && row.bossEntity().equals(event.boss().getUUID())
                    && !SmpData.closed(row)) {
                if(!event.success()) notifyDefeat(server,row,event.raid());
                row.state(event.success() ? "COMPLETED" : "FAILED");
                
                com.siirio.jemserver.smp.events.StructureStaging.finish(server, row);
                if(!event.success()&&!event.raid()&&row.hasStructureId()) server.execute(()->{
                    if(event.boss().level() instanceof net.minecraft.server.level.ServerLevel level)
                        com.siirio.jemserver.smp.events.StructureStaging.stageBoss(level,row,event.boss());
                });
            }
        if (event.success())
            for (UUID id : event.eligibleParticipants()) {
                Profiles.count(server, id, event.raid() ? com.siirio.jemserver.smp.ProfileCounter.RAIDS_COMPLETED : com.siirio.jemserver.smp.ProfileCounter.BOSS_ASSISTS);
                Profiles.countEvent(server, id, event.raid() ? "BOSS_RAID" : "BOSS");
            }
        for (var row : data.events().currentRows())
            if (row.hasBossEntity()
                    && row.bossEntity().equals(event.boss().getUUID())
                    && row.state().equals("ACTIVE"))
                if(event.success()) com.siirio.jemserver.smp.events.EventScheduler.finish(server,row,"COMPLETED");
                else com.siirio.jemserver.smp.events.EventScheduler.failAttempt(server,row);
    }

    private static void notifyDefeat(net.minecraft.server.MinecraftServer server,PartyState party,boolean raid) {
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
        var data = SmpData.get(server);
        data.edit(() -> tickOwned(server));
    }

    private static void tickOwned(net.minecraft.server.MinecraftServer server) {
        if (!SmpEnvironment.active(server)) return;
        if (server.getTickCount() % 20 != 0) return;
        var data = SmpData.get(server);
        long now = System.currentTimeMillis();
        for (var row : data.parties().currentRows()) {
            if (SmpData.closed(row) || row.state().equals("ACTIVE") || !row.hasBossEntity()) continue;
            var level = server.getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, new net.minecraft.resources.ResourceLocation(row.dimension())));
            if (level != null && level.getEntity(row.bossEntity()) instanceof LivingEntity boss && boss.isAlive()) HostedEncounterApi.hold(boss);
        }

        for (var row : data.parties().currentRows())
            if (!SmpData.closed(row)
                    && !row.state().equals("ACTIVE")
                    && now - row.created() > SmpConfig.PARTY_WAIT_MINUTES.get() * 60000L) {
                release(server, row);
                row.state("CLOSED");
                
            }
    }

    public static void arrive(ServerPlayer player, PartyState row) {
        var data = SmpData.get(player.server);
        data.edit(() -> arriveOwned(player, data.parties().bind(row)));
    }

    private static void arriveOwned(ServerPlayer player, PartyState row) {
        SmpRecords.require(SmpEnvironment.active(player), "unavailable");
        SmpRecords.require(!row.state().equals("ACTIVE") && !SmpData.closed(row), "combat_locked");
        SmpRecords.require(Parties.accepted(row,player.getUUID()),"not_member");
        if (row.activity().equals("BOSS")&&com.siirio.jemserver.smp.events.StructureStaging.arrive(player, row)) return;
        if(row.bloodMoon()&&row.hasEventId()) {
            var event=SmpData.get(player.server).events().current(row.eventId());
            SmpRecords.require(event!=null,"unavailable");
            var targetDimension=net.minecraft.resources.ResourceLocation.tryParse(event.dimension());
            var targetLevel=targetDimension==null?null:player.server.getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,targetDimension));
            SmpRecords.require(targetLevel!=null,"unavailable");
            var outside=com.siirio.jemserver.smp.events.EventRegions.safeOutside(targetLevel,event,BlockPos.of(event.position())).orElseThrow(()->new IllegalArgumentException("no_safe_arrival"));
            player.teleportTo(targetLevel,outside.getX()+.5,outside.getY(),outside.getZ()+.5,player.getYRot(),player.getXRot());
            player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
            player.fallDistance=0;
            return;
        }
        var host=player.server.getPlayerList().getPlayer(row.owner());
        var dimension=net.minecraft.resources.ResourceLocation.tryParse(row.activity().equals("BOSS")?row.dimension()
                :host==null?"":host.serverLevel().dimension().location().toString());
        var level=dimension==null?null:player.server.getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,dimension));
        SmpRecords.require(level!=null,"unavailable");
        var origin = row.activity().equals("BOSS")?BlockPos.of(row.position()):host.blockPosition();
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

    public static void release(net.minecraft.server.MinecraftServer server, PartyState row) {
        var data = SmpData.get(server);
        data.edit(() -> releaseOwned(server, data.parties().bind(row)));
    }

    private static void releaseOwned(net.minecraft.server.MinecraftServer server, PartyState row) {
        if (!row.hasBossEntity()) return;
        var level = server.getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, new net.minecraft.resources.ResourceLocation(row.dimension())));
        if (level != null && level.getEntity(row.bossEntity()) instanceof LivingEntity boss) HostedEncounterApi.release(boss);
    }

    public static void forfeit(ServerPlayer player,UUID partyId,UUID actionToken) {
        var data = SmpData.get(player.server);
        data.edit(() -> forfeitOwned(player, partyId, actionToken));
    }

    private static void forfeitOwned(ServerPlayer player,UUID partyId,UUID actionToken) {
        SmpRecords.require(SmpEnvironment.active(player), "unavailable");
        PartyState row=SmpData.get(player.server).parties().current(partyId);
        SmpRecords.require(row!=null&&!SmpData.closed(row)&&row.state().equals("ACTIVE")
                &&row.activity().equals("BOSS"),"unavailable");
        SmpRecords.owner(player,row);
        SmpRecords.require(com.siirio.jemserver.smp.events.HostedBoundary.matchesAction(player.server,row,actionToken),"unavailable");
        if(row.hasEventId()) {
            var event=SmpData.get(player.server).events().current(row.eventId());
            SmpRecords.require(event!=null&&com.siirio.jemserver.smp.events.EventScheduler.cancelAttempt(player.server,event,actionToken),"unavailable");
            return;
        }
        var dimension=net.minecraft.resources.ResourceLocation.tryParse(row.dimension());
        var level=dimension==null?null:player.server.getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,dimension));
        LivingEntity boss=level!=null&&row.hasBossEntity()&&level.getEntity(row.bossEntity()) instanceof LivingEntity living?living:null;
        if(boss!=null&&HostedEncounterApi.cancel(boss)) return;
        if(boss!=null) HostedEncounterApi.release(boss);
        notifyDefeat(player.server,row,row.raid());
        row.state("FAILED");
        
        com.siirio.jemserver.smp.events.HostedBoundary.clearParty(row);
        com.siirio.jemserver.smp.events.StructureStaging.finish(player.server,row);
    }

    public static void leaveEncounter(ServerPlayer player,UUID partyId) {
        var data = SmpData.get(player.server);
        data.edit(() -> leaveEncounterOwned(player, partyId));
    }

    private static void leaveEncounterOwned(ServerPlayer player,UUID partyId) {
        SmpRecords.require(SmpEnvironment.active(player), "unavailable");
        PartyState row=SmpData.get(player.server).parties().current(partyId);
        SmpRecords.require(row!=null&&!SmpData.closed(row)&&row.state().equals("ACTIVE")
                &&row.activity().equals("BOSS"),"unavailable");
        SmpRecords.require(Parties.accepted(row,player.getUUID()),"not_member");
        SmpRecords.require(!row.owner().equals(player.getUUID()),"host_only");
        SmpRecords.require(com.siirio.jemserver.smp.events.HostedBoundary.canForfeit(player,row),"unavailable");
        if(row.hasEventId()) {
            var event=SmpData.get(player.server).events().current(row.eventId());
            SmpRecords.require(event!=null&&event.state().equals("ACTIVE"),"unavailable");
            var member=event.member(player.getUUID());
            if (member != null) member.eliminated(true);
            com.siirio.jemserver.smp.events.PartyTeleportFlow.returnNow(player,row);
            
            boolean remains=new com.siirio.jemserver.smp.events.EncounterContext(event).participantIds().stream()
                    .anyMatch(id->event.member(id) == null || !event.member(id).eliminated());
            if(!remains) com.siirio.jemserver.smp.events.EventScheduler.failAttempt(player.server,event);
            return;
        }
        SmpRecords.require(HostedEncounterApi.leave(player),"unavailable");
    }

    public static String startReason(ServerPlayer viewer, PartyState row) {
        if (!SmpEnvironment.active(viewer)) return "unavailable";
        if (!row.owner().equals(viewer.getUUID())) return "host_only";
        if (SmpData.closed(row)) return "closed";
        if (row.state().equals("ACTIVE")) return "combat_locked";
        var host = viewer.server.getPlayerList().getPlayer(row.owner());
        if (host == null) return "host_must_be_present";
        if (row.hasEventId()) {
            var event = SmpData.get(viewer.server).events().snapshot(row.eventId());
            if (event == null || SmpData.closed(event)) return "unavailable";
            if (!event.state().equals("ACTIVE")) return "event_not_started";
            if (event.combatStarted() || event.hasBossEntity()) return "combat_locked";
        }
        for (UUID id : SmpRecords.memberIds(row)) {
            if (!Parties.accepted(row, id)) continue;
            var member = viewer.server.getPlayerList().getPlayer(id);
            if (member != null && (!member.isAlive() || member.isSpectator()
                    || !row.member(id).ready())) return "party_not_ready";
        }
        if (row.activity().equals("BOSS") && !row.raid() && !row.bloodMoon() && !row.nativePending()) {
            if (row.hasBossEntity()) {
                if (!(host.serverLevel().getEntity(row.bossEntity()) instanceof LivingEntity boss) || !boss.isAlive()) return "unavailable";
            } else {
                var candidates = host.serverLevel().getEntitiesOfClass(LivingEntity.class,
                        new AABB(BlockPos.of(row.position())).inflate(SmpConfig.ENCOUNTER_RADIUS.get()),
                        entity -> entity.isAlive() && WorldTierApi.profile(entity).isPresent());
                if (candidates.size() != 1) return "one_awakened_boss_required";
            }
        }
        if (row.activity().equals("BOSS") && !row.raid() && row.nativePending()) {
            String reason=com.siirio.jemserver.smp.events.StructureStaging.nativeBossStartReason(host,row);
            if(!reason.isEmpty()) return reason;
        }
        return "";
    }

    private HostedParties() {}
}
