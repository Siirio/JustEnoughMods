package com.siirio.jemworldbosstiers.progression;

import com.siirio.jemworldbosstiers.balance.BalanceRegistry;
import com.siirio.jemworldbosstiers.balance.TierValues;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Collection;
import java.util.Optional;
import java.util.List;
import com.siirio.jemworldbosstiers.revival.ArenaRecord;

public final class WorldTierData extends SavedData {
    private static final String FILE_ID = "jem_world_boss_tiers";
    private static final String SCHEMA_VERSION = "SchemaVersion";
    private static final int CURRENT_SCHEMA_VERSION = 2;
    private static final String DEFEATED_BOSSES = "DefeatedBosses";
    private static final String ARENAS = "Arenas";
    private static final String TIER_OVERRIDE = "TierOverride";
    private static final String RESPAWNS = "ScheduledRespawns";
    private static final String VERIFIED_ANCHORS = "VerifiedNativeAnchors";
    private static final String RAID_PARTICIPATION = "RaidParticipation";
    private static final String RAID_REPLACEMENTS = "PendingRaidReplacements";
    private final Set<ResourceLocation> defeatedBosses = new LinkedHashSet<>();
    private final Map<String, ArenaRecord> arenas = new LinkedHashMap<>();
    private final Map<String, RespawnState> respawns = new LinkedHashMap<>();
    private final Set<String> verifiedAnchors = new LinkedHashSet<>();
    private final Map<String, Set<java.util.UUID>> raidParticipation = new LinkedHashMap<>();
    private final Set<String> pendingRaidReplacements = new LinkedHashSet<>();
    private Integer tierOverride;

