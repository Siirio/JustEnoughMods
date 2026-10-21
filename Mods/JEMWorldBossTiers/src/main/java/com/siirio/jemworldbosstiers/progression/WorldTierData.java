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
    private final Map<String, java.util.UUID> liveBosses = new LinkedHashMap<>();
    private final Map<String, Set<java.util.UUID>> conflictingBosses = new LinkedHashMap<>();
    private final Map<String, String> arenaAliases = new LinkedHashMap<>();
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
        return Optional.ofNullable(arenas.get(canonicalArenaId(id)));
    }

    public Collection<ArenaRecord> arenas() {
        return arenas.values().stream().filter(arena -> !arenaAliases.containsKey(arena.id())).toList();
    }

    public String canonicalArenaId(String id) {
        var visited = new java.util.HashSet<String>();
        while (arenaAliases.containsKey(id) && visited.add(id)) id = arenaAliases.get(id);
        return id;
    }

    public Optional<java.util.UUID> liveBoss(String arenaId) {
        String id = canonicalArenaId(arenaId);
        java.util.UUID keeper = liveBosses.get(id);
        return keeper != null ? Optional.of(keeper) : conflictingBosses.getOrDefault(id, Set.of()).stream().sorted().findFirst();
    }

    public boolean recordLiveBoss(String arenaId, java.util.UUID bossId) {
        String id = canonicalArenaId(arenaId);
        java.util.UUID current = liveBoss(id).orElse(null);
        if (current != null) return current.equals(bossId);
        liveBosses.put(id, bossId);
        setDirty();
        return true;
    }

    public boolean recordLiveBoss(String arenaId, net.minecraft.world.entity.LivingEntity boss) {
        ArenaRecord arena = arena(arenaId).orElse(null);
        var profile = com.siirio.jemworldbosstiers.api.WorldTierApi.profile(boss).orElse(null);
        var encounter = com.siirio.jemworldbosstiers.api.WorldTierApi.encounter(boss).orElse(null);
        if (arena == null || profile == null || encounter == null || encounter.arenaId() == null
                || !arena.profileKey().equals(profile.key()) || !arena.dimension().equals(boss.level().dimension().location().toString())
                || !canonicalArenaId(arenaId).equals(canonicalArenaId(encounter.arenaId()))) return false;
        return recordLiveBoss(arenaId, boss.getUUID());
    }

    public void clearLiveBoss(String arenaId, java.util.UUID bossId) {
        String id = canonicalArenaId(arenaId);
        boolean removed = liveBosses.remove(id, bossId);
        var conflicts = conflictingBosses.get(id);
        if (conflicts != null) removed |= conflicts.remove(bossId);
        if (!liveBosses.containsKey(id) && conflicts != null && !conflicts.isEmpty()) {
            java.util.UUID promoted = conflicts.stream().sorted().findFirst().orElseThrow();
            liveBosses.put(id, promoted);
            conflicts.remove(promoted);
            removed = true;
        }
        if (removed) setDirty();
    }

    public boolean observeLiveBoss(String arenaId, net.minecraft.world.entity.LivingEntity boss) {
        var owner = arena(arenaId).orElse(null);
        var profile = com.siirio.jemworldbosstiers.api.WorldTierApi.profile(boss).orElse(null);
        var encounter = com.siirio.jemworldbosstiers.api.WorldTierApi.encounter(boss).orElse(null);
        if (owner == null || profile == null || encounter == null || encounter.arenaId() == null
                || !owner.profileKey().equals(profile.key()) || !owner.dimension().equals(boss.level().dimension().location().toString())
                || !canonicalArenaId(arenaId).equals(canonicalArenaId(encounter.arenaId()))) return false;
        if (recordLiveBoss(arenaId, boss)) return true;
        if (conflictingBosses.computeIfAbsent(canonicalArenaId(arenaId), ignored -> new LinkedHashSet<>()).add(boss.getUUID())) setDirty();
        return false;
    }

    public boolean replaceLiveBoss(String arenaId, java.util.UUID expected, java.util.UUID replacement) {
        if (!liveBosses.replace(canonicalArenaId(arenaId), expected, replacement)) return false;
        conflictingBosses.computeIfAbsent(canonicalArenaId(arenaId), ignored -> new LinkedHashSet<>()).remove(replacement);
        setDirty();
        return true;
    }

    /** Aliases retain old records, but only one canonical arena owns timers and admission. */
    public void aliasArena(String formerId, String structureId) {
        String from = canonicalArenaId(formerId), to = canonicalArenaId(structureId);
        if (from.equals(to) || !arenas.containsKey(from) || !arenas.containsKey(to)) return;
        ArenaRecord old = arenas.get(from), target = arenas.get(to);
        if (old.structureArena() || !target.structureArena() || !old.profileKey().equals(target.profileKey())
                || !old.dimension().equals(target.dimension())) return;
        var center = old.center();
        String legacyOriginId = old.dimension() + '|' + old.profileKey() + '|' + center.getX() + '|' + center.getZ();
        if (!old.id().equals(legacyOriginId)) return;
        if (center.getX() < target.minX() || center.getX() > target.maxX()
                || center.getZ() < target.minZ() || center.getZ() > target.maxZ()) return;
        arenaAliases.put(from, to);
        if (old.unlocked()) arenas.put(to, target.unlock());
        if (target.activeEncounterId() != null) liveBosses.putIfAbsent(to, target.activeEncounterId());
        var claim = liveBosses.getOrDefault(from, old.activeEncounterId());
        if (claim != null) liveBosses.putIfAbsent(to, claim);
        if (claim != null && !claim.equals(liveBosses.get(to))) conflictingBosses.computeIfAbsent(to, ignored -> new LinkedHashSet<>()).add(claim);
        conflictingBosses.computeIfAbsent(to, ignored -> new LinkedHashSet<>()).addAll(conflictingBosses.getOrDefault(from, Set.of()));
        RespawnState previous = respawns.remove(from), current = respawns.get(to);
        if (previous != null && (current == null || previous.dueTime() < current.dueTime())) respawns.put(to, previous);
        var participants = raidParticipation.get(from);
        if (participants != null) raidParticipation.computeIfAbsent(to, ignored -> new LinkedHashSet<>()).addAll(participants);
        if (pendingRaidReplacements.remove(from)) pendingRaidReplacements.add(to);
        setDirty();
    }

    /** Native storage must acknowledge the exact persisted claim before spawning a replacement. */
    public boolean flush(MinecraftServer server) {
        CompoundTag expected = save(new CompoundTag());
        setDirty();
        server.overworld().getDataStorage().save();
        var path = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)
                .resolve("data").resolve(FILE_ID + ".dat").toFile();
        try {
            CompoundTag persisted = net.minecraft.nbt.NbtIo.readCompressed(path);
            if (persisted.getCompound("data").equals(expected)) return true;
        } catch (java.io.IOException error) {
            com.mojang.logging.LogUtils.getLogger().error("Boss identity storage acknowledgement failed", error);
        }
        setDirty();
        return false;
    }

    public void verifyNativeAnchor(String arenaId) {
        arenaId = canonicalArenaId(arenaId);
        if (verifiedAnchors.add(arenaId)) setDirty();
    }

    public boolean nativeAnchorVerified(String arenaId) {
        arenaId = canonicalArenaId(arenaId);
        return verifiedAnchors.contains(arenaId);
    }

    public void scheduleRespawn(String arenaId, ResourceLocation entityType, long dueTime) {
        arenaId = canonicalArenaId(arenaId);
        respawns.put(arenaId, new RespawnState(entityType, dueTime));
        setDirty();
    }

    public Optional<RespawnState> respawn(String arenaId) {
        arenaId = canonicalArenaId(arenaId);
        return Optional.ofNullable(respawns.get(arenaId));
    }

    public Collection<Map.Entry<String, RespawnState>> respawns() {
        return List.copyOf(respawns.entrySet());
    }

    public void cancelRespawn(String arenaId) {
        arenaId = canonicalArenaId(arenaId);
        if (respawns.remove(arenaId) != null) setDirty();
    }

    public boolean hasRaidParticipation(String arenaId, java.util.UUID playerId) {
        arenaId = canonicalArenaId(arenaId);
        return raidParticipation.getOrDefault(arenaId, Set.of()).contains(playerId);
    }

    public void recordRaidParticipation(String arenaId, Collection<java.util.UUID> players) {
        arenaId = canonicalArenaId(arenaId);
        if (raidParticipation.computeIfAbsent(arenaId, ignored -> new LinkedHashSet<>()).addAll(players)) setDirty();
    }

    public boolean raidReplacementPending(String arenaId) {
        arenaId = canonicalArenaId(arenaId); return pendingRaidReplacements.contains(arenaId); }

    public void markRaidReplacement(String arenaId) {
        arenaId = canonicalArenaId(arenaId);
        if (pendingRaidReplacements.add(arenaId)) setDirty();
    }

    public void clearRaidReplacement(String arenaId) {
        arenaId = canonicalArenaId(arenaId);
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
        ListTag claims = new ListTag();
        liveBosses.forEach((arena, uuid) -> { CompoundTag claim = new CompoundTag(); claim.putString("Arena", arena); claim.putUUID("Boss", uuid); claims.add(claim); });
        tag.put("LiveBossIdentities", claims);
        ListTag conflicts = new ListTag();
        conflictingBosses.forEach((arena, ids) -> ids.forEach(uuid -> { CompoundTag claim = new CompoundTag(); claim.putString("Arena", arena); claim.putUUID("Boss", uuid); conflicts.add(claim); }));
        tag.put("ConflictingBossIdentities", conflicts);
        ListTag aliases = new ListTag();
        arenaAliases.forEach((from, to) -> { CompoundTag alias = new CompoundTag(); alias.putString("From", from); alias.putString("To", to); aliases.add(alias); });
        tag.put("ArenaAliases", aliases);
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
        for (Tag value : tag.getList("LiveBossIdentities", Tag.TAG_COMPOUND)) {
            CompoundTag claim = (CompoundTag)value;
            if (claim.hasUUID("Boss") && !claim.getString("Arena").isBlank()) data.liveBosses.put(claim.getString("Arena"), claim.getUUID("Boss"));
        }
        for (Tag value : tag.getList("ArenaAliases", Tag.TAG_COMPOUND)) {
            CompoundTag alias = (CompoundTag)value;
            if (data.arenas.containsKey(alias.getString("From")) && data.arenas.containsKey(alias.getString("To"))
                    && !alias.getString("From").equals(alias.getString("To"))) data.arenaAliases.put(alias.getString("From"), alias.getString("To"));
        }
        for (Tag value : tag.getList("ConflictingBossIdentities", Tag.TAG_COMPOUND)) {
            CompoundTag claim = (CompoundTag)value;
            if (claim.hasUUID("Boss") && !claim.getString("Arena").isBlank())
                data.conflictingBosses.computeIfAbsent(data.canonicalArenaId(claim.getString("Arena")), ignored -> new LinkedHashSet<>()).add(claim.getUUID("Boss"));
        }
        return data;
    }

    public record RespawnState(ResourceLocation entityType, long dueTime) {}
}
