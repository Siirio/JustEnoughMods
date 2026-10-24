package com.siirio.jemserver.smp.events;

import com.siirio.jemserver.smp.*;

import com.siirio.jemserver.smp.SmpData;
import com.siirio.jemserver.smp.SmpRecords;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class EncounterContext {
    public enum State { PREPARING, TELEPORTING, ARRIVED, INTRO, ACTIVE, COMPLETED, FAILED }

    private static final String STATE = "encounterState";
    private static final String HOST = "encounterHost";
    private static final String PARTICIPANTS = "encounterParticipants";
    private static final String RESPAWN = "arenaRespawnPoint";
    private static final String ARRIVAL = "encounterArrival";
    private static final String TELEPORTED = "encounterTeleported";
    private static final String WALK_IN = "encounterWalkIn";
    private static final String RETURN_PENDING = "eventReturnPending";
    private static final String RETURN_DIMENSION = "originDimension";
    private static final String RETURN_X = "originX";
    private static final String RETURN_Y = "originY";
    private static final String RETURN_Z = "originZ";
    private static final String RETURN_YAW = "originYaw";
    private static final String RETURN_PITCH = "originPitch";
    private static final String DISCONNECTED_AT = "eventDisconnectedAt";
    private static final String ABSENCE_DEFEAT_PENDING = "eventAbsenceDefeatPending";
    private static final int INTRO_TICKS = 40;
    private static final int SEARCH_RADIUS = 24;

    private final EventState data;

    public EncounterContext(EventState data) {
        this.data = data;
    }

    public State state() {
        String value = data.encounterState();
        if (value.isEmpty()) return data.combatStarted() ? State.ACTIVE : State.PREPARING;
        try {
            return State.valueOf(value);
        } catch (IllegalArgumentException ignored) {
            return State.FAILED;
        }
    }

    public boolean isParticipant(UUID playerId) {
        return data.hasEncounterParticipants() ? data.encounterParticipants().contains(playerId) : new EventSession(data).legacyAccepted(playerId);
    }
    public Set<UUID> participantIds() { return Collections.unmodifiableSet(new LinkedHashSet<>(data.encounterParticipants())); }

    public void initialize(ServerPlayer host, EventParticipants snapshot) {
        var serverForMutation = host.server;
        if (serverForMutation == null || serverForMutation.overworld() == null) { initializeInScope(host, snapshot); return; }
        var store = com.siirio.jemserver.smp.SmpData.get(serverForMutation);
        store.edit(() -> initializeInScope(host, snapshot));
    }

    private void initializeInScope(ServerPlayer host, EventParticipants snapshot) {
        SmpRecords.require(!data.hasEncounterParticipants() && !data.combatStarted(), "combat_locked");
        var participants = snapshot.accepted();
        for (UUID id : participants) {
            var player = host.server.getPlayerList().getPlayer(id);
            SmpRecords.require(player != null && player.isAlive() && !player.isSpectator()
                    && !data.memberOrEmpty(id).eliminated(), "party_not_ready");
        }
        var stored = List.copyOf(participants);
        data.encounterHost( host.getUUID());
        data.encounterParticipants(stored);
        for (UUID id : participants) {
            var player = host.server.getPlayerList().getPlayer(id);
            var member = data.memberOrEmpty(id);
            member.accepted( true);
            member.eliminated( false);
            if (player != null) {
                member.name( player.getGameProfile().getName());
            }
            data.addMember(id, member);
        }
        transition(State.PREPARING, host.serverLevel().getGameTime());
    }

    public boolean prepare(MinecraftServer server) {
        ServerLevel level = EventRegions.level(server, data);
        if (level == null) return false;
        for (int chunkX = EventRegions.minX(data) >> 4; chunkX <= EventRegions.maxX(data) >> 4; chunkX++)
            for (int chunkZ = EventRegions.minZ(data) >> 4; chunkZ <= EventRegions.maxZ(data) >> 4; chunkZ++) level.getChunk(chunkX, chunkZ);
        BlockPos center = BlockPos.of(data.position());
        BlockPos respawn = findSafe(level, center, Set.of());
        if (respawn == null) return false;
        data.arenaRespawnPoint( respawn.asLong());
        var occupied = new LinkedHashSet<BlockPos>();
        occupied.add(respawn);
        int index = 0;
        for (UUID id : participantIds()) {
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player == null || !player.isAlive() || player.isSpectator()
                    || data.memberOrEmpty(id).eliminated()) return false;
            BlockPos preferred = index++ == 0 ? respawn : center.offset(slotOffset(index));
            BlockPos arrival = findSafe(level, preferred, occupied);
            if (arrival == null || !PartyTeleportFlow.safeDestination(player, level, Vec3.atBottomCenterOf(arrival))
                    || !EventRegions.contains(data, level, player.getDimensions(player.getPose()).makeBoundingBox(
                    arrival.getX() + .5, arrival.getY(), arrival.getZ() + .5))) return false;
            occupied.add(arrival);
            var member = data.memberOrEmpty(id);
            member.encounterArrival( arrival.asLong());
            member.encounterTeleported( false);
            data.addMember(id, member);
        }
        return data.memberOrEmpty(data.encounterHost()).hasEncounterArrival();
    }

    public void teleportParticipants(MinecraftServer server) {
        var store = SmpData.get(server);
        store.edit(() -> new EncounterContext(store.events().bind(data)).teleportParticipantsInScope(server));
    }

    private void teleportParticipantsInScope(MinecraftServer server) {
        ServerLevel level = EventRegions.level(server, data);
        SmpRecords.require(level != null, "no_safe_arrival");
        var targets = new java.util.LinkedHashMap<ServerPlayer, Vec3>();
        var occupied = new java.util.ArrayList<AABB>();
        for (UUID id : participantIds()) {
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            var member = data.memberOrEmpty(id);
            SmpRecords.require(player != null && !member.eliminated() && member.hasEncounterArrival(), "party_not_ready");
            if (member.encounterTeleported()) continue;
            Vec3 target = Vec3.atBottomCenterOf(BlockPos.of(member.encounterArrival()));
            var box = player.getDimensions(player.getPose()).makeBoundingBox(target.x, target.y, target.z);
            SmpRecords.require(PartyTeleportFlow.safeDestination(player, level, target)
                    && EventRegions.contains(data, level, box) && occupied.stream().noneMatch(box::intersects), "no_safe_arrival");
            occupied.add(box); targets.put(player, target);
        }
        for (var player : targets.keySet()) {
            var member = data.memberOrEmpty(player.getUUID());
            recordReturn(player, member); member.eventReturnPending(true);
        }
        transition(State.TELEPORTING, level.getGameTime());
        SmpData.get(server).flush(server);
        var moves = new java.util.ArrayList<Runnable>();
        var restores = new java.util.ArrayList<Runnable>();
        for (var entry : targets.entrySet()) {
            var player = entry.getKey(); var target = entry.getValue();
            moves.add(() -> {
                BossTeleportSafety.apply(player, level, target, player.getYRot(), player.getXRot());
                SmpRecords.require(player.serverLevel() == level && player.position().distanceToSqr(target) < .01, "transfer_rejected");
                data.memberOrEmpty(player.getUUID()).encounterTeleported(true);
            });
            restores.add(() -> SmpRecords.require(returnPlayerNow(player), "return_pending"));
        }
        PartyTeleportFlow.transferAll(moves, restores);
        SmpData.get(server).flush(server);
    }

    public void awaitEntry(ServerLevel level) {
        data.encounterWalkIn(true);
        transition(State.TELEPORTING,level.getGameTime());
    }

    public boolean tickStartup(MinecraftServer server) {
        ServerLevel level = EventRegions.level(server, data);
        if (level == null) return false;
        long now = level.getGameTime();
        if (state() == State.TELEPORTING) {
            if (data.encounterWalkIn()) {
                if (allParticipantsInside(server)) transition(State.ARRIVED, now);
            } else {
                boolean arrived = true;
                for (UUID id : participantIds()) {
                    ServerPlayer player = server.getPlayerList().getPlayer(id);
                    if (player == null || !player.isAlive() || player.isSpectator()
                            || data.memberOrEmpty(id).eliminated()) { arrived = false; continue; }
                    var member = data.memberOrEmpty(id);
                    BlockPos expected = member.hasEncounterArrival() ? BlockPos.of(member.encounterArrival()) : null;
                    if (expected == null || player.serverLevel() != level || !EventRegions.contains(data, level, player.getBoundingBox())
                            || player.distanceToSqr(Vec3.atBottomCenterOf(expected)) > 16) arrived = false;
                }
                if (arrived) transition(State.ARRIVED, now);
            }
        }
        if (state() == State.ARRIVED) {
            transition(State.INTRO, now);
            data.encounterIntroEnds( now + INTRO_TICKS);
        }
        if (state() == State.INTRO && now >= data.encounterIntroEnds()) transition(State.ACTIVE, now);
        return state() == State.ACTIVE;
    }

    public boolean allParticipantsInside(MinecraftServer server) {
        ServerLevel level = EventRegions.level(server, data);
        Set<UUID> participants = participantIds();
        if (level == null || participants.isEmpty()) return false;
        for (UUID id : participants) {
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player == null || !player.isAlive() || player.isSpectator() || player.serverLevel() != level
                    || !EventRegions.contains(data, level, player.getBoundingBox())) return false;
        }
        return true;
    }

    public boolean arrivalInProgress() {
        return state() == State.PREPARING || state() == State.TELEPORTING || state() == State.ARRIVED;
    }

    public BlockPos respawnPoint(ServerLevel level) {
        BlockPos saved = data.hasArenaRespawnPoint() ? BlockPos.of(data.arenaRespawnPoint()) : null;
        if (saved != null && safe(level, saved, Set.of())) return saved;
        BlockPos resolved = findSafe(level, BlockPos.of(data.position()), Set.of());
        if (resolved != null) data.arenaRespawnPoint( resolved.asLong());
        return resolved;
    }

    public void complete(MinecraftServer server, boolean success) {
        ServerLevel level = EventRegions.level(server, data);
        transition(success ? State.COMPLETED : State.FAILED, level == null ? server.getTickCount() : level.getGameTime());
        clearDisconnectTimers();
        clearStartupData();
    }

    public boolean returnPending(UUID playerId) {
        return data.memberOrEmpty(playerId).eventReturnPending();
    }

    public void stay(MinecraftServer server, UUID playerId) {
        var serverForMutation = server;
        if (serverForMutation == null || serverForMutation.overworld() == null) { stayInScope(server, playerId); return; }
        var store = com.siirio.jemserver.smp.SmpData.get(serverForMutation);
        store.edit(() -> new EncounterContext(store.events().bind(data)).stayInScope(server, playerId));
    }

    private void stayInScope(MinecraftServer server, UUID playerId) {
        data.memberOrEmpty(playerId).clearEventReturnPending();

    }

    public boolean returnPlayer(ServerPlayer player) {
        var member = data.memberOrEmpty(player.getUUID());
        if (!member.eventReturnPending()) return false;
        return returnPlayerNow(player);
    }

    public boolean returnPlayerNow(ServerPlayer player) {
        var serverForMutation = player.server;
        if (serverForMutation == null || serverForMutation.overworld() == null) { return returnPlayerNowInScope(player); }
        var store = com.siirio.jemserver.smp.SmpData.get(serverForMutation);
        return store.edit(() -> new EncounterContext(store.events().bind(data)).returnPlayerNowInScope(player));
    }

    private boolean returnPlayerNowInScope(ServerPlayer player) {
        var member = data.memberOrEmpty(player.getUUID());
        if (!member.hasOriginDimension()) return false;
        ResourceLocation dimension = ResourceLocation.tryParse(member.originDimension());
        ServerLevel level = dimension == null ? null : player.server.getLevel(ResourceKey.create(Registries.DIMENSION, dimension));
        if (level == null) return false;
        var target = new Vec3(member.originX(), member.originY(), member.originZ());
        if (!PartyTeleportFlow.safeDestination(player, level, target)) return false;
        member.eventReturnPending(true);
        SmpData.get(player.server).flush(player.server);
        BossTeleportSafety.apply(player, level, target, member.originYaw(), member.originPitch());
        if (player.serverLevel() != level || player.position().distanceToSqr(target) > .01) return false;
        SmpData.flushPlayerPosition(player);
        member.clearEventReturnPending();
        try { SmpData.get(player.server).flush(player.server); }
        catch (RuntimeException failure) { member.eventReturnPending(true); throw failure; }
        return true;
    }

    public void returnParticipants(MinecraftServer server) {
        for (UUID id : participantIds()) {
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player != null) returnPlayerNow(player);
        }
    }

    public void resetAttempt() {
        data.clearEncounterState();
        data.clearEncounterHost();
        data.clearEncounterParticipants();
        data.clearArenaRespawnPoint();
        data.clearEncounterStateSince();
        data.clearEncounterIntroEnds();
        data.clearEncounterWalkIn();
        for (UUID id : data.members().keySet()) {
            EventMember member = data.memberOrEmpty(id);
            member.clearEncounterArrival();
            member.clearEncounterTeleported();
            if (!member.eventReturnPending()) {
                member.clearOriginDimension(); member.clearOriginX(); member.clearOriginY(); member.clearOriginZ();
                member.clearOriginYaw(); member.clearOriginPitch();
            }
            member.clearEventDisconnectedAt();
            member.clearEventAbsenceDefeatPending();
        }
    }

    public void clearStartupData() {
        data.clearArenaRespawnPoint();
        data.clearEncounterWalkIn();
        data.clearEncounterIntroEnds();
        for (UUID id : data.members().keySet()) {
            EventMember member = data.memberOrEmpty(id);
            member.clearEncounterArrival();
            member.clearEncounterTeleported();
            member.clearEventDisconnectedAt();
            member.clearEventAbsenceDefeatPending();
        }
    }

    public void disconnected(ServerPlayer player) {
        var serverForMutation = player.server;
        if (serverForMutation == null || serverForMutation.overworld() == null) { disconnectedInScope(player); return; }
        var store = com.siirio.jemserver.smp.SmpData.get(serverForMutation);
        store.edit(() -> new EncounterContext(store.events().bind(data)).disconnectedInScope(player));
    }

    private void disconnectedInScope(ServerPlayer player) {
        if (!data.state().equals("ACTIVE") || !data.combatStarted()
                || !isParticipant(player.getUUID()) || !new EventSession(data).accepted(player.getUUID())) return;
        data.memberOrEmpty(player.getUUID()).eventDisconnectedAt(System.currentTimeMillis());

    }

    public void reconnected(ServerPlayer player) {
        var serverForMutation = player.server;
        if (serverForMutation == null || serverForMutation.overworld() == null) { reconnectedInScope(player); return; }
        var store = com.siirio.jemserver.smp.SmpData.get(serverForMutation);
        store.edit(() -> new EncounterContext(store.events().bind(data)).reconnectedInScope(player));
    }

    private void reconnectedInScope(ServerPlayer player) {
        var member = data.memberOrEmpty(player.getUUID());
        if (!isParticipant(player.getUUID()) && !member.eventReturnPending()) return;
        if (member.eliminated()) {
            if (data.activity().equals("BOSS_RAID") && !member.nativeDeath() && member.eventAbsenceDefeatPending()
                    && host(player.server, player.getUUID())) {
                SmpData.get(player.server).flush(player.server);
                EventScheduler.failAttempt(player.server, data);
                return;
            }
            member.clearEventDisconnectedAt();
            member.clearEventAbsenceDefeatPending();
            if (member.nativeDeath()) member.clearEventReturnPending();
            else if (member.eventReturnPending()) returnPlayerNow(player);
            SmpData.get(player.server).flush(player.server);
            return;
        }
        if (member.eventReturnPending() && (!data.state().equals("ACTIVE") || !data.hasEncounterParticipants())) {
            returnPlayerNow(player);
            return;
        }
        if (member.eventAbsenceDefeatPending()) {
            member.clearEventAbsenceDefeatPending();
            notifyAbsenceDefeat(player);
            if (!returnPlayerNow(player)) EventRegions.eject(player, data);

            return;
        }
        if (!member.hasEventDisconnectedAt()) return;
        long disconnectedAt = member.eventDisconnectedAt();
        member.clearEventDisconnectedAt();
        if (System.currentTimeMillis() - disconnectedAt >= com.siirio.jemworldbosstiers.api.HostedEncounterApi.disconnectGraceMillis()) {
            if (canonicalNativeDeath(player.server, player.getUUID())) { member.nativeDeath(true); member.eliminated(true); SmpData.get(player.server).flush(player.server); return; }
            member.eliminated(true); member.eventAbsenceDefeatPending(true);
            SmpData.get(player.server).flush(player.server);
            if (data.activity().equals("BOSS_RAID") && host(player.server, player.getUUID())) {
                EventScheduler.failAttempt(player.server, data);
                return;
            }
            notifyAbsenceDefeat(player);
            if (!returnPlayerNow(player)) EventRegions.eject(player, data);
        }

    }

    public void expireDisconnected(MinecraftServer server, long now) {
        var serverForMutation = server;
        if (serverForMutation == null || serverForMutation.overworld() == null) { expireDisconnectedInScope(server, now); return; }
        var store = com.siirio.jemserver.smp.SmpData.get(serverForMutation);
        store.edit(() -> new EncounterContext(store.events().bind(data)).expireDisconnectedInScope(server, now));
    }

    private void expireDisconnectedInScope(MinecraftServer server, long now) {
        if (!data.state().equals("ACTIVE") || !data.combatStarted()) return;
        boolean changed = false;
        boolean absentHost = false;
        for (UUID id : participantIds()) {
            var member = data.memberOrEmpty(id);
            if (member.eventAbsenceDefeatPending() && !member.nativeDeath() && host(server, id)) absentHost = true;
            if (member.eliminated() || !member.hasEventDisconnectedAt()
                    || server.getPlayerList().getPlayer(id) != null
                    || now - member.eventDisconnectedAt() < com.siirio.jemworldbosstiers.api.HostedEncounterApi.disconnectGraceMillis()) continue;
            member.clearEventDisconnectedAt();
            member.eliminated(true);
            if (canonicalNativeDeath(server, id)) member.nativeDeath(true);
            else { member.eventAbsenceDefeatPending(true); absentHost |= host(server, id); }
            changed = true;
        }
        if (changed) SmpData.get(server).flush(server);
        if (data.activity().equals("BOSS_RAID") && (absentHost || !participantIds().isEmpty()
                && participantIds().stream().allMatch(id -> data.memberOrEmpty(id).eliminated())))
            EventScheduler.failAttempt(server, data);
    }

    private boolean host(MinecraftServer server, UUID playerId) {
        var party = data.hasParty() ? SmpData.get(server).parties().current(data.party()) : null;
        return party != null ? playerId.equals(party.owner()) : playerId.equals(data.encounterHost());
    }

    private boolean canonicalNativeDeath(MinecraftServer server, UUID playerId) {
        var level = EventRegions.level(server, data);
        if (!data.hasBossEntity() || level == null || !(level.getEntity(data.bossEntity()) instanceof net.minecraft.world.entity.LivingEntity boss)) {
            var snapshot = data.hasAttemptId() ? com.siirio.jemworldbosstiers.progression.WorldTierData.get(server)
                    .hostedAttempts().get(data.attemptId()) : null;
            if (snapshot != null && data.hasBossEntity() && snapshot.getUUID("Boss").equals(data.bossEntity()))
                return snapshot.getCompound("State").getCompound("Players").getCompound(playerId.toString()).getBoolean("NativeDeath");
            return data.memberOrEmpty(playerId).nativeDeath();
        }
        if (data.hasAttemptId() && !data.attemptId().equals(com.siirio.jemworldbosstiers.api.HostedEncounterApi.attemptId(boss))) return false;
        return boss.getPersistentData().getCompound("JEMHostedEncounter").getCompound("Players")
                .getCompound(playerId.toString()).getBoolean("NativeDeath");
    }

    public static EventState activeFor(ServerPlayer player) {
        var serverForMutation = player.server;
        if (serverForMutation == null || serverForMutation.overworld() == null) { return activeForInScope(player); }
        var store = com.siirio.jemserver.smp.SmpData.get(serverForMutation);
        if (store.inCommand()) return activeForInScope(player);
        return store.edit(() -> { var result = activeForInScope(player); return result == null ? null : result.snapshot(); });
    }

    private static EventState activeForInScope(ServerPlayer player) {
        for (var row : SmpData.get(player.server).events().currentRows())
            if (EventRuntime.available(player.server, row) && row.state().equals("ACTIVE") && row.combatStarted()
                    && new EncounterContext(row).isParticipant(player.getUUID())) return row;
        return null;
    }

    public static EventState reconnectFor(ServerPlayer player) {
        var serverForMutation = player.server;
        if (serverForMutation == null || serverForMutation.overworld() == null) { return reconnectForInScope(player); }
        var store = com.siirio.jemserver.smp.SmpData.get(serverForMutation);
        if (store.inCommand()) return reconnectForInScope(player);
        return store.edit(() -> { var result = reconnectForInScope(player); return result == null ? null : result.snapshot(); });
    }

    private static EventState reconnectForInScope(ServerPlayer player) {
        EventState latest = null;
        for (var row : SmpData.get(player.server).events().currentRows()) {
            if (!EventRuntime.available(player.server, row)) continue;
            var member = row.memberOrEmpty(player.getUUID());
            if (!new EncounterContext(row).isParticipant(player.getUUID()) && !member.eventReturnPending()) continue;
            if (!member.hasEventDisconnectedAt() && !member.eventAbsenceDefeatPending() && !member.eventReturnPending()) continue;
            if (latest == null || row.created() > latest.created()) latest = row;
        }
        return latest;
    }

    private void transition(State state, long tick) {
        data.encounterState( state.name());
        data.encounterStateSince( tick);
    }

    private void returnToArena(ServerPlayer player) {
        ServerLevel level = EventRegions.level(player.server, data);
        BlockPos position = level == null ? null : respawnPoint(level);
        if (level == null || position == null) return;
        player.teleportTo(level, position.getX() + .5, position.getY(), position.getZ() + .5, player.getYRot(), player.getXRot());
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0;
    }

    private void clearDisconnectTimers() {
        for (UUID id : participantIds()) {
            var member = data.memberOrEmpty(id);
            member.clearEventDisconnectedAt();
        }
    }

    private static void notifyAbsenceDefeat(ServerPlayer player) {
        player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("jem.event.defeat_title")
                .withStyle(net.minecraft.ChatFormatting.RED, net.minecraft.ChatFormatting.BOLD));
        player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("jem.event.disconnect_defeat")
                .withStyle(net.minecraft.ChatFormatting.GOLD));
    }

    private static void recordReturn(ServerPlayer player, EventMember member) {
        if (member.hasOriginDimension()) return;
        member.originDimension( player.serverLevel().dimension().location().toString());
        member.originX( player.getX());
        member.originY( player.getY());
        member.originZ( player.getZ());
        member.originYaw( player.getYRot());
        member.originPitch( player.getXRot());
    }

    private BlockPos findSafe(ServerLevel level, BlockPos anchor, Set<BlockPos> occupied) {
        List<BlockPos> candidates = new ArrayList<>();
        for (int radius = 0; radius <= SEARCH_RADIUS; radius++)
            for (int x = -radius; x <= radius; x++)
                for (int z = -radius; z <= radius; z++)
                    if (radius == 0 || Math.abs(x) == radius || Math.abs(z) == radius) candidates.add(anchor.offset(x, 0, z));
        int minY=data.hasMinY()?Math.max(level.getMinBuildHeight()+1,data.minY()):level.getMinBuildHeight()+1;
        int maxY=data.hasMaxY()?Math.min(level.getMaxBuildHeight()-2,data.maxY()):level.getMaxBuildHeight()-2;
        int anchorY=Math.max(minY,Math.min(maxY,anchor.getY()));
        if(data.activity().equals("BLOOD_MOON")) {
            for(BlockPos column:candidates) {
                int y=Math.max(minY,Math.min(maxY,level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,column.getX(),column.getZ())));
                BlockPos position=new BlockPos(column.getX(),y,column.getZ());
                if(safe(level,position,occupied)) return position;
            }
            return null;
        }
        for (BlockPos column : candidates) {
            for(int delta=0;delta<=maxY-minY;delta++) for(int direction:delta==0?new int[]{0}:new int[]{delta,-delta}) {
                int y=anchorY+direction;
                if(y<minY||y>maxY) continue;
                BlockPos position = new BlockPos(column.getX(), y, column.getZ());
                if (safe(level, position, occupied)) return position;
            }
        }
        return null;
    }

    private boolean safe(ServerLevel level, BlockPos position, Set<BlockPos> occupied) {
        if (!EventRegions.contains(data, level, position) || occupied.stream().anyMatch(value -> value.distSqr(position) < 4)) return false;
        var floor = level.getBlockState(position.below());
        if (!floor.isFaceSturdy(level, position.below(), Direction.UP) || floor.is(Blocks.MAGMA_BLOCK) || floor.is(Blocks.CACTUS)
                || floor.is(Blocks.FIRE) || floor.is(Blocks.SOUL_FIRE) || !level.getFluidState(position).isEmpty()
                || !level.getFluidState(position.above()).isEmpty()) return false;
        if (!level.getBlockState(position).getCollisionShape(level, position).isEmpty()
                || !level.getBlockState(position.above()).getCollisionShape(level, position.above()).isEmpty()) return false;
        AABB box = new AABB(position.getX() + .2, position.getY(), position.getZ() + .2, position.getX() + .8, position.getY() + 1.8, position.getZ() + .8);
        return level.getEntities((net.minecraft.world.entity.Entity)null, box, entity -> entity.isAlive() && !(entity instanceof ServerPlayer)).isEmpty();
    }

    private static BlockPos slotOffset(int index) {
        int ring = (index + 7) / 8;
        double angle = Math.PI * 2 * Math.floorMod(index - 1, 8) / 8;
        return new BlockPos((int) Math.round(Math.cos(angle) * ring * 3), 0, (int) Math.round(Math.sin(angle) * ring * 3));
    }
}