    public static WorldTierData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(WorldTierData::load, WorldTierData::new, FILE_ID);
    }

    public int tier() {
        return tierOverride == null ? BalanceRegistry.worldTierForDefeats(defeatedBosses) : tierOverride;
    }

    public Optional<Integer> tierOverride() {
        return Optional.ofNullable(tierOverride);
    }

    public void setTierOverride(int tier) {
        if (tier < 1 || tier > TierValues.TIER_COUNT) throw new IllegalArgumentException("Tier must be between one and five");
        if (java.util.Objects.equals(tierOverride,tier)) return;
        tierOverride=tier;
        setDirty();
    }

    public void clearTierOverride() {
        if(tierOverride==null) return;
        tierOverride=null;
        setDirty();
    }

    public boolean recordDefeat(ResourceLocation bossKey) {
        if (!defeatedBosses.add(bossKey)) {
            return false;
        }
        setDirty();
        return true;
    }

    public Set<ResourceLocation> defeatedBosses() {
        return Set.copyOf(defeatedBosses);
    }

    public int activeDefeatCount() {
        return ProgressionRules.activeDefeatCount(defeatedBosses, BalanceRegistry.activeQualifyingKeys());
    }

    public void putArena(ArenaRecord arena) {
        arenas.put(arena.id(), arena);
        setDirty();
    }

    public Optional<ArenaRecord> arena(String id) {
        return Optional.ofNullable(arenas.get(id));
    }

    public Collection<ArenaRecord> arenas() {
        return List.copyOf(arenas.values());
    }

    public void verifyNativeAnchor(String arenaId) {
        if (verifiedAnchors.add(arenaId)) setDirty();
    }

    public boolean nativeAnchorVerified(String arenaId) {
        return verifiedAnchors.contains(arenaId);
    }

    public void scheduleRespawn(String arenaId, ResourceLocation entityType, long dueTime) {
        respawns.put(arenaId, new RespawnState(entityType, dueTime));
        setDirty();
    }

    public Optional<RespawnState> respawn(String arenaId) {
        return Optional.ofNullable(respawns.get(arenaId));
    }

    public Collection<Map.Entry<String, RespawnState>> respawns() {
        return List.copyOf(respawns.entrySet());
    }

    public void cancelRespawn(String arenaId) {
        if (respawns.remove(arenaId) != null) setDirty();
    }

    public boolean hasRaidParticipation(String arenaId, java.util.UUID playerId) {
        return raidParticipation.getOrDefault(arenaId, Set.of()).contains(playerId);
    }

    public void recordRaidParticipation(String arenaId, Collection<java.util.UUID> players) {
        if (raidParticipation.computeIfAbsent(arenaId, ignored -> new LinkedHashSet<>()).addAll(players)) setDirty();
    }

    public boolean raidReplacementPending(String arenaId) { return pendingRaidReplacements.contains(arenaId); }

    public void markRaidReplacement(String arenaId) {
        if (pendingRaidReplacements.add(arenaId)) setDirty();
    }

    public void clearRaidReplacement(String arenaId) {
        if (pendingRaidReplacements.remove(arenaId)) setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putInt(SCHEMA_VERSION, CURRENT_SCHEMA_VERSION);
        if(tierOverride!=null) tag.putInt(TIER_OVERRIDE,tierOverride);
        ListTag values = new ListTag();
        defeatedBosses.forEach(id -> values.add(StringTag.valueOf(id.toString())));
        tag.put(DEFEATED_BOSSES, values);
        ListTag arenaValues = new ListTag();
        arenas.values().forEach(arena -> arenaValues.add(arena.save()));
        tag.put(ARENAS, arenaValues);
        ListTag respawnValues = new ListTag();
        respawns.forEach((arenaId, state) -> {
            CompoundTag value = new CompoundTag();
            value.putString("Arena", arenaId);
            value.putString("Entity", state.entityType().toString());
            value.putLong("Due", state.dueTime());
            respawnValues.add(value);
        });
        tag.put(RESPAWNS, respawnValues);
        ListTag anchors = new ListTag();
        verifiedAnchors.forEach(id -> anchors.add(StringTag.valueOf(id)));
        tag.put(VERIFIED_ANCHORS, anchors);
        ListTag participation = new ListTag();
        raidParticipation.forEach((arenaId, players) -> {
            CompoundTag value = new CompoundTag();
            value.putString("Arena", arenaId);
            ListTag ids = new ListTag();
            players.forEach(id -> ids.add(StringTag.valueOf(id.toString())));
            value.put("Players", ids);
            participation.add(value);
        });
        tag.put(RAID_PARTICIPATION, participation);
        ListTag replacements = new ListTag();
        pendingRaidReplacements.forEach(id -> replacements.add(StringTag.valueOf(id)));
        tag.put(RAID_REPLACEMENTS, replacements);
        return tag;
    }

    static WorldTierData load(CompoundTag tag) {
        WorldTierData data = new WorldTierData();
        if (tag.getInt(SCHEMA_VERSION) != CURRENT_SCHEMA_VERSION) {
            return data;
        }
        if(tag.contains(TIER_OVERRIDE,Tag.TAG_INT)) data.tierOverride=tag.getInt(TIER_OVERRIDE);
        ListTag values = tag.getList(DEFEATED_BOSSES, Tag.TAG_STRING);
        for (Tag value : values) {
            ResourceLocation id = ResourceLocation.tryParse(value.getAsString());
            if (id != null) {
                data.defeatedBosses.add(id);
            }
        }
        ListTag arenaValues = tag.getList(ARENAS, Tag.TAG_COMPOUND);
        for (Tag value : arenaValues) {
            ArenaRecord.load((CompoundTag) value).ifPresent(arena -> data.arenas.put(arena.id(), arena));
        }
        for (Tag value : tag.getList(RESPAWNS, Tag.TAG_COMPOUND)) {
            CompoundTag state = (CompoundTag) value;
            ResourceLocation entity = ResourceLocation.tryParse(state.getString("Entity"));
            if (!state.getString("Arena").isBlank() && entity != null && state.getLong("Due") > 0)
                data.respawns.put(state.getString("Arena"), new RespawnState(entity, state.getLong("Due")));
        }
        for (Tag value : tag.getList(VERIFIED_ANCHORS, Tag.TAG_STRING)) data.verifiedAnchors.add(value.getAsString());
        for (Tag value : tag.getList(RAID_PARTICIPATION, Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) value;
            Set<java.util.UUID> players = new LinkedHashSet<>();
            for (Tag id : entry.getList("Players", Tag.TAG_STRING)) try { players.add(java.util.UUID.fromString(id.getAsString())); } catch (IllegalArgumentException ignored) {}
            if (!entry.getString("Arena").isBlank()) data.raidParticipation.put(entry.getString("Arena"), players);
        }
        for (Tag value : tag.getList(RAID_REPLACEMENTS, Tag.TAG_STRING)) data.pendingRaidReplacements.add(value.getAsString());
        return data;
    }

    public record RespawnState(ResourceLocation entityType, long dueTime) {}
}
