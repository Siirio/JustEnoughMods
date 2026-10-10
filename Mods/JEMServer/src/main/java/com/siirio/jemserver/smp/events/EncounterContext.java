package com.siirio.jemserver.smp.events;

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

    private final CompoundTag data;

    public EncounterContext(CompoundTag data) {
        this.data = data;
    }

    public State state() {
        String value = data.getString(STATE);
        if (value.isEmpty()) return data.getBoolean("combatStarted") ? State.ACTIVE : State.PREPARING;
        try {
            return State.valueOf(value);
        } catch (IllegalArgumentException ignored) {
            return State.FAILED;
        }
    }

    public boolean isParticipant(UUID playerId) {
        if (!data.contains(PARTICIPANTS, Tag.TAG_LIST)) return new EventSession(data).legacyAccepted(playerId);
        for (Tag value : data.getList(PARTICIPANTS, Tag.TAG_STRING))
            if (value.getAsString().equals(playerId.toString())) return true;
        return false;
    }

    public Set<UUID> participantIds() {
        var result = new LinkedHashSet<UUID>();
        for (Tag value : data.getList(PARTICIPANTS, Tag.TAG_STRING)) {
            try {
                result.add(UUID.fromString(value.getAsString()));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return Collections.unmodifiableSet(result);
    }

    public void initialize(ServerPlayer host, EventParticipants snapshot) {
        var participants = snapshot.accepted();
        var stored = new ListTag();
        participants.forEach(id -> stored.add(StringTag.valueOf(id.toString())));
        data.putUUID(HOST, host.getUUID());
        data.put(PARTICIPANTS, stored);
        for (UUID id : participants) {
            var player = host.server.getPlayerList().getPlayer(id);
            var member = SmpRecords.members(data).getCompound(id.toString());
            member.putBoolean("accepted", true);
            member.putBoolean("eliminated", false);
            if (player != null) {
                member.putString("name", player.getGameProfile().getName());
            }
            SmpRecords.members(data).put(id.toString(), member);
        }
        transition(State.PREPARING, host.serverLevel().getGameTime());
    }

    public boolean prepare(MinecraftServer server) {
        ServerLevel level = EventRegions.level(server, data);
        if (level == null) return false;
        for (int chunkX = EventRegions.minX(data) >> 4; chunkX <= EventRegions.maxX(data) >> 4; chunkX++)
            for (int chunkZ = EventRegions.minZ(data) >> 4; chunkZ <= EventRegions.maxZ(data) >> 4; chunkZ++) level.getChunk(chunkX, chunkZ);
        BlockPos center = BlockPos.of(data.getLong("position"));
        BlockPos respawn = findSafe(level, center, Set.of());
        if (respawn == null) return false;
        data.putLong(RESPAWN, respawn.asLong());
        var occupied = new LinkedHashSet<BlockPos>();
        occupied.add(respawn);
        int index = 0;
        for (UUID id : participantIds()) {
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player == null) continue;
            BlockPos preferred = index++ == 0 ? respawn : center.offset(slotOffset(index));
            BlockPos arrival = findSafe(level, preferred, occupied);
            if (arrival == null) {
                if (id.equals(data.getUUID(HOST))) arrival = respawn;
                else continue;
            }
            occupied.add(arrival);
            var member = SmpRecords.members(data).getCompound(id.toString());
            member.putLong(ARRIVAL, arrival.asLong());
            member.putBoolean(TELEPORTED, false);
            SmpRecords.members(data).put(id.toString(), member);
        }
        return SmpRecords.members(data).getCompound(data.getUUID(HOST).toString()).contains(ARRIVAL);
    }

    public void teleportParticipants(MinecraftServer server) {
        ServerLevel level = EventRegions.level(server, data);
        if (level == null) return;
        transition(State.TELEPORTING, level.getGameTime());
        for (UUID id : participantIds()) {
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            var member = SmpRecords.members(data).getCompound(id.toString());
            if (player == null || member.getBoolean(TELEPORTED) || !member.contains(ARRIVAL)) continue;
            BlockPos arrival = BlockPos.of(member.getLong(ARRIVAL));
            player.teleportTo(level, arrival.getX() + .5, arrival.getY(), arrival.getZ() + .5, player.getYRot(), player.getXRot());
            player.setDeltaMovement(Vec3.ZERO);
            player.fallDistance = 0;
            member.putBoolean(TELEPORTED, true);
        }
    }

    public void awaitEntry(ServerLevel level) {
        data.putBoolean(WALK_IN,true);
        transition(State.TELEPORTING,level.getGameTime());
    }

    public boolean tickStartup(MinecraftServer server) {
        ServerLevel level = EventRegions.level(server, data);
        if (level == null) return false;
        long now = level.getGameTime();
        if (state() == State.TELEPORTING) {
            if (data.getBoolean(WALK_IN)) {
                if (allParticipantsInside(server)) transition(State.ARRIVED, now);
            } else {
                boolean arrived = true;
                for (UUID id : participantIds()) {
                    ServerPlayer player = server.getPlayerList().getPlayer(id);
                    if (player == null) continue;
                    var member = SmpRecords.members(data).getCompound(id.toString());
                    BlockPos expected = member.contains(ARRIVAL) ? BlockPos.of(member.getLong(ARRIVAL)) : null;
                    if (expected == null || player.serverLevel() != level || !EventRegions.contains(data, level, player.blockPosition())
                            || player.distanceToSqr(Vec3.atBottomCenterOf(expected)) > 16) arrived = false;
                }
                if (arrived) transition(State.ARRIVED, now);
            }
        }
        if (state() == State.ARRIVED) {
            transition(State.INTRO, now);
            data.putLong("encounterIntroEnds", now + INTRO_TICKS);
        }
        if (state() == State.INTRO && now >= data.getLong("encounterIntroEnds")) transition(State.ACTIVE, now);
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
        BlockPos saved = data.contains(RESPAWN) ? BlockPos.of(data.getLong(RESPAWN)) : null;
        if (saved != null && safe(level, saved, Set.of())) return saved;
        BlockPos resolved = findSafe(level, BlockPos.of(data.getLong("position")), Set.of());
        if (resolved != null) data.putLong(RESPAWN, resolved.asLong());
        return resolved;
    }

    public void complete(MinecraftServer server, boolean success) {
        ServerLevel level = EventRegions.level(server, data);
        transition(success ? State.COMPLETED : State.FAILED, level == null ? server.getTickCount() : level.getGameTime());
        clearDisconnectTimers();
        clearStartupData();
    }

    public boolean returnPending(UUID playerId) {
        return isParticipant(playerId) && SmpRecords.members(data).getCompound(playerId.toString()).getBoolean(RETURN_PENDING);
    }

    public void stay(MinecraftServer server, UUID playerId) {
        SmpRecords.members(data).getCompound(playerId.toString()).remove(RETURN_PENDING);
        SmpData.get(server).changed(data);
    }

    public boolean returnPlayer(ServerPlayer player) {
        var member = SmpRecords.members(data).getCompound(player.getStringUUID());
        if (!member.getBoolean(RETURN_PENDING)) return false;
        return returnPlayerNow(player);
    }

    public boolean returnPlayerNow(ServerPlayer player) {
        var member = SmpRecords.members(data).getCompound(player.getStringUUID());
        if (!member.contains(RETURN_DIMENSION)) return false;
        ResourceLocation dimension = ResourceLocation.tryParse(member.getString(RETURN_DIMENSION));
        ServerLevel level = dimension == null ? null : player.server.getLevel(ResourceKey.create(Registries.DIMENSION, dimension));
        if (level == null) return false;
        player.teleportTo(level, member.getDouble(RETURN_X), member.getDouble(RETURN_Y), member.getDouble(RETURN_Z), member.getFloat(RETURN_YAW), member.getFloat(RETURN_PITCH));
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0;
        member.remove(RETURN_PENDING);
        SmpData.get(player.server).changed(data);
        return true;
    }

    public void returnParticipants(MinecraftServer server) {
        for (UUID id : participantIds()) {
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player != null) returnPlayerNow(player);
        }
    }

    public void resetAttempt() {
        data.remove(STATE);
        data.remove(HOST);
        data.remove(PARTICIPANTS);
        data.remove(RESPAWN);
        data.remove("encounterStateSince");
        data.remove("encounterIntroEnds");
        data.remove(WALK_IN);
        for (String id : SmpRecords.members(data).getAllKeys()) {
            CompoundTag member = SmpRecords.members(data).getCompound(id);
            member.remove(ARRIVAL);
            member.remove(TELEPORTED);
            member.remove(RETURN_PENDING);
            member.remove(RETURN_DIMENSION);
            member.remove(RETURN_X);
            member.remove(RETURN_Y);
            member.remove(RETURN_Z);
            member.remove(RETURN_YAW);
            member.remove(RETURN_PITCH);
            member.remove(DISCONNECTED_AT);
            member.remove(ABSENCE_DEFEAT_PENDING);
        }
    }

    public void clearStartupData() {
        data.remove(RESPAWN);
        data.remove(WALK_IN);
        data.remove("encounterIntroEnds");
        for (String id : SmpRecords.members(data).getAllKeys()) {
            CompoundTag member = SmpRecords.members(data).getCompound(id);
            member.remove(ARRIVAL);
            member.remove(TELEPORTED);
            member.remove(DISCONNECTED_AT);
            member.remove(ABSENCE_DEFEAT_PENDING);
        }
    }

    public void disconnected(ServerPlayer player) {
        if (!data.getString("state").equals("ACTIVE") || !data.getBoolean("combatStarted")
                || !isParticipant(player.getUUID()) || !new EventSession(data).accepted(player.getUUID())) return;
        SmpRecords.members(data).getCompound(player.getStringUUID()).putLong(DISCONNECTED_AT, System.currentTimeMillis());
        SmpData.get(player.server).changed(data);
    }

    public void reconnected(ServerPlayer player) {
        if (!isParticipant(player.getUUID())) return;
        var member = SmpRecords.members(data).getCompound(player.getStringUUID());
        if (member.getBoolean(ABSENCE_DEFEAT_PENDING)) {
            member.remove(ABSENCE_DEFEAT_PENDING);
            notifyAbsenceDefeat(player);
            if (!returnPlayerNow(player)) EventRegions.eject(player, data);
            SmpData.get(player.server).changed(data);
            return;
        }
        if (!member.contains(DISCONNECTED_AT)) return;
        long disconnectedAt = member.getLong(DISCONNECTED_AT);
        member.remove(DISCONNECTED_AT);
        if (System.currentTimeMillis() - disconnectedAt >= ABSENCE_DEFEAT_MILLIS) {
            member.putBoolean("eliminated", true);
            notifyAbsenceDefeat(player);
            if (!returnPlayerNow(player)) EventRegions.eject(player, data);
        } else if (data.getBoolean("solo") && data.getString("state").equals("ACTIVE") && data.getBoolean("combatStarted")) {
            returnToArena(player);
        }
        SmpData.get(player.server).changed(data);
    }

    public void expireDisconnected(MinecraftServer server, long now) {
        if (!data.getString("state").equals("ACTIVE") || !data.getBoolean("combatStarted")) return;
        boolean changed = false;
        for (UUID id : participantIds()) {
            var member = SmpRecords.members(data).getCompound(id.toString());
            if (member.getBoolean("eliminated") || !member.contains(DISCONNECTED_AT)
                    || server.getPlayerList().getPlayer(id) != null
                    || now - member.getLong(DISCONNECTED_AT) < ABSENCE_DEFEAT_MILLIS) continue;
            member.remove(DISCONNECTED_AT);
            member.putBoolean("eliminated", true);
            member.putBoolean(ABSENCE_DEFEAT_PENDING, true);
            changed = true;
        }
        if (changed) SmpData.get(server).changed(data);
    }

    public static CompoundTag activeFor(ServerPlayer player) {
        for (var row : SmpData.get(player.server).all("events"))
            if (row.getString("state").equals("ACTIVE") && row.getBoolean("combatStarted")
                    && new EncounterContext(row).isParticipant(player.getUUID())) return row;
        return null;
    }

    public static CompoundTag reconnectFor(ServerPlayer player) {
        CompoundTag latest = null;
        for (var row : SmpData.get(player.server).all("events")) {
            if (!new EncounterContext(row).isParticipant(player.getUUID())) continue;
            var member = SmpRecords.members(row).getCompound(player.getStringUUID());
            if (!member.contains(DISCONNECTED_AT) && !member.getBoolean(ABSENCE_DEFEAT_PENDING)) continue;
            if (latest == null || row.getLong("created") > latest.getLong("created")) latest = row;
        }
        return latest;
    }

    private void transition(State state, long tick) {
        data.putString(STATE, state.name());
        data.putLong("encounterStateSince", tick);
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
            var member = SmpRecords.members(data).getCompound(id.toString());
            member.remove(DISCONNECTED_AT);
        }
    }

    private static void notifyAbsenceDefeat(ServerPlayer player) {
        player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("jem.event.defeat_title")
                .withStyle(net.minecraft.ChatFormatting.RED, net.minecraft.ChatFormatting.BOLD));
        player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("jem.event.disconnect_defeat")
                .withStyle(net.minecraft.ChatFormatting.GOLD));
    }

    private static void recordReturn(ServerPlayer player, CompoundTag member) {
        if (member.contains(RETURN_DIMENSION)) return;
        member.putString(RETURN_DIMENSION, player.serverLevel().dimension().location().toString());
        member.putDouble(RETURN_X, player.getX());
        member.putDouble(RETURN_Y, player.getY());
        member.putDouble(RETURN_Z, player.getZ());
        member.putFloat(RETURN_YAW, player.getYRot());
        member.putFloat(RETURN_PITCH, player.getXRot());
    }

    private BlockPos findSafe(ServerLevel level, BlockPos anchor, Set<BlockPos> occupied) {
        List<BlockPos> candidates = new ArrayList<>();
        for (int radius = 0; radius <= SEARCH_RADIUS; radius++)
            for (int x = -radius; x <= radius; x++)
                for (int z = -radius; z <= radius; z++)
                    if (radius == 0 || Math.abs(x) == radius || Math.abs(z) == radius) candidates.add(anchor.offset(x, 0, z));
        int minY=data.contains("minY")?Math.max(level.getMinBuildHeight()+1,data.getInt("minY")):level.getMinBuildHeight()+1;
        int maxY=data.contains("maxY")?Math.min(level.getMaxBuildHeight()-2,data.getInt("maxY")):level.getMaxBuildHeight()-2;
        int anchorY=Math.max(minY,Math.min(maxY,anchor.getY()));
        if(data.getString("activity").equals("BLOOD_MOON")) {
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
