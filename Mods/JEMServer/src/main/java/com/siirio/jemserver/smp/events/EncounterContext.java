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
import java.util.concurrent.TimeUnit;

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
    private static final long ABSENCE_DEFEAT_MILLIS = TimeUnit.MINUTES.toMillis(5);
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
        var participants = snapshot.accepted();
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
            if (player == null) continue;
            BlockPos preferred = index++ == 0 ? respawn : center.offset(slotOffset(index));
            BlockPos arrival = findSafe(level, preferred, occupied);
            if (arrival == null) {
                if (id.equals(data.encounterHost())) arrival = respawn;
                else continue;
            }
            occupied.add(arrival);
            var member = data.memberOrEmpty(id);
            member.encounterArrival( arrival.asLong());
            member.encounterTeleported( false);
            data.addMember(id, member);
        }
        return data.memberOrEmpty(data.encounterHost()).hasEncounterArrival();
    }

    public void teleportParticipants(MinecraftServer server) {
        ServerLevel level = EventRegions.level(server, data);
        if (level == null) return;
        transition(State.TELEPORTING, level.getGameTime());
        for (UUID id : participantIds()) {
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            var member = data.memberOrEmpty(id);
            if (player == null || member.encounterTeleported() || !member.hasEncounterArrival()) continue;
            BlockPos arrival = BlockPos.of(member.encounterArrival());
            player.teleportTo(level, arrival.getX() + .5, arrival.getY(), arrival.getZ() + .5, player.getYRot(), player.getXRot());
            player.setDeltaMovement(Vec3.ZERO);
            player.fallDistance = 0;
            member.encounterTeleported( true);
        }
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
                    if (player == null) continue;
                    var member = data.memberOrEmpty(id);
                    BlockPos expected = member.hasEncounterArrival() ? BlockPos.of(member.encounterArrival()) : null;
                    if (expected == null || player.serverLevel() != level || !EventRegions.contains(data, level, player.blockPosition())
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
        return isParticipant(playerId) && data.memberOrEmpty(playerId).eventReturnPending();
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
        player.teleportTo(level, member.originX(), member.originY(), member.originZ(), member.originYaw(), member.originPitch());
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0;
        member.clearEventReturnPending();

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
            member.clearEventReturnPending();
            member.clearOriginDimension();
            member.clearOriginX();
            member.clearOriginY();
            member.clearOriginZ();
            member.clearOriginYaw();
            member.clearOriginPitch();
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
        if (!isParticipant(player.getUUID())) return;
        var member = data.memberOrEmpty(player.getUUID());
        if (member.eventAbsenceDefeatPending()) {
            member.clearEventAbsenceDefeatPending();
            notifyAbsenceDefeat(player);
            if (!returnPlayerNow(player)) EventRegions.eject(player, data);

            return;
        }
        if (!member.hasEventDisconnectedAt()) return;
        long disconnectedAt = member.eventDisconnectedAt();
        member.clearEventDisconnectedAt();
        if (System.currentTimeMillis() - disconnectedAt >= ABSENCE_DEFEAT_MILLIS) {
            member.eliminated( true);
            notifyAbsenceDefeat(player);
            if (!returnPlayerNow(player)) EventRegions.eject(player, data);
        } else if (data.solo() && data.state().equals("ACTIVE") && data.combatStarted()) {
            returnToArena(player);
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
        for (UUID id : participantIds()) {
            var member = data.memberOrEmpty(id);
            if (member.eliminated() || !member.hasEventDisconnectedAt()
                    || server.getPlayerList().getPlayer(id) != null
                    || now - member.eventDisconnectedAt() < ABSENCE_DEFEAT_MILLIS) continue;
            member.clearEventDisconnectedAt();
            member.eliminated( true);
            member.eventAbsenceDefeatPending( true);
            changed = true;
        }
        
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
            if (!new EncounterContext(row).isParticipant(player.getUUID())) continue;
            var member = row.memberOrEmpty(player.getUUID());
            if (!member.hasEventDisconnectedAt() && !member.eventAbsenceDefeatPending()) continue;
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
