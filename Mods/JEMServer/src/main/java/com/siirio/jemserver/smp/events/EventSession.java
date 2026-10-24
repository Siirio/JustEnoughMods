package com.siirio.jemserver.smp.events;

import com.siirio.jemserver.smp.*;

import com.siirio.jemserver.smp.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import java.util.*;

public record EventSession(EventState data) {
    public static final String SESSION = "jem:event_session_id";
    public static final String SPAWNED = "jem:event_spawned";
    public static final String TYPE = "jem:event_type";
    public UUID id() { return data.id(); }
    public boolean accepted(UUID id) {
        if (data.hasEncounterParticipants())
            return new EncounterContext(data).isParticipant(id) && !data.memberOrEmpty(id).eliminated();
        return legacyAccepted(id);
    }
    boolean legacyAccepted(UUID id) {
        var member=data.memberOrEmpty(id);
        return member.accepted()&&!member.eliminated();
    }
    public List<ServerPlayer> active(MinecraftServer server) {
        if (!EventRuntime.available(server, data)) return List.of();
        return server.getPlayerList().getPlayers().stream().filter(p -> accepted(p.getUUID()) && p.isAlive() && !p.isSpectator() && EventRegions.contains(data,p.serverLevel(),p.getBoundingBox())).toList();
    }
    public void accept(ServerPlayer player) {
        var serverForMutation = player.server;
        if (serverForMutation == null || serverForMutation.overworld() == null) { acceptInScope(player); return; }
        var store = com.siirio.jemserver.smp.SmpData.get(serverForMutation);
        store.edit(() -> new EventSession(store.events().bind(data)).acceptInScope(player));
    }

    private void acceptInScope(ServerPlayer player) {
        var member=data.memberOrEmpty(player.getUUID());
        SmpRecords.require(canAccept(data, player.getUUID()), "combat_locked");
        member.accepted(true);
        member.name(player.getGameProfile().getName());
        data.addMember(player.getUUID(),member);

    }
    static boolean canAccept(EventState event, UUID playerId) {
        return event.pendingOutcome().isEmpty() && !event.hasEncounterParticipants() && !event.combatStarted()
                && !event.memberOrEmpty(playerId).eliminated();
    }

    public void mark(Entity entity) {
        var tag=entity.getPersistentData();
        tag.putUUID(SESSION,id()); tag.putBoolean(SPAWNED,true);
        tag.putString(TYPE,data.activity().toLowerCase(Locale.ROOT));
    }
    public static Entity owner(Entity entity) {
        return com.siirio.jemworldbosstiers.api.HostedEncounterApi.damageOwner(entity);
    }

    /** One event admission rule for support, control, attackers and recipients. */
    public static boolean canInteract(LivingEntity actor, LivingEntity target) {
        if (actor.level().isClientSide || actor.level() != target.level()
                || !actor.isAlive() || !target.isAlive() || actor.isSpectator() || target.isSpectator()) return false;
        EventState source = combatForEntity(actor), destination = combatForEntity(target);
        if (source == null && destination == null) return true;
        return source != null && destination != null && source.id().equals(destination.id())
                && admitted(source, actor) && admitted(source, target);
    }

    static EventState combatForEntity(LivingEntity entity) {
        Entity root = owner(entity);
        EventState row;
        if (root instanceof ServerPlayer player) {
            row = combatForPlayer(player);
        } else {
            UUID id = eventId(root);
            row = id == null || entity.getServer() == null ? null : SmpData.get(entity.getServer()).events().snapshot(id);
        }
        return row != null && row.state().equals("ACTIVE") && row.combatStarted()
                && EventRuntime.available(entity.getServer(), row) ? row : null;
    }

    static boolean admitted(EventState row, LivingEntity entity) {
        if (!(entity.level() instanceof ServerLevel level) || !entity.isAlive() || entity.isSpectator()
                || !EventRegions.contains(row, level, entity.getBoundingBox())) return false;
        Entity root = owner(entity);
        if (root instanceof ServerPlayer player) {
            EventState active = combatForPlayer(player);
            return player.isAlive() && !player.isSpectator() && player.level() == entity.level()
                    && EventRegions.contains(row, player.serverLevel(), player.getBoundingBox())
                    && new EventSession(row).accepted(player.getUUID())
                    && active != null && active.id().equals(row.id());
        }
        return root instanceof LivingEntity living && living.isAlive() && !living.isSpectator()
                && living.level() == entity.level() && EventRegions.contains(row, level, living.getBoundingBox())
                && row.id().equals(eventId(living));
    }

    private static UUID eventId(Entity entity) {
        if (entity == null) return null;
        var tag = entity.getPersistentData();
        if (tag.hasUUID(SESSION)) return tag.getUUID(SESSION);
        if (tag.hasUUID(RaidEvent.EVENT_ID)) return tag.getUUID(RaidEvent.EVENT_ID);
        return tag.hasUUID(BloodMoon.EVENT_ID) ? tag.getUUID(BloodMoon.EVENT_ID) : null;
    }

    public static EventState forPlayer(ServerPlayer player) {
        var serverForMutation = player.server;
        if (serverForMutation == null || serverForMutation.overworld() == null) { return forPlayerInScope(player); }
        var store = com.siirio.jemserver.smp.SmpData.get(serverForMutation);
        if (store.inCommand()) return forPlayerInScope(player);
        return store.edit(() -> { var result = forPlayerInScope(player); return result == null ? null : result.snapshot(); });
    }

    private static EventState forPlayerInScope(ServerPlayer player) {
        if (!SmpEnvironment.active(player) || !EventRuntime.enabled(player.server)) return null;
        for(var row:SmpData.get(player.server).events().currentRows())
            if(row.activity().equals("BLOOD_MOON") && row.state().equals("ACTIVE") && new EventSession(row).accepted(player.getUUID())) return row;
        return null;
    }

    public static EventState combatForPlayer(ServerPlayer player) {
        EventState bloodMoon = forPlayer(player);
        if (bloodMoon != null) return bloodMoon;
        if (!SmpEnvironment.active(player)) return null;
        var raid = EventScheduler.active(player.server, "BOSS_RAID");
        if (raid == null || !raid.combatStarted() || !new EventSession(raid).accepted(player.getUUID())) return null;
        var level = EventRegions.level(player.server, raid);
        if (level == null || !raid.hasBossEntity()
                || !(level.getEntity(raid.bossEntity()) instanceof net.minecraft.world.entity.LivingEntity boss)) return null;
        return com.siirio.jemworldbosstiers.api.HostedEncounterApi.status(boss).stream()
                .anyMatch(member -> member.playerId().equals(player.getUUID()) && member.state().equals("ALIVE")) ? raid : null;
    }
}
