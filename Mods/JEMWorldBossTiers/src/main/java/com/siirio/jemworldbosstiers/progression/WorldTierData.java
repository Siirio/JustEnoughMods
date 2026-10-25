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
    private final Map<String, Map<java.util.UUID, java.util.UUID>> raidParticipationAttempts = new LinkedHashMap<>();
    private final Set<String> pendingRaidReplacements = new LinkedHashSet<>();
    private final Map<String, java.util.UUID> liveBosses = new LinkedHashMap<>();
    private final Map<String, Set<java.util.UUID>> conflictingBosses = new LinkedHashMap<>();
    private final Map<String, String> arenaAliases = new LinkedHashMap<>();
    private final Map<java.util.UUID, CompoundTag> hostedSettlements = new LinkedHashMap<>();
    private final Map<java.util.UUID, CompoundTag> hostedAttempts = new LinkedHashMap<>();
    private final Set<java.util.UUID> completedHostedSettlements = new LinkedHashSet<>();
    private final Set<java.util.UUID> canonicalHostedClosures = new LinkedHashSet<>();
    private final Set<java.util.UUID> failedHostedAttempts = new LinkedHashSet<>();
    private final Map<String, CompoundTag> raidRetirements = new LinkedHashMap<>();
    private final Map<String, CompoundTag> pendingBossCreations = new LinkedHashMap<>();
    private final Map<java.util.UUID, String> retiredBossIdentities = new LinkedHashMap<>();
    private Integer tierOverride;
    private final Set<ResourceLocation> progressionRoster = new LinkedHashSet<>();
    private List<Integer> progressionThresholds = List.of();
    private boolean legacyProfile;
    private long ownershipRevision;
    private CompoundTag nonExclusiveArenaHistory = new CompoundTag();

    public static WorldTierData get(MinecraftServer server) {
        WorldTierData data = server.overworld().getDataStorage().computeIfAbsent(WorldTierData::load, WorldTierData::new, FILE_ID);
        data.initializeProgressionProfile();
        data.migrateNonExclusiveArenaIdentities(server);
        return data;
    }

    /** Reconcile after profiles load or reload, never from an incomplete registry. */
    private void migrateNonExclusiveArenaIdentities(MinecraftServer server) {
        long revision = BalanceRegistry.bossRevision();
        if (revision == 0 || revision == ownershipRevision) return;
        ownershipRevision = revision;
        for (ArenaRecord arena : List.copyOf(arenas.values())) {
            var profile = BalanceRegistry.bossByKey(arena.profileKey()).orElse(null);
            if (profile == null || profile.ownsExclusiveArena()) continue;
            String id = arena.id();
            Set<java.util.UUID> loadedIdentities = new LinkedHashSet<>();
            CompoundTag archived = nonExclusiveArenaHistory.getCompound(id);
            boolean changed = false;
            if (arena.activeEncounterId() != null) {
                loadedIdentities.add(arena.activeEncounterId());
                archived.put("Arena", arena.save());
                arenas.put(id, arena.release(arena.activeEncounterId()));
                changed = true;
            }
            java.util.UUID keeper = liveBosses.remove(id);
            if (keeper != null) { loadedIdentities.add(keeper); archived.putUUID("LiveBoss", keeper); changed = true; }
            var conflicts = conflictingBosses.remove(id);
            if (conflicts != null && !conflicts.isEmpty()) {
                loadedIdentities.addAll(conflicts);
                ListTag previous = archived.getList("ConflictingBosses", Tag.TAG_COMPOUND);
                for (var uuid : conflicts) {
                    CompoundTag row = new CompoundTag(); row.putUUID("Boss", uuid); previous.add(row);
                }
                archived.put("ConflictingBosses", previous);
                changed = true;
            }
            RespawnState respawn = respawns.remove(id);
            if (respawn != null) {
                CompoundTag row = new CompoundTag(); row.putString("Entity", respawn.entityType().toString());
                row.putLong("Due", respawn.dueTime());
                if (respawn.deathId() != null) row.putUUID("DeathId", respawn.deathId());
                archived.put("ScheduledRespawn", row); changed = true;
            }
            CompoundTag creation = pendingBossCreations.remove(id), retirement = raidRetirements.remove(id);
            if (creation != null) {
                archived.put("PendingCreation", creation); changed = true;
                if (creation.hasUUID("UUID")) loadedIdentities.add(creation.getUUID("UUID"));
            }
            if (retirement != null) { archived.put("RaidRetirement", retirement); changed = true; }
            if (pendingRaidReplacements.remove(id)) { archived.putBoolean("RaidReplacement", true); changed = true; }
            if (verifiedAnchors.remove(id)) { archived.putBoolean("VerifiedAnchor", true); changed = true; }
            for (var uuid : loadedIdentities) releaseNonExclusiveEntity(server, arena, uuid);
            if (changed) {
                if (!archived.contains("Arena")) archived.put("Arena", arena.save());
                nonExclusiveArenaHistory.put(id, archived);
                setDirty();
            }
        }
        // One pass over tombstones, rather than a full scan for every saved arena.
        var retiredEntries = retiredBossIdentities.entrySet().iterator();
        while (retiredEntries.hasNext()) {
            var entry = retiredEntries.next();
            ArenaRecord arena = arenas.get(canonicalArenaId(entry.getValue()));
            var profile = arena == null ? null : BalanceRegistry.bossByKey(arena.profileKey()).orElse(null);
            if (profile == null || profile.ownsExclusiveArena()) continue;
            CompoundTag archived = nonExclusiveArenaHistory.getCompound(arena.id());
            if (!archived.contains("Arena")) archived.put("Arena", arena.save());
            ListTag retired = archived.getList("RetiredBosses", Tag.TAG_COMPOUND);
            CompoundTag row = new CompoundTag(); row.putUUID("Boss", entry.getKey()); retired.add(row);
            archived.put("RetiredBosses", retired);
            nonExclusiveArenaHistory.put(arena.id(), archived);
            releaseNonExclusiveEntity(server, arena, entry.getKey());
            retiredEntries.remove(); setDirty();
        }
    }

    private static void releaseNonExclusiveEntity(MinecraftServer server, ArenaRecord arena, java.util.UUID uuid) {
        var dimension = ResourceLocation.tryParse(arena.dimension());
        var level = dimension == null ? null : server.getLevel(net.minecraft.resources.ResourceKey.create(
                net.minecraft.core.registries.Registries.DIMENSION, dimension));
        if (level == null || !(level.getEntity(uuid) instanceof net.minecraft.world.entity.LivingEntity living)
                || com.siirio.jemworldbosstiers.api.WorldTierApi.profile(living)
                .filter(profile -> !profile.ownsExclusiveArena()).isEmpty()) return;
        com.siirio.jemworldbosstiers.encounter.EncounterData.detachArena(living.getPersistentData());
        com.siirio.jemworldbosstiers.revival.NativeBossStorage.releaseNonExclusiveCreation(living);
        if (!com.siirio.jemworldbosstiers.encounter.HostedGroup.member(living)
                && !com.siirio.jemworldbosstiers.encounter.HostedEncounters.isHosted(living))
            com.siirio.jemworldbosstiers.encounter.HostedLobby.release(living);
    }

    public int tier() {
        initializeProgressionProfile();
        return tierOverride == null ? ProgressionRules.tier(defeatedBosses, progressionRoster, tierThresholds()) : tierOverride;
    }

    private void initializeProgressionProfile() {
        if (!progressionThresholds.isEmpty() || BalanceRegistry.qualifyingKeys().isEmpty()) return;
        Set<ResourceLocation> full = BalanceRegistry.qualifyingKeys();
        Set<ResourceLocation> available = legacyProfile ? full : BalanceRegistry.activeQualifyingKeys();
        progressionRoster.addAll(available);
        var thresholds = new java.util.ArrayList<Integer>();
        thresholds.add(0);
        for (int configured : BalanceRegistry.tierThresholds().stream().skip(1).toList()) {
            int scaled = legacyProfile || available.size() == full.size() ? configured
                    : Math.max(1, (int) Math.ceil((double) configured * available.size() / full.size()));
            int next = Math.max(thresholds.get(thresholds.size() - 1) + 1, scaled);
            if (!legacyProfile && next > available.size()) break;
            thresholds.add(next);
        }
        progressionThresholds = List.copyOf(thresholds);
        setDirty();
    }

    public List<Integer> tierThresholds() {
        return progressionThresholds.isEmpty() ? List.of(0) : progressionThresholds;
    }

    public Set<ResourceLocation> progressionRoster() {
        return Set.copyOf(progressionRoster);
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
        return ProgressionRules.activeDefeatCount(defeatedBosses, progressionRoster);
    }

    public void putArena(ArenaRecord arena) {
        arenas.put(arena.id(), arena);
        setDirty();
    }

    public Optional<ArenaRecord> arena(String id) {
        return Optional.ofNullable(arenas.get(canonicalArenaId(id)))
                .filter(arena -> BalanceRegistry.bossByKey(arena.profileKey())
                        .map(com.siirio.jemworldbosstiers.balance.BossProfile::ownsExclusiveArena).orElse(true));
    }

    public Collection<ArenaRecord> arenas() {
        return arenas.values().stream().filter(arena -> !arenaAliases.containsKey(arena.id()))
                .filter(arena -> BalanceRegistry.bossByKey(arena.profileKey())
                        .map(com.siirio.jemworldbosstiers.balance.BossProfile::ownsExclusiveArena).orElse(true)).toList();
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
        if (arena(arenaId).flatMap(arena -> BalanceRegistry.bossByKey(arena.profileKey()))
                .filter(com.siirio.jemworldbosstiers.balance.BossProfile::ownsExclusiveArena).isEmpty()) return false;
        if (bossIdentityRetired(bossId)) return false;
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
        if (owner == null || profile == null || !profile.ownsExclusiveArena() || encounter == null || encounter.arenaId() == null
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
        var participationAttempts = raidParticipationAttempts.remove(from);
        if (participationAttempts != null)
            participationAttempts.forEach((player, attempt) -> raidParticipationAttempts
                    .computeIfAbsent(to, ignored -> new LinkedHashMap<>()).putIfAbsent(player, attempt));
        if (pendingRaidReplacements.remove(from)) pendingRaidReplacements.add(to);
        CompoundTag retirement = raidRetirements.remove(from);
        if (retirement != null) {
            CompoundTag conflicting = raidRetirements.putIfAbsent(to, retirement);
            if (conflicting != null && !conflicting.equals(retirement))
                throw new IllegalStateException("Conflicting legacy Raid retirement evidence");
        }
        CompoundTag creation = pendingBossCreations.remove(from);
        if (creation != null) {
            CompoundTag conflicting = pendingBossCreations.putIfAbsent(to, creation);
            if (conflicting != null && !conflicting.getUUID("UUID").equals(creation.getUUID("UUID")))
                throw new IllegalStateException("Conflicting legacy boss creation identity");
        }
        setDirty();
    }

    /** Native storage must acknowledge the exact persisted claim before spawning a replacement. */
    public boolean flush(MinecraftServer server) {
        CompoundTag expected = save(new CompoundTag());
        setDirty();
        try {
            server.overworld().getDataStorage().save();
            var path = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)
                    .resolve("data").resolve(FILE_ID + ".dat").toFile();
            CompoundTag persisted = net.minecraft.nbt.NbtIo.readCompressed(path);
            if (persisted.getCompound("data").equals(expected)) return true;
        } catch (java.io.IOException | RuntimeException error) {
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
        scheduleRespawn(arenaId, entityType, dueTime, null);
    }

    public void scheduleRespawn(String arenaId, ResourceLocation entityType, long dueTime, java.util.UUID deathId) {
        arenaId = canonicalArenaId(arenaId);
        RespawnState previous = respawns.get(arenaId);
        if (previous != null && (deathId == null || java.util.Objects.equals(previous.deathId(), deathId))) return;
        respawns.put(arenaId, new RespawnState(entityType, dueTime, deathId));
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

    public void recordRaidParticipation(String arenaId, Collection<java.util.UUID> players, java.util.UUID attempt) {
        arenaId = canonicalArenaId(arenaId);
        var participants = raidParticipation.computeIfAbsent(arenaId, ignored -> new LinkedHashSet<>());
        var attempts = raidParticipationAttempts.computeIfAbsent(arenaId, ignored -> new LinkedHashMap<>());
        for (java.util.UUID player : players) {
            if (participants.add(player)) { attempts.put(player, attempt); setDirty(); }
            else if (!attempt.equals(attempts.get(player))) throw new IllegalStateException("Raid participation belongs to another attempt");
        }
    }

    public void rollbackRaidParticipation(String arenaId, java.util.UUID attempt, Collection<java.util.UUID> players) {
        arenaId = canonicalArenaId(arenaId);
        var attempts = raidParticipationAttempts.get(arenaId);
        var participants = raidParticipation.get(arenaId);
        if (attempts == null || participants == null) return;
        for (java.util.UUID player : players) if (attempts.remove(player, attempt)) { participants.remove(player); setDirty(); }
    }

    public boolean raidReplacementPending(String arenaId) {
        arenaId = canonicalArenaId(arenaId); return pendingRaidReplacements.contains(arenaId); }

    public void markRaidReplacement(String arenaId) {
        arenaId = canonicalArenaId(arenaId);
        if (pendingRaidReplacements.add(arenaId)) setDirty();
    }

    public void clearRaidReplacement(String arenaId) {
        arenaId = canonicalArenaId(arenaId);
        if (raidRetirements.remove(arenaId) != null) setDirty();
        if (pendingRaidReplacements.remove(arenaId)) setDirty();
    }

    public void beginRaidRetirement(String arenaId, java.util.UUID boss, net.minecraft.core.BlockPos position) {
        arenaId = canonicalArenaId(arenaId);
        CompoundTag previous = raidRetirements.get(arenaId);
        if (previous != null) {
            if (!previous.getUUID("Boss").equals(boss)) throw new IllegalStateException("Conflicting Raid retirement identity");
            return;
        }
        CompoundTag intent = new CompoundTag();
        intent.putUUID("Boss", boss);
        intent.putLong("Position", position.asLong());
        raidRetirements.put(arenaId, intent);
        markRaidReplacement(arenaId);
    }

    public Optional<CompoundTag> raidRetirement(String arenaId) {
        CompoundTag intent = raidRetirements.get(canonicalArenaId(arenaId));
        return Optional.ofNullable(intent == null ? null : intent.copy());
    }

    public boolean canonicalHostedClosed(java.util.UUID attempt) { return canonicalHostedClosures.contains(attempt); }

    public void markCanonicalHostedClosed(java.util.UUID attempt) {
        if (canonicalHostedClosures.add(attempt)) setDirty();
    }

    public void beginBossCreation(String arenaId, CompoundTag entity) {
        if (!entity.hasUUID("UUID") || !entity.contains("id", Tag.TAG_STRING))
            throw new IllegalStateException("Boss creation requires exact native identity and type");
        arenaId = canonicalArenaId(arenaId);
        CompoundTag previous = pendingBossCreations.putIfAbsent(arenaId, entity.copy());
        if (previous != null && !previous.getUUID("UUID").equals(entity.getUUID("UUID")))
            throw new IllegalStateException("A different boss creation is unresolved");
        if (previous == null) setDirty();
    }

    public Map<String, CompoundTag> pendingBossCreations() {
        Map<String, CompoundTag> snapshots = new LinkedHashMap<>();
        pendingBossCreations.forEach((arena, entity) -> snapshots.put(arena, entity.copy()));
        return java.util.Collections.unmodifiableMap(snapshots);
    }

    public void clearBossCreation(String arenaId, java.util.UUID boss) {
        arenaId = canonicalArenaId(arenaId);
        CompoundTag current = pendingBossCreations.get(arenaId);
        if (current != null && current.getUUID("UUID").equals(boss)) {
            pendingBossCreations.remove(arenaId);
            setDirty();
        }
    }

    public void retireBossIdentity(String arenaId, java.util.UUID boss) {
        String canonical = canonicalArenaId(arenaId);
        String previous = retiredBossIdentities.putIfAbsent(boss, canonical);
        if (previous == null) setDirty();
        else if (!canonicalArenaId(previous).equals(canonical))
            throw new IllegalStateException("Retired boss identity belongs to another arena");
    }

    public boolean bossIdentityRetired(java.util.UUID boss) { return retiredBossIdentities.containsKey(boss); }

    /** Frozen attempt evidence must outlive removal of its dead boss entity. */
    public void putHostedSettlement(java.util.UUID attempt, CompoundTag snapshot) {
        if (completedHostedSettlements.contains(attempt) || failedHostedAttempts.contains(attempt)) return;
        CompoundTag previous = hostedSettlements.putIfAbsent(attempt, snapshot.copy());
        if (previous == null) setDirty();
        else if (!previous.equals(snapshot)) throw new IllegalStateException("Hosted settlement is immutable: " + attempt);
    }

    public void putHostedAttempt(java.util.UUID attempt, CompoundTag snapshot) {
        if (completedHostedSettlements.contains(attempt) || failedHostedAttempts.contains(attempt)
                || hostedSettlements.containsKey(attempt)) return;
        CompoundTag previous = hostedAttempts.get(attempt);
        if (previous != null && previous.hasUUID("Boss") && snapshot.hasUUID("Boss")
                && !previous.getUUID("Boss").equals(snapshot.getUUID("Boss")))
            throw new IllegalStateException("Hosted attempt changed its boss identity");
        hostedAttempts.put(attempt, snapshot.copy());
        setDirty();
    }

    public Map<java.util.UUID, CompoundTag> hostedAttempts() {
        Map<java.util.UUID, CompoundTag> snapshots = new LinkedHashMap<>();
        hostedAttempts.forEach((attempt, snapshot) -> snapshots.put(attempt, snapshot.copy()));
        return java.util.Collections.unmodifiableMap(snapshots);
    }

    public void removeHostedAttempt(java.util.UUID attempt) {
        if (hostedAttempts.remove(attempt) != null) setDirty();
    }

    public boolean hostedAttemptFailed(java.util.UUID attempt) { return failedHostedAttempts.contains(attempt); }

    public void failHostedAttempt(java.util.UUID attempt) {
        if (hostedSettlements.containsKey(attempt) || completedHostedSettlements.contains(attempt))
            throw new IllegalStateException("Victory settlement cannot become a failed attempt");
        boolean changed = failedHostedAttempts.add(attempt);
        changed |= hostedAttempts.remove(attempt) != null;
        if (changed) setDirty();
    }

    public void restoreHostedAttempt(java.util.UUID attempt, CompoundTag snapshot) {
        if (completedHostedSettlements.contains(attempt) || hostedSettlements.containsKey(attempt))
            throw new IllegalStateException("Terminal victory cannot be reactivated");
        failedHostedAttempts.remove(attempt);
        hostedAttempts.put(attempt, snapshot.copy());
        setDirty();
    }

    public Map<java.util.UUID, CompoundTag> hostedSettlements() {
        Map<java.util.UUID, CompoundTag> snapshots = new LinkedHashMap<>();
        hostedSettlements.forEach((attempt, snapshot) -> snapshots.put(attempt, snapshot.copy()));
        return java.util.Collections.unmodifiableMap(snapshots);
    }

    public boolean hostedSettlementCompleted(java.util.UUID attempt) {
        return completedHostedSettlements.contains(attempt);
    }

    public boolean hostedSettlementPending(String arenaId) {
        String canonical = canonicalArenaId(arenaId);
        return hostedSettlements.values().stream().anyMatch(snapshot -> canonical.equals(canonicalArenaId(
                snapshot.getCompound("State").getCompound("NativeEncounter").getString("ArenaId"))));
    }

    public void removeHostedSettlement(java.util.UUID attempt) {
        if (hostedSettlements.remove(attempt) != null) setDirty();
    }

    public void completeHostedSettlement(java.util.UUID attempt) {
        if (failedHostedAttempts.contains(attempt)) throw new IllegalStateException("A failed attempt cannot settle victory");
        boolean changed = completedHostedSettlements.add(attempt);
        changed |= hostedSettlements.remove(attempt) != null;
        changed |= hostedAttempts.remove(attempt) != null;
        if (changed) setDirty();
    }

    /** Only the failed durable-completion transaction may undo its in-memory marker. */
    public void restoreHostedSettlement(java.util.UUID attempt, CompoundTag snapshot) {
        completedHostedSettlements.remove(attempt);
        hostedSettlements.put(attempt, snapshot.copy());
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.put("NonExclusiveArenaHistory", nonExclusiveArenaHistory.copy());
        tag.putInt(SCHEMA_VERSION, CURRENT_SCHEMA_VERSION);
        if (!progressionThresholds.isEmpty()) {
            ListTag roster = new ListTag();
            progressionRoster.forEach(id -> roster.add(StringTag.valueOf(id.toString())));
            tag.put("ProgressionRoster", roster);
            tag.putIntArray("ProgressionThresholds", progressionThresholds);
        }
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
            if (state.deathId() != null) value.putUUID("DeathId", state.deathId());
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
            ListTag attempts = new ListTag();
            raidParticipationAttempts.getOrDefault(arenaId, Map.of()).forEach((player, attempt) -> {
                CompoundTag receipt = new CompoundTag(); receipt.putUUID("Player", player); receipt.putUUID("Attempt", attempt); attempts.add(receipt);
            });
            value.put("Attempts", attempts);
            participation.add(value);
        });
        tag.put(RAID_PARTICIPATION, participation);
        ListTag replacements = new ListTag();
        pendingRaidReplacements.forEach(id -> replacements.add(StringTag.valueOf(id)));
        tag.put(RAID_REPLACEMENTS, replacements);
        CompoundTag retirements = new CompoundTag();
        raidRetirements.forEach((arena, intent) -> retirements.put(arena, intent.copy()));
        tag.put("RaidRetirements", retirements);
        CompoundTag creations = new CompoundTag();
        pendingBossCreations.forEach((arena, entity) -> creations.put(arena, entity.copy()));
        tag.put("PendingBossCreations", creations);
        ListTag retired = new ListTag();
        retiredBossIdentities.forEach((boss, arena) -> {
            CompoundTag row = new CompoundTag(); row.putUUID("Boss", boss); row.putString("Arena", arena); retired.add(row);
        });
        tag.put("RetiredBossIdentities", retired);
        ListTag claims = new ListTag();
        liveBosses.forEach((arena, uuid) -> { CompoundTag claim = new CompoundTag(); claim.putString("Arena", arena); claim.putUUID("Boss", uuid); claims.add(claim); });
        tag.put("LiveBossIdentities", claims);
        ListTag conflicts = new ListTag();
        conflictingBosses.forEach((arena, ids) -> ids.forEach(uuid -> { CompoundTag claim = new CompoundTag(); claim.putString("Arena", arena); claim.putUUID("Boss", uuid); conflicts.add(claim); }));
        tag.put("ConflictingBossIdentities", conflicts);
        ListTag aliases = new ListTag();
        arenaAliases.forEach((from, to) -> { CompoundTag alias = new CompoundTag(); alias.putString("From", from); alias.putString("To", to); aliases.add(alias); });
        tag.put("ArenaAliases", aliases);
        ListTag settlements = new ListTag();
        hostedSettlements.forEach((attempt, snapshot) -> {
            CompoundTag row = new CompoundTag();
            row.putUUID("Attempt", attempt);
            row.put("Snapshot", snapshot.copy());
            settlements.add(row);
        });
        tag.put("PendingHostedSettlements", settlements);
        ListTag activeAttempts = new ListTag();
        hostedAttempts.forEach((attempt, snapshot) -> {
            CompoundTag row = new CompoundTag();
            row.putUUID("Attempt", attempt);
            row.put("Snapshot", snapshot.copy());
            activeAttempts.add(row);
        });
        tag.put("HostedAttempts", activeAttempts);
        ListTag completed = new ListTag();
        completedHostedSettlements.forEach(attempt -> completed.add(StringTag.valueOf(attempt.toString())));
        tag.put("CompletedHostedSettlements", completed);
        ListTag canonicalClosures = new ListTag();
        canonicalHostedClosures.forEach(attempt -> canonicalClosures.add(StringTag.valueOf(attempt.toString())));
        tag.put("CanonicalHostedClosures", canonicalClosures);
        ListTag failedAttempts = new ListTag();
        failedHostedAttempts.forEach(attempt -> failedAttempts.add(StringTag.valueOf(attempt.toString())));
        tag.put("FailedHostedAttempts", failedAttempts);
        return tag;
    }

    static WorldTierData load(CompoundTag tag) {
        WorldTierData data = new WorldTierData();
        if (tag.getInt(SCHEMA_VERSION) != CURRENT_SCHEMA_VERSION) {
            return data;
        }
        data.nonExclusiveArenaHistory = tag.getCompound("NonExclusiveArenaHistory").copy();
        data.legacyProfile = !tag.contains("ProgressionThresholds", Tag.TAG_INT_ARRAY);
        if (!data.legacyProfile) {
            data.progressionThresholds = java.util.Arrays.stream(tag.getIntArray("ProgressionThresholds")).boxed().toList();
            for (Tag value : tag.getList("ProgressionRoster", Tag.TAG_STRING)) {
                ResourceLocation id = ResourceLocation.tryParse(value.getAsString());
                if (id != null) data.progressionRoster.add(id);
            }
            ProgressionRules.tierForCount(0, data.progressionThresholds);
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
                data.respawns.put(state.getString("Arena"), new RespawnState(entity, state.getLong("Due"),
                        state.hasUUID("DeathId") ? state.getUUID("DeathId") : null));
        }
        for (Tag value : tag.getList(VERIFIED_ANCHORS, Tag.TAG_STRING)) data.verifiedAnchors.add(value.getAsString());
        for (Tag value : tag.getList(RAID_PARTICIPATION, Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) value;
            Set<java.util.UUID> players = new LinkedHashSet<>();
            for (Tag id : entry.getList("Players", Tag.TAG_STRING)) try { players.add(java.util.UUID.fromString(id.getAsString())); } catch (IllegalArgumentException ignored) {}
            if (!entry.getString("Arena").isBlank()) data.raidParticipation.put(entry.getString("Arena"), players);
            for (Tag receiptTag : entry.getList("Attempts", Tag.TAG_COMPOUND)) {
                CompoundTag receipt = (CompoundTag) receiptTag;
                if (receipt.hasUUID("Player") && receipt.hasUUID("Attempt") && players.contains(receipt.getUUID("Player")))
                    data.raidParticipationAttempts.computeIfAbsent(entry.getString("Arena"), ignored -> new LinkedHashMap<>())
                            .put(receipt.getUUID("Player"), receipt.getUUID("Attempt"));
            }
        }
        for (Tag value : tag.getList(RAID_REPLACEMENTS, Tag.TAG_STRING)) data.pendingRaidReplacements.add(value.getAsString());
        CompoundTag retirements = tag.getCompound("RaidRetirements");
        for (String arena : retirements.getAllKeys()) {
            CompoundTag intent = retirements.getCompound(arena);
            if (!intent.hasUUID("Boss")) throw new IllegalStateException("Malformed Raid retirement identity");
            data.raidRetirements.put(arena, intent.copy());
            data.pendingRaidReplacements.add(arena);
        }
        CompoundTag creations = tag.getCompound("PendingBossCreations");
        for (String arena : creations.getAllKeys()) {
            CompoundTag entity = creations.getCompound(arena);
            if (!entity.hasUUID("UUID") || !entity.contains("id", Tag.TAG_STRING))
                throw new IllegalStateException("Malformed pending boss creation");
            data.pendingBossCreations.put(arena, entity.copy());
        }
        for (Tag value : tag.getList("RetiredBossIdentities", Tag.TAG_COMPOUND)) {
            CompoundTag row = (CompoundTag) value;
            if (!row.hasUUID("Boss") || row.getString("Arena").isBlank())
                throw new IllegalStateException("Malformed retired boss identity");
            data.retiredBossIdentities.put(row.getUUID("Boss"), row.getString("Arena"));
        }
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
        for (Tag value : tag.getList("PendingHostedSettlements", Tag.TAG_COMPOUND)) {
            CompoundTag row = (CompoundTag) value;
            if (!row.hasUUID("Attempt") || !row.contains("Snapshot", Tag.TAG_COMPOUND))
                throw new IllegalStateException("Malformed pending hosted settlement");
            data.hostedSettlements.put(row.getUUID("Attempt"), row.getCompound("Snapshot").copy());
        }
        for (Tag value : tag.getList("HostedAttempts", Tag.TAG_COMPOUND)) {
            CompoundTag row = (CompoundTag) value;
            if (!row.hasUUID("Attempt") || !row.contains("Snapshot", Tag.TAG_COMPOUND))
                throw new IllegalStateException("Malformed hosted attempt");
            data.hostedAttempts.put(row.getUUID("Attempt"), row.getCompound("Snapshot").copy());
        }
        for (Tag value : tag.getList("CompletedHostedSettlements", Tag.TAG_STRING))
            data.completedHostedSettlements.add(java.util.UUID.fromString(value.getAsString()));
        data.completedHostedSettlements.forEach(data.hostedSettlements::remove);
        data.completedHostedSettlements.forEach(data.hostedAttempts::remove);
        for (String former : List.copyOf(data.pendingBossCreations.keySet())) {
            String canonical = data.canonicalArenaId(former);
            if (former.equals(canonical)) continue;
            CompoundTag intent = data.pendingBossCreations.remove(former);
            CompoundTag conflict = data.pendingBossCreations.putIfAbsent(canonical, intent);
            if (conflict != null && !conflict.getUUID("UUID").equals(intent.getUUID("UUID")))
                throw new IllegalStateException("Conflicting aliased boss creation identity");
        }
        for (String former : List.copyOf(data.raidRetirements.keySet())) {
            String canonical = data.canonicalArenaId(former);
            if (former.equals(canonical)) continue;
            CompoundTag intent = data.raidRetirements.remove(former);
            CompoundTag conflict = data.raidRetirements.putIfAbsent(canonical, intent);
            if (conflict != null && !conflict.getUUID("Boss").equals(intent.getUUID("Boss")))
                throw new IllegalStateException("Conflicting aliased Raid retirement identity");
        }
        for (Tag value : tag.getList("CanonicalHostedClosures", Tag.TAG_STRING))
            data.canonicalHostedClosures.add(java.util.UUID.fromString(value.getAsString()));
        for (Tag value : tag.getList("FailedHostedAttempts", Tag.TAG_STRING))
            data.failedHostedAttempts.add(java.util.UUID.fromString(value.getAsString()));
        for (java.util.UUID failed : data.failedHostedAttempts) {
            if (data.hostedSettlements.containsKey(failed) || data.completedHostedSettlements.contains(failed))
                throw new IllegalStateException("Conflicting persisted hosted attempt outcomes");
            data.hostedAttempts.remove(failed);
        }
        return data;
    }

    public record RespawnState(ResourceLocation entityType, long dueTime, java.util.UUID deathId) {
        public RespawnState(ResourceLocation entityType, long dueTime) { this(entityType, dueTime, null); }
    }
}
