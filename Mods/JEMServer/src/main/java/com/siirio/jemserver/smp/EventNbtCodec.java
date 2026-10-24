package com.siirio.jemserver.smp;
import java.util.*;
import java.util.function.*;
import net.minecraft.nbt.*;
/** The only event save/network codec. Original tags retain unknown fields and untouched numeric representations. */
final class EventNbtCodec {
    static final class Source {
        private final CompoundTag original;
        Source() { original = new CompoundTag(); }
        Source(CompoundTag original) { this.original = original.copy(); }
        Source copy() { return new Source(original); }
    }
    static EventState decodeEventState(CompoundTag tag) {
        var value = new EventState();
        value.pendingOutcome(tag.getString("pendingOutcome"));
        if (tag.contains("scores", Tag.TAG_COMPOUND)) value.scores(map(tag.getCompound("scores"), UUID::fromString, item -> decodeFishingScore((CompoundTag)item)));
        value.scoresChanged = false;
        if (tag.contains("description")) value.description(tag.getString("description"));
        value.descriptionChanged = false;
        if (tag.contains("nextBloodSpawn")) value.nextBloodSpawn(tag.getLong("nextBloodSpawn"));
        value.nextBloodSpawnChanged = false;
        if (tag.contains("structureBoss")) value.structureBoss(tag.getString("structureBoss"));
        value.structureBossChanged = false;
        value.source = new Source(tag);
        if (tag.hasUUID("id")) value.id(tag.getUUID("id"));
        value.idChanged = false;
        if (tag.hasUUID("owner")) value.owner(tag.getUUID("owner"));
        value.ownerChanged = false;
        if (tag.hasUUID("attemptId")) value.attemptId(tag.getUUID("attemptId"));
        value.attemptIdChanged = false;
        if (tag.hasUUID("bossEntity")) value.bossEntity(tag.getUUID("bossEntity"));
        value.bossEntityChanged = false;
        if (tag.hasUUID("encounterHost")) value.encounterHost(tag.getUUID("encounterHost"));
        value.encounterHostChanged = false;
        if (tag.hasUUID("party")) value.party(tag.getUUID("party"));
        value.partyChanged = false;
        if (tag.hasUUID("structureId")) value.structureId(tag.getUUID("structureId"));
        value.structureIdChanged = false;
        if (tag.hasUUID("targetBoss")) value.targetBoss(tag.getUUID("targetBoss"));
        value.targetBossChanged = false;
        if (tag.contains("activity")) value.activity(tag.getString("activity"));
        value.activityChanged = false;
        if (tag.contains("arenaId")) value.arenaId(tag.getString("arenaId"));
        value.arenaIdChanged = false;
        if (tag.contains("bossType")) value.bossType(tag.getString("bossType"));
        value.bossTypeChanged = false;
        if (tag.contains("cookingDifficulty")) value.cookingDifficulty(tag.getString("cookingDifficulty"));
        value.cookingDifficultyChanged = false;
        if (tag.contains("cookingReward")) value.cookingReward(tag.getString("cookingReward"));
        value.cookingRewardChanged = false;
        if (tag.contains("dimension")) value.dimension(tag.getString("dimension"));
        value.dimensionChanged = false;
        if (tag.contains("encounterState")) value.encounterState(tag.getString("encounterState"));
        value.encounterStateChanged = false;
        if (tag.contains("name")) value.name(tag.getString("name"));
        value.nameChanged = false;
        if (tag.contains("raidArenaId")) value.raidArenaId(tag.getString("raidArenaId"));
        value.raidArenaIdChanged = false;
        if (tag.contains("slot")) value.slot(tag.getString("slot"));
        value.slotChanged = false;
        if (tag.contains("state")) value.state(tag.getString("state"));
        value.stateChanged = false;
        if (tag.contains("title")) value.title(tag.getString("title"));
        value.titleChanged = false;
        if (tag.contains("wave3SetPiece")) value.wave3SetPiece(tag.getString("wave3SetPiece"));
        value.wave3SetPieceChanged = false;
        if (tag.contains("wave5SetPiece")) value.wave5SetPiece(tag.getString("wave5SetPiece"));
        value.wave5SetPieceChanged = false;
        if (tag.contains("revision")) value.revision(tag.getInt("revision"));
        value.revisionChanged = false;
        if (tag.contains("batchNumber")) value.batchNumber(tag.getInt("batchNumber"));
        value.batchNumberChanged = false;
        if (tag.contains("batchSpawned")) value.batchSpawned(tag.getInt("batchSpawned"));
        value.batchSpawnedChanged = false;
        if (tag.contains("batchTarget")) value.batchTarget(tag.getInt("batchTarget"));
        value.batchTargetChanged = false;
        if (tag.contains("continuedWave")) value.continuedWave(tag.getInt("continuedWave"));
        value.continuedWaveChanged = false;
        if (tag.contains("fullEquipped")) value.fullEquipped(tag.getInt("fullEquipped"));
        value.fullEquippedChanged = false;
        if (tag.contains("maxX")) value.maxX(tag.getInt("maxX"));
        value.maxXChanged = false;
        if (tag.contains("maxY")) value.maxY(tag.getInt("maxY"));
        value.maxYChanged = false;
        if (tag.contains("maxZ")) value.maxZ(tag.getInt("maxZ"));
        value.maxZChanged = false;
        if (tag.contains("minX")) value.minX(tag.getInt("minX"));
        value.minXChanged = false;
        if (tag.contains("minY")) value.minY(tag.getInt("minY"));
        value.minYChanged = false;
        if (tag.contains("minZ")) value.minZ(tag.getInt("minZ"));
        value.minZChanged = false;
        if (tag.contains("radius")) value.radius(tag.getInt("radius"));
        value.radiusChanged = false;
        if (tag.contains("rewardedWave")) value.rewardedWave(tag.getInt("rewardedWave"));
        value.rewardedWaveChanged = false;
        if (tag.contains("specialEquipped")) value.specialEquipped(tag.getInt("specialEquipped"));
        value.specialEquippedChanged = false;
        if (tag.contains("wave")) value.wave(tag.getInt("wave"));
        value.waveChanged = false;
        if (tag.contains("waveKilled")) value.waveKilled(tag.getInt("waveKilled"));
        value.waveKilledChanged = false;
        if (tag.contains("waveSpawned")) value.waveSpawned(tag.getInt("waveSpawned"));
        value.waveSpawnedChanged = false;
        if (tag.contains("waveTotal")) value.waveTotal(tag.getInt("waveTotal"));
        value.waveTotalChanged = false;
        if (tag.contains("worldTier")) value.worldTier(tag.getInt("worldTier"));
        value.worldTierChanged = false;
        if (tag.contains("created")) value.created(tag.getLong("created"));
        value.createdChanged = false;
        if (tag.contains("arenaRespawnPoint")) value.arenaRespawnPoint(tag.getLong("arenaRespawnPoint"));
        value.arenaRespawnPointChanged = false;
        if (tag.contains("encounterIntroEnds")) value.encounterIntroEnds(tag.getLong("encounterIntroEnds"));
        value.encounterIntroEndsChanged = false;
        if (tag.contains("encounterStateSince")) value.encounterStateSince(tag.getLong("encounterStateSince"));
        value.encounterStateSinceChanged = false;
        if (tag.contains("ends")) value.ends(tag.getLong("ends"));
        value.endsChanged = false;
        if (tag.contains("nextRegionAttempt")) value.nextRegionAttempt(tag.getLong("nextRegionAttempt"));
        value.nextRegionAttemptChanged = false;
        if (tag.contains("nextWave")) value.nextWave(tag.getLong("nextWave"));
        value.nextWaveChanged = false;
        if (tag.contains("planned")) value.planned(tag.getLong("planned"));
        value.plannedChanged = false;
        if (tag.contains("position")) value.position(tag.getLong("position"));
        value.positionChanged = false;
        if (tag.contains("preparationQueued")) value.preparationQueued(tag.getLong("preparationQueued"));
        value.preparationQueuedChanged = false;
        if (tag.contains("requestedPosition")) value.requestedPosition(tag.getLong("requestedPosition"));
        value.requestedPositionChanged = false;
        if (tag.contains("started")) value.started(tag.getLong("started"));
        value.startedChanged = false;
        if (tag.contains("waveStartedTick")) value.waveStartedTick(tag.getLong("waveStartedTick"));
        value.waveStartedTickChanged = false;
        if (tag.contains("catchup")) value.catchup(tag.getBoolean("catchup"));
        value.catchupChanged = false;
        if (tag.contains("combatStarted")) value.combatStarted(tag.getBoolean("combatStarted"));
        value.combatStartedChanged = false;
        if (tag.contains("encounterWalkIn")) value.encounterWalkIn(tag.getBoolean("encounterWalkIn"));
        value.encounterWalkInChanged = false;
        if (tag.contains("nativePending")) value.nativePending(tag.getBoolean("nativePending"));
        value.nativePendingChanged = false;
        if (tag.contains("ordinaryRaid")) value.ordinaryRaid(tag.getBoolean("ordinaryRaid"));
        value.ordinaryRaidChanged = false;
        if (tag.contains("preparing")) value.preparing(tag.getBoolean("preparing"));
        value.preparingChanged = false;
        if (tag.contains("raid")) value.raid(tag.getBoolean("raid"));
        value.raidChanged = false;
        if (tag.contains("rushTrackingFull")) value.rushTrackingFull(tag.getBoolean("rushTrackingFull"));
        value.rushTrackingFullChanged = false;
        if (tag.contains("singleBatchWaveThree")) value.singleBatchWaveThree(tag.getBoolean("singleBatchWaveThree"));
        value.singleBatchWaveThreeChanged = false;
        if (tag.contains("solo")) value.solo(tag.getBoolean("solo"));
        value.soloChanged = false;
        if (tag.contains("observedPartyDps")) value.observedPartyDps(tag.getDouble("observedPartyDps"));
        value.observedPartyDpsChanged = false;
        if (tag.contains("waveDamageStart")) value.waveDamageStart(tag.getDouble("waveDamageStart"));
        value.waveDamageStartChanged = false;
        if (tag.contains("wave3SetPieceSpawned")) value.wave3SetPieceSpawned(tag.getBoolean("wave3SetPieceSpawned"));
        value.wave3SetPieceSpawnedChanged = false;
        if (tag.contains("wave5SetPieceSpawned")) value.wave5SetPieceSpawned(tag.getBoolean("wave5SetPieceSpawned"));
        value.wave5SetPieceSpawnedChanged = false;
        if (tag.contains("members", Tag.TAG_COMPOUND)) value.members(map(tag.getCompound("members"), UUID::fromString, item -> decodeEventMember((CompoundTag)item)));
        value.membersChanged = false;
        if (true) value.setPiece(decodeEventSetPieceState(tag));
        value.setPieceChanged = false;
        if (tag.contains("bloodMoonWeather", Tag.TAG_COMPOUND)) value.bloodMoonWeather(decodeEventWeather(tag.getCompound("bloodMoonWeather")));
        value.bloodMoonWeatherChanged = false;
        if (tag.contains("voting", Tag.TAG_COMPOUND)) value.voting(decodeEventVote(tag.getCompound("voting")));
        value.votingChanged = false;
        if (tag.contains("mobs", Tag.TAG_LIST)) value.mobs(stringList(tag.get("mobs"), UUID::fromString));
        value.mobsChanged = false;
        if (tag.contains("encounterParticipants", Tag.TAG_LIST)) value.encounterParticipants(stringList(tag.get("encounterParticipants"), UUID::fromString));
        value.encounterParticipantsChanged = false;
        if (tag.contains("invalidatedLoot", Tag.TAG_LIST)) value.invalidatedLoot(uuidArrayList(tag.getList("invalidatedLoot", Tag.TAG_INT_ARRAY)));
        value.invalidatedLootChanged = false;
        if (tag.contains("raidBosses", Tag.TAG_LIST)) value.raidBosses(stringList(tag.get("raidBosses"), UUID::fromString));
        value.raidBossesChanged = false;
        if (tag.contains("remaining", Tag.TAG_COMPOUND)) value.remaining(map(tag.getCompound("remaining"), Function.identity(), item -> ((NumericTag)item).getAsInt()));
        value.remainingChanged = false;
        if (tag.contains("spawnFailures", Tag.TAG_COMPOUND)) value.spawnFailures(map(tag.getCompound("spawnFailures"), Function.identity(), item -> ((NumericTag)item).getAsInt()));
        value.spawnFailuresChanged = false;
        if (tag.contains("eliteTypes", Tag.TAG_COMPOUND)) value.eliteTypes(map(tag.getCompound("eliteTypes"), Function.identity(), item -> ((NumericTag)item).getAsByte() != 0));
        value.eliteTypesChanged = false;
        if (tag.contains("placements", Tag.TAG_COMPOUND)) value.placements(longKeys(tag.getCompound("placements")));
        value.placementsChanged = false;
        if (tag.contains("rushParticipants", Tag.TAG_COMPOUND)) value.rushParticipants(uuidKeys(tag.getCompound("rushParticipants")));
        value.rushParticipantsChanged = false;
        if (tag.contains("cookingRequirements", Tag.TAG_COMPOUND)) value.cookingRequirements(map(tag.getCompound("cookingRequirements"), Function.identity(), item -> ((NumericTag)item).getAsInt()));
        value.cookingRequirementsChanged = false;
        if (tag.contains("cookingScores", Tag.TAG_COMPOUND)) value.cookingScores(map(tag.getCompound("cookingScores"), UUID::fromString, item -> decodeEventCookingProgress((CompoundTag)item)));
        value.cookingScoresChanged = false;
        if (tag.contains("pendingWaveRewards", Tag.TAG_COMPOUND)) value.pendingWaveRewards(map(tag.getCompound("pendingWaveRewards"), UUID::fromString, item -> compoundList((ListTag)item, EventNbtCodec::decodeEventRewardGroup)));
        value.pendingWaveRewardsChanged = false;
        if (tag.contains("bloodMoonCover", Tag.TAG_COMPOUND)) value.bloodMoonCover(map(tag.getCompound("bloodMoonCover"), Long::valueOf, item -> ((NumericTag)item).getAsByte() != 0));
        value.bloodMoonCoverChanged = false;
        if (tag.contains("structureBounds", Tag.TAG_INT_ARRAY)) value.structureBounds(bounds(tag.getIntArray("structureBounds")));
        value.structureBoundsChanged = false;
        return value;
    }
    static CompoundTag encode(EventState value) {
        var tag = value.source.original.copy();
        if (value.pendingOutcome().isEmpty()) tag.remove("pendingOutcome"); else tag.putString("pendingOutcome", value.pendingOutcome());
        // Retired Raid wave counters are not revived by legacy saves. Native summons remain native.
        tag.remove("raidAlliesSpawned");
        tag.remove("raidReinforcementStage");
        tag.remove("raidBossInitialHealth");
        tag.remove("raidAllies");
        if (value.scoresChanged || value.hasScores()) { if (value.hasScores()) tag.put("scores", map(value.scores(), EventNbtCodec::encode, value.source.original.getCompound("scores"))); else tag.remove("scores"); }
        if (value.descriptionChanged) { if (value.hasDescription()) tag.putString("description",value.description()); else tag.remove("description"); }
        if (value.nextBloodSpawnChanged) { if (value.hasNextBloodSpawn()) tag.putLong("nextBloodSpawn",value.nextBloodSpawn()); else tag.remove("nextBloodSpawn"); }
        if (value.structureBossChanged) { if (value.hasStructureBoss()) tag.putString("structureBoss", value.structureBoss()); else tag.remove("structureBoss"); }
        if (value.idChanged) { if (value.hasId()) tag.putUUID("id", value.id()); else tag.remove("id"); }
        if (value.ownerChanged) { if (value.hasOwner()) tag.putUUID("owner", value.owner()); else tag.remove("owner"); }
        if (value.attemptIdChanged) { if (value.hasAttemptId()) tag.putUUID("attemptId", value.attemptId()); else tag.remove("attemptId"); }
        if (value.bossEntityChanged) { if (value.hasBossEntity()) tag.putUUID("bossEntity", value.bossEntity()); else tag.remove("bossEntity"); }
        if (value.encounterHostChanged) { if (value.hasEncounterHost()) tag.putUUID("encounterHost", value.encounterHost()); else tag.remove("encounterHost"); }
        if (value.partyChanged) { if (value.hasParty()) tag.putUUID("party", value.party()); else tag.remove("party"); }
        if (value.structureIdChanged) { if (value.hasStructureId()) tag.putUUID("structureId", value.structureId()); else tag.remove("structureId"); }
        if (value.targetBossChanged) { if (value.hasTargetBoss()) tag.putUUID("targetBoss", value.targetBoss()); else tag.remove("targetBoss"); }
        if (value.activityChanged) { if (value.hasActivity()) tag.putString("activity", value.activity()); else tag.remove("activity"); }
        if (value.arenaIdChanged) { if (value.hasArenaId()) tag.putString("arenaId", value.arenaId()); else tag.remove("arenaId"); }
        if (value.bossTypeChanged) { if (value.hasBossType()) tag.putString("bossType", value.bossType()); else tag.remove("bossType"); }
        if (value.cookingDifficultyChanged) { if (value.hasCookingDifficulty()) tag.putString("cookingDifficulty", value.cookingDifficulty()); else tag.remove("cookingDifficulty"); }
        if (value.cookingRewardChanged) { if (value.hasCookingReward()) tag.putString("cookingReward", value.cookingReward()); else tag.remove("cookingReward"); }
        if (value.dimensionChanged) { if (value.hasDimension()) tag.putString("dimension", value.dimension()); else tag.remove("dimension"); }
        if (value.encounterStateChanged) { if (value.hasEncounterState()) tag.putString("encounterState", value.encounterState()); else tag.remove("encounterState"); }
        if (value.nameChanged) { if (value.hasName()) tag.putString("name", value.name()); else tag.remove("name"); }
        if (value.raidArenaIdChanged) { if (value.hasRaidArenaId()) tag.putString("raidArenaId", value.raidArenaId()); else tag.remove("raidArenaId"); }
        if (value.slotChanged) { if (value.hasSlot()) tag.putString("slot", value.slot()); else tag.remove("slot"); }
        if (value.stateChanged) { if (value.hasState()) tag.putString("state", value.state()); else tag.remove("state"); }
        if (value.titleChanged) { if (value.hasTitle()) tag.putString("title", value.title()); else tag.remove("title"); }
        if (value.wave3SetPieceChanged) { if (value.hasWave3SetPiece()) tag.putString("wave3SetPiece", value.wave3SetPiece()); else tag.remove("wave3SetPiece"); }
        if (value.wave5SetPieceChanged) { if (value.hasWave5SetPiece()) tag.putString("wave5SetPiece", value.wave5SetPiece()); else tag.remove("wave5SetPiece"); }
        if (value.revisionChanged) { if (value.hasRevision()) tag.putInt("revision", value.revision()); else tag.remove("revision"); }
        if (value.batchNumberChanged) { if (value.hasBatchNumber()) tag.putInt("batchNumber", value.batchNumber()); else tag.remove("batchNumber"); }
        if (value.batchSpawnedChanged) { if (value.hasBatchSpawned()) tag.putInt("batchSpawned", value.batchSpawned()); else tag.remove("batchSpawned"); }
        if (value.batchTargetChanged) { if (value.hasBatchTarget()) tag.putInt("batchTarget", value.batchTarget()); else tag.remove("batchTarget"); }
        if (value.continuedWaveChanged) { if (value.hasContinuedWave()) tag.putInt("continuedWave", value.continuedWave()); else tag.remove("continuedWave"); }
        if (value.fullEquippedChanged) { if (value.hasFullEquipped()) tag.putInt("fullEquipped", value.fullEquipped()); else tag.remove("fullEquipped"); }
        if (value.maxXChanged) { if (value.hasMaxX()) tag.putInt("maxX", value.maxX()); else tag.remove("maxX"); }
        if (value.maxYChanged) { if (value.hasMaxY()) tag.putInt("maxY", value.maxY()); else tag.remove("maxY"); }
        if (value.maxZChanged) { if (value.hasMaxZ()) tag.putInt("maxZ", value.maxZ()); else tag.remove("maxZ"); }
        if (value.minXChanged) { if (value.hasMinX()) tag.putInt("minX", value.minX()); else tag.remove("minX"); }
        if (value.minYChanged) { if (value.hasMinY()) tag.putInt("minY", value.minY()); else tag.remove("minY"); }
        if (value.minZChanged) { if (value.hasMinZ()) tag.putInt("minZ", value.minZ()); else tag.remove("minZ"); }
        if (value.radiusChanged) { if (value.hasRadius()) tag.putInt("radius", value.radius()); else tag.remove("radius"); }
        if (value.rewardedWaveChanged) { if (value.hasRewardedWave()) tag.putInt("rewardedWave", value.rewardedWave()); else tag.remove("rewardedWave"); }
        if (value.specialEquippedChanged) { if (value.hasSpecialEquipped()) tag.putInt("specialEquipped", value.specialEquipped()); else tag.remove("specialEquipped"); }
        if (value.waveChanged) { if (value.hasWave()) tag.putInt("wave", value.wave()); else tag.remove("wave"); }
        if (value.waveKilledChanged) { if (value.hasWaveKilled()) tag.putInt("waveKilled", value.waveKilled()); else tag.remove("waveKilled"); }
        if (value.waveSpawnedChanged) { if (value.hasWaveSpawned()) tag.putInt("waveSpawned", value.waveSpawned()); else tag.remove("waveSpawned"); }
        if (value.waveTotalChanged) { if (value.hasWaveTotal()) tag.putInt("waveTotal", value.waveTotal()); else tag.remove("waveTotal"); }
        if (value.worldTierChanged) { if (value.hasWorldTier()) tag.putInt("worldTier", value.worldTier()); else tag.remove("worldTier"); }
        if (value.createdChanged) { if (value.hasCreated()) tag.putLong("created", value.created()); else tag.remove("created"); }
        if (value.arenaRespawnPointChanged) { if (value.hasArenaRespawnPoint()) tag.putLong("arenaRespawnPoint", value.arenaRespawnPoint()); else tag.remove("arenaRespawnPoint"); }
        if (value.encounterIntroEndsChanged) { if (value.hasEncounterIntroEnds()) tag.putLong("encounterIntroEnds", value.encounterIntroEnds()); else tag.remove("encounterIntroEnds"); }
        if (value.encounterStateSinceChanged) { if (value.hasEncounterStateSince()) tag.putLong("encounterStateSince", value.encounterStateSince()); else tag.remove("encounterStateSince"); }
        if (value.endsChanged) { if (value.hasEnds()) tag.putLong("ends", value.ends()); else tag.remove("ends"); }
        if (value.nextRegionAttemptChanged) { if (value.hasNextRegionAttempt()) tag.putLong("nextRegionAttempt", value.nextRegionAttempt()); else tag.remove("nextRegionAttempt"); }
        if (value.nextWaveChanged) { if (value.hasNextWave()) tag.putLong("nextWave", value.nextWave()); else tag.remove("nextWave"); }
        if (value.plannedChanged) { if (value.hasPlanned()) tag.putLong("planned", value.planned()); else tag.remove("planned"); }
        if (value.positionChanged) { if (value.hasPosition()) tag.putLong("position", value.position()); else tag.remove("position"); }
        if (value.preparationQueuedChanged) { if (value.hasPreparationQueued()) tag.putLong("preparationQueued", value.preparationQueued()); else tag.remove("preparationQueued"); }
        if (value.requestedPositionChanged) { if (value.hasRequestedPosition()) tag.putLong("requestedPosition", value.requestedPosition()); else tag.remove("requestedPosition"); }
        if (value.startedChanged) { if (value.hasStarted()) tag.putLong("started", value.started()); else tag.remove("started"); }
        if (value.waveStartedTickChanged) { if (value.hasWaveStartedTick()) tag.putLong("waveStartedTick", value.waveStartedTick()); else tag.remove("waveStartedTick"); }
        if (value.catchupChanged) { if (value.hasCatchup()) tag.putBoolean("catchup", value.catchup()); else tag.remove("catchup"); }
        if (value.combatStartedChanged) { if (value.hasCombatStarted()) tag.putBoolean("combatStarted", value.combatStarted()); else tag.remove("combatStarted"); }
        if (value.encounterWalkInChanged) { if (value.hasEncounterWalkIn()) tag.putBoolean("encounterWalkIn", value.encounterWalkIn()); else tag.remove("encounterWalkIn"); }
        if (value.nativePendingChanged) { if (value.hasNativePending()) tag.putBoolean("nativePending", value.nativePending()); else tag.remove("nativePending"); }
        if (value.ordinaryRaidChanged) { if (value.hasOrdinaryRaid()) tag.putBoolean("ordinaryRaid", value.ordinaryRaid()); else tag.remove("ordinaryRaid"); }
        if (value.preparingChanged) { if (value.hasPreparing()) tag.putBoolean("preparing", value.preparing()); else tag.remove("preparing"); }
        if (value.raidChanged) { if (value.hasRaid()) tag.putBoolean("raid", value.raid()); else tag.remove("raid"); }
        if (value.rushTrackingFullChanged) { if (value.hasRushTrackingFull()) tag.putBoolean("rushTrackingFull", value.rushTrackingFull()); else tag.remove("rushTrackingFull"); }
        if (value.singleBatchWaveThreeChanged) { if (value.hasSingleBatchWaveThree()) tag.putBoolean("singleBatchWaveThree", value.singleBatchWaveThree()); else tag.remove("singleBatchWaveThree"); }
        if (value.soloChanged) { if (value.hasSolo()) tag.putBoolean("solo", value.solo()); else tag.remove("solo"); }
        if (value.observedPartyDpsChanged) { if (value.hasObservedPartyDps()) tag.putDouble("observedPartyDps", value.observedPartyDps()); else tag.remove("observedPartyDps"); }
        if (value.waveDamageStartChanged) { if (value.hasWaveDamageStart()) tag.putDouble("waveDamageStart", value.waveDamageStart()); else tag.remove("waveDamageStart"); }
        if (value.wave3SetPieceSpawnedChanged) { if (value.hasWave3SetPieceSpawned()) tag.putBoolean("wave3SetPieceSpawned", value.wave3SetPieceSpawned()); else tag.remove("wave3SetPieceSpawned"); }
        if (value.wave5SetPieceSpawnedChanged) { if (value.hasWave5SetPieceSpawned()) tag.putBoolean("wave5SetPieceSpawned", value.wave5SetPieceSpawned()); else tag.remove("wave5SetPieceSpawned"); }
        if (value.membersChanged || value.hasMembers()) { if (value.hasMembers()) tag.put("members", map(value.members(), EventNbtCodec::encode, value.source.original.getCompound("members"))); else tag.remove("members"); }
        var piece = encode(value.setPiece());
        for (String key : new HashSet<>(tag.getAllKeys())) if (key.startsWith("setPiece")) tag.remove(key);
        for (String key : piece.getAllKeys()) if (key.startsWith("setPiece")) tag.put(key, piece.get(key).copy());
        if (value.bloodMoonWeatherChanged || value.hasBloodMoonWeather()) { if (value.hasBloodMoonWeather()) tag.put("bloodMoonWeather", encode(value.bloodMoonWeather())); else tag.remove("bloodMoonWeather"); }
        if (value.votingChanged || value.hasVoting()) { if (value.hasVoting()) tag.put("voting", encode(value.voting())); else tag.remove("voting"); }
        if (value.mobsChanged) { if (value.hasMobs()) tag.put("mobs", list(value.mobs(), item -> StringTag.valueOf(item.toString()))); else tag.remove("mobs"); }
        if (value.encounterParticipantsChanged) { if (value.hasEncounterParticipants()) tag.put("encounterParticipants", list(value.encounterParticipants(), item -> StringTag.valueOf(item.toString()))); else tag.remove("encounterParticipants"); }
        if (value.invalidatedLootChanged) { if (value.hasInvalidatedLoot()) tag.put("invalidatedLoot", list(value.invalidatedLoot(), NbtUtils::createUUID)); else tag.remove("invalidatedLoot"); }
        if (value.raidBossesChanged) { if (value.hasRaidBosses()) tag.put("raidBosses", list(value.raidBosses(), item -> StringTag.valueOf(item.toString()))); else tag.remove("raidBosses"); }
        if (value.remainingChanged) { if (value.hasRemaining()) tag.put("remaining", map(value.remaining(), IntTag::valueOf, value.source.original.getCompound("remaining"))); else tag.remove("remaining"); }
        if (value.spawnFailuresChanged) { if (value.hasSpawnFailures()) tag.put("spawnFailures", map(value.spawnFailures(), IntTag::valueOf, value.source.original.getCompound("spawnFailures"))); else tag.remove("spawnFailures"); }
        if (value.eliteTypesChanged) { if (value.hasEliteTypes()) tag.put("eliteTypes", map(value.eliteTypes(), ByteTag::valueOf, value.source.original.getCompound("eliteTypes"))); else tag.remove("eliteTypes"); }
        if (value.placementsChanged) { if (value.hasPlacements()) tag.put("placements", set(value.placements(), value.source.original.getCompound("placements"), false)); else tag.remove("placements"); }
        if (value.rushParticipantsChanged) { if (value.hasRushParticipants()) tag.put("rushParticipants", set(value.rushParticipants(), value.source.original.getCompound("rushParticipants"), true)); else tag.remove("rushParticipants"); }
        if (value.cookingRequirementsChanged) { if (value.hasCookingRequirements()) tag.put("cookingRequirements", map(value.cookingRequirements(), IntTag::valueOf, value.source.original.getCompound("cookingRequirements"))); else tag.remove("cookingRequirements"); }
        if (value.cookingScoresChanged || value.hasCookingScores()) { if (value.hasCookingScores()) tag.put("cookingScores", map(value.cookingScores(), EventNbtCodec::encode, value.source.original.getCompound("cookingScores"))); else tag.remove("cookingScores"); }
        if (value.pendingWaveRewardsChanged || value.hasPendingWaveRewards()) { if (value.hasPendingWaveRewards()) tag.put("pendingWaveRewards", map(value.pendingWaveRewards(), items -> list(items, EventNbtCodec::encode), value.source.original.getCompound("pendingWaveRewards"))); else tag.remove("pendingWaveRewards"); }
        if (value.bloodMoonCoverChanged) { if (value.hasBloodMoonCover()) tag.put("bloodMoonCover", map(value.bloodMoonCover(), ByteTag::valueOf, value.source.original.getCompound("bloodMoonCover"))); else tag.remove("bloodMoonCover"); }
        if (value.structureBoundsChanged) { if (value.hasStructureBounds()) tag.put("structureBounds", new IntArrayTag(new int[]{value.structureBounds().minX(),value.structureBounds().minY(),value.structureBounds().minZ(),value.structureBounds().maxX(),value.structureBounds().maxY(),value.structureBounds().maxZ()})); else tag.remove("structureBounds"); }
        return tag;
    }
    static EventSetPieceState decodeEventSetPieceState(CompoundTag tag) {
        var value = new EventSetPieceState();
        value.source = new Source(tag);
        if (tag.contains("setPieceAttack")) value.attack(tag.getString("setPieceAttack"));
        value.attackChanged = false;
        if (tag.contains("setPieceAttackPhase")) value.attackPhase(tag.getString("setPieceAttackPhase"));
        value.attackPhaseChanged = false;
        if (tag.contains("setPieceLastAttack")) value.lastAttack(tag.getString("setPieceLastAttack"));
        value.lastAttackChanged = false;
        if (tag.contains("setPiecePreviousAttack")) value.previousAttack(tag.getString("setPiecePreviousAttack"));
        value.previousAttackChanged = false;
        if (tag.contains("setPieceArenaWave")) value.arenaWave(tag.getInt("setPieceArenaWave"));
        value.arenaWaveChanged = false;
        if (tag.contains("setPieceCastParity")) value.castParity(tag.getInt("setPieceCastParity"));
        value.castParityChanged = false;
        if (tag.contains("setPieceComboIndex")) value.comboIndex(tag.getInt("setPieceComboIndex"));
        value.comboIndexChanged = false;
        if (tag.contains("setPieceCongregation")) value.congregation(tag.getInt("setPieceCongregation"));
        value.congregationChanged = false;
        if (tag.contains("setPieceDebuffProfileCursor")) value.debuffProfileCursor(tag.getInt("setPieceDebuffProfileCursor"));
        value.debuffProfileCursorChanged = false;
        if (tag.contains("setPieceFloorIndex")) value.floorIndex(tag.getInt("setPieceFloorIndex"));
        value.floorIndexChanged = false;
        if (tag.contains("setPieceSafeQuarter")) value.safeQuarter(tag.getInt("setPieceSafeQuarter"));
        value.safeQuarterChanged = false;
        if (tag.contains("setPieceTwinPattern")) value.twinPattern(tag.getInt("setPieceTwinPattern"));
        value.twinPatternChanged = false;
        if (tag.contains("setPieceAttackBlocksUntil")) value.attackBlocksUntil(tag.getLong("setPieceAttackBlocksUntil"));
        value.attackBlocksUntilChanged = false;
        if (tag.contains("setPieceAttackStart")) value.attackStart(tag.getLong("setPieceAttackStart"));
        value.attackStartChanged = false;
        if (tag.contains("setPieceComboNext")) value.comboNext(tag.getLong("setPieceComboNext"));
        value.comboNextChanged = false;
        if (tag.contains("setPieceDebuffNextCast")) value.debuffNextCast(tag.getLong("setPieceDebuffNextCast"));
        value.debuffNextCastChanged = false;
        if (tag.contains("setPieceImpact")) value.impact(tag.getLong("setPieceImpact"));
        value.impactChanged = false;
        if (tag.contains("setPiecePhaseUntil")) value.phaseUntil(tag.getLong("setPiecePhaseUntil"));
        value.phaseUntilChanged = false;
        if (tag.contains("setPieceRecoveryUntil")) value.recoveryUntil(tag.getLong("setPieceRecoveryUntil"));
        value.recoveryUntilChanged = false;
        if (tag.contains("setPieceComboActive")) value.comboActive(tag.getBoolean("setPieceComboActive"));
        value.comboActiveChanged = false;
        if (tag.contains("setPieceFinalUsed")) value.finalUsed(tag.getBoolean("setPieceFinalUsed"));
        value.finalUsedChanged = false;
        if (tag.contains("setPieceRestoring")) value.restoring(tag.getBoolean("setPieceRestoring"));
        value.restoringChanged = false;
        if (tag.contains("setPieceReverse")) value.reverse(tag.getBoolean("setPieceReverse"));
        value.reverseChanged = false;
        if (tag.contains("setPieceInitialHealth")) value.initialHealth(tag.getDouble("setPieceInitialHealth"));
        value.initialHealthChanged = false;
        if (tag.contains("setPieceOriginX")) value.originX(tag.getDouble("setPieceOriginX"));
        value.originXChanged = false;
        if (tag.contains("setPieceOriginZ")) value.originZ(tag.getDouble("setPieceOriginZ"));
        value.originZChanged = false;
        if (tag.contains("setPieceSecondX")) value.secondX(tag.getDouble("setPieceSecondX"));
        value.secondXChanged = false;
        if (tag.contains("setPieceSecondZ")) value.secondZ(tag.getDouble("setPieceSecondZ"));
        value.secondZChanged = false;
        if (tag.contains("setPieceTargetX")) value.targetX(tag.getDouble("setPieceTargetX"));
        value.targetXChanged = false;
        if (tag.contains("setPieceTargetZ")) value.targetZ(tag.getDouble("setPieceTargetZ"));
        value.targetZChanged = false;
        if (tag.contains("setPieceMobs", Tag.TAG_LIST)) value.mobs(stringList(tag.get("setPieceMobs"), UUID::fromString));
        value.mobsChanged = false;
        if (tag.contains("setPieceBosses", Tag.TAG_LIST)) value.bosses(stringList(tag.get("setPieceBosses"), UUID::fromString));
        value.bossesChanged = false;
        if (tag.contains("setPieceCooldowns", Tag.TAG_COMPOUND)) value.cooldowns(map(tag.getCompound("setPieceCooldowns"), Function.identity(), item -> ((NumericTag)item).getAsLong()));
        value.cooldownsChanged = false;
        if (tag.contains("setPieceTerrain", Tag.TAG_COMPOUND)) value.terrain(map(tag.getCompound("setPieceTerrain"), Long::valueOf, item -> decodeEventTerrainChange((CompoundTag)item)));
        value.terrainChanged = false;
        if (tag.contains("setPieceAttackTerrain", Tag.TAG_COMPOUND)) value.attackTerrain(map(tag.getCompound("setPieceAttackTerrain"), Long::valueOf, item -> decodeEventTerrainChange((CompoundTag)item)));
        value.attackTerrainChanged = false;
        if (tag.contains("setPieceAttackBlocks", Tag.TAG_LIST)) value.attackBlocks(longList(tag.getList("setPieceAttackBlocks", Tag.TAG_LONG)));
        value.attackBlocksChanged = false;
        if (tag.contains("setPieceFloorQueue", Tag.TAG_LIST)) value.floorQueue(longList(tag.getList("setPieceFloorQueue", Tag.TAG_LONG)));
        value.floorQueueChanged = false;
        if (tag.contains("setPieceComboQueue", Tag.TAG_LIST)) value.comboQueue(stringList(tag.get("setPieceComboQueue"), Function.identity()));
        value.comboQueueChanged = false;
        return value;
    }
    static CompoundTag encode(EventSetPieceState value) {
        var tag = value.source.original.copy();
        if (value.attackChanged) { if (value.hasAttack()) tag.putString("setPieceAttack", value.attack()); else tag.remove("setPieceAttack"); }
        if (value.attackPhaseChanged) { if (value.hasAttackPhase()) tag.putString("setPieceAttackPhase", value.attackPhase()); else tag.remove("setPieceAttackPhase"); }
        if (value.lastAttackChanged) { if (value.hasLastAttack()) tag.putString("setPieceLastAttack", value.lastAttack()); else tag.remove("setPieceLastAttack"); }
        if (value.previousAttackChanged) { if (value.hasPreviousAttack()) tag.putString("setPiecePreviousAttack", value.previousAttack()); else tag.remove("setPiecePreviousAttack"); }
        if (value.arenaWaveChanged) { if (value.hasArenaWave()) tag.putInt("setPieceArenaWave", value.arenaWave()); else tag.remove("setPieceArenaWave"); }
        if (value.castParityChanged) { if (value.hasCastParity()) tag.putInt("setPieceCastParity", value.castParity()); else tag.remove("setPieceCastParity"); }
        if (value.comboIndexChanged) { if (value.hasComboIndex()) tag.putInt("setPieceComboIndex", value.comboIndex()); else tag.remove("setPieceComboIndex"); }
        if (value.congregationChanged) { if (value.hasCongregation()) tag.putInt("setPieceCongregation", value.congregation()); else tag.remove("setPieceCongregation"); }
        if (value.debuffProfileCursorChanged) { if (value.hasDebuffProfileCursor()) tag.putInt("setPieceDebuffProfileCursor", value.debuffProfileCursor()); else tag.remove("setPieceDebuffProfileCursor"); }
        if (value.floorIndexChanged) { if (value.hasFloorIndex()) tag.putInt("setPieceFloorIndex", value.floorIndex()); else tag.remove("setPieceFloorIndex"); }
        if (value.safeQuarterChanged) { if (value.hasSafeQuarter()) tag.putInt("setPieceSafeQuarter", value.safeQuarter()); else tag.remove("setPieceSafeQuarter"); }
        if (value.twinPatternChanged) { if (value.hasTwinPattern()) tag.putInt("setPieceTwinPattern", value.twinPattern()); else tag.remove("setPieceTwinPattern"); }
        if (value.attackBlocksUntilChanged) { if (value.hasAttackBlocksUntil()) tag.putLong("setPieceAttackBlocksUntil", value.attackBlocksUntil()); else tag.remove("setPieceAttackBlocksUntil"); }
        if (value.attackStartChanged) { if (value.hasAttackStart()) tag.putLong("setPieceAttackStart", value.attackStart()); else tag.remove("setPieceAttackStart"); }
        if (value.comboNextChanged) { if (value.hasComboNext()) tag.putLong("setPieceComboNext", value.comboNext()); else tag.remove("setPieceComboNext"); }
        if (value.debuffNextCastChanged) { if (value.hasDebuffNextCast()) tag.putLong("setPieceDebuffNextCast", value.debuffNextCast()); else tag.remove("setPieceDebuffNextCast"); }
        if (value.impactChanged) { if (value.hasImpact()) tag.putLong("setPieceImpact", value.impact()); else tag.remove("setPieceImpact"); }
        if (value.phaseUntilChanged) { if (value.hasPhaseUntil()) tag.putLong("setPiecePhaseUntil", value.phaseUntil()); else tag.remove("setPiecePhaseUntil"); }
        if (value.recoveryUntilChanged) { if (value.hasRecoveryUntil()) tag.putLong("setPieceRecoveryUntil", value.recoveryUntil()); else tag.remove("setPieceRecoveryUntil"); }
        if (value.comboActiveChanged) { if (value.hasComboActive()) tag.putBoolean("setPieceComboActive", value.comboActive()); else tag.remove("setPieceComboActive"); }
        if (value.finalUsedChanged) { if (value.hasFinalUsed()) tag.putBoolean("setPieceFinalUsed", value.finalUsed()); else tag.remove("setPieceFinalUsed"); }
        if (value.restoringChanged) { if (value.hasRestoring()) tag.putBoolean("setPieceRestoring", value.restoring()); else tag.remove("setPieceRestoring"); }
        if (value.reverseChanged) { if (value.hasReverse()) tag.putBoolean("setPieceReverse", value.reverse()); else tag.remove("setPieceReverse"); }
        if (value.initialHealthChanged) { if (value.hasInitialHealth()) tag.putDouble("setPieceInitialHealth", value.initialHealth()); else tag.remove("setPieceInitialHealth"); }
        if (value.originXChanged) { if (value.hasOriginX()) tag.putDouble("setPieceOriginX", value.originX()); else tag.remove("setPieceOriginX"); }
        if (value.originZChanged) { if (value.hasOriginZ()) tag.putDouble("setPieceOriginZ", value.originZ()); else tag.remove("setPieceOriginZ"); }
        if (value.secondXChanged) { if (value.hasSecondX()) tag.putDouble("setPieceSecondX", value.secondX()); else tag.remove("setPieceSecondX"); }
        if (value.secondZChanged) { if (value.hasSecondZ()) tag.putDouble("setPieceSecondZ", value.secondZ()); else tag.remove("setPieceSecondZ"); }
        if (value.targetXChanged) { if (value.hasTargetX()) tag.putDouble("setPieceTargetX", value.targetX()); else tag.remove("setPieceTargetX"); }
        if (value.targetZChanged) { if (value.hasTargetZ()) tag.putDouble("setPieceTargetZ", value.targetZ()); else tag.remove("setPieceTargetZ"); }
        if (value.mobsChanged) { if (value.hasMobs()) tag.put("setPieceMobs", list(value.mobs(), item -> StringTag.valueOf(item.toString()))); else tag.remove("setPieceMobs"); }
        if (value.bossesChanged) { if (value.hasBosses()) tag.put("setPieceBosses", list(value.bosses(), item -> StringTag.valueOf(item.toString()))); else tag.remove("setPieceBosses"); }
        if (value.cooldownsChanged) { if (value.hasCooldowns()) tag.put("setPieceCooldowns", map(value.cooldowns(), LongTag::valueOf, value.source.original.getCompound("setPieceCooldowns"))); else tag.remove("setPieceCooldowns"); }
        if (value.terrainChanged || value.hasTerrain()) { if (value.hasTerrain()) tag.put("setPieceTerrain", map(value.terrain(), EventNbtCodec::encode, value.source.original.getCompound("setPieceTerrain"))); else tag.remove("setPieceTerrain"); }
        if (value.attackTerrainChanged || value.hasAttackTerrain()) { if (value.hasAttackTerrain()) tag.put("setPieceAttackTerrain", map(value.attackTerrain(), EventNbtCodec::encode, value.source.original.getCompound("setPieceAttackTerrain"))); else tag.remove("setPieceAttackTerrain"); }
        if (value.attackBlocksChanged) { if (value.hasAttackBlocks()) tag.put("setPieceAttackBlocks", list(value.attackBlocks(), item -> LongTag.valueOf(item))); else tag.remove("setPieceAttackBlocks"); }
        if (value.floorQueueChanged) { if (value.hasFloorQueue()) tag.put("setPieceFloorQueue", list(value.floorQueue(), item -> LongTag.valueOf(item))); else tag.remove("setPieceFloorQueue"); }
        if (value.comboQueueChanged) { if (value.hasComboQueue()) tag.put("setPieceComboQueue", list(value.comboQueue(), item -> StringTag.valueOf(item.toString()))); else tag.remove("setPieceComboQueue"); }
        return tag;
    }
    static EventMember decodeEventMember(CompoundTag tag) {
        var value = new EventMember(); value.nativeDeath(tag.getBoolean("nativeDeath"));
        if (tag.contains("raidArenaExitPending")) value.raidArenaExitPending(tag.getBoolean("raidArenaExitPending"));
        value.raidArenaExitPendingChanged = false;
        value.source = new Source(tag);
        if (tag.contains("name")) value.name(tag.getString("name"));
        value.nameChanged = false;
        if (tag.contains("originDimension")) value.originDimension(tag.getString("originDimension"));
        value.originDimensionChanged = false;
        if (tag.contains("actualDeaths")) value.actualDeaths(tag.getInt("actualDeaths"));
        value.actualDeathsChanged = false;
        if (tag.contains("kills")) value.kills(tag.getInt("kills"));
        value.killsChanged = false;
        if (tag.contains("revives")) value.revives(tag.getInt("revives"));
        value.revivesChanged = false;
        if (tag.contains("joinedAt")) value.joinedAt(tag.getLong("joinedAt"));
        value.joinedAtChanged = false;
        if (tag.contains("encounterArrival")) value.encounterArrival(tag.getLong("encounterArrival"));
        value.encounterArrivalChanged = false;
        if (tag.contains("eventDisconnectedAt")) value.eventDisconnectedAt(tag.getLong("eventDisconnectedAt"));
        value.eventDisconnectedAtChanged = false;
        if (tag.contains("downedAt")) value.downedAt(tag.getLong("downedAt"));
        value.downedAtChanged = false;
        if (tag.contains("accepted")) value.accepted(tag.getBoolean("accepted"));
        value.acceptedChanged = false;
        if (tag.contains("ready")) value.ready(tag.getBoolean("ready"));
        value.readyChanged = false;
        if (tag.contains("eliminated")) value.eliminated(tag.getBoolean("eliminated"));
        value.eliminatedChanged = false;
        if (tag.contains("contributed")) value.contributed(tag.getBoolean("contributed"));
        value.contributedChanged = false;
        if (tag.contains("supported")) value.supported(tag.getBoolean("supported"));
        value.supportedChanged = false;
        if (tag.contains("safeRespawn")) value.safeRespawn(tag.getBoolean("safeRespawn"));
        value.safeRespawnChanged = false;
        if (tag.contains("awaitingReturn")) value.awaitingReturn(tag.getBoolean("awaitingReturn"));
        value.awaitingReturnChanged = false;
        if (tag.contains("encounterTeleported")) value.encounterTeleported(tag.getBoolean("encounterTeleported"));
        value.encounterTeleportedChanged = false;
        if (tag.contains("eventReturnPending")) value.eventReturnPending(tag.getBoolean("eventReturnPending"));
        value.eventReturnPendingChanged = false;
        if (tag.contains("eventAbsenceDefeatPending")) value.eventAbsenceDefeatPending(tag.getBoolean("eventAbsenceDefeatPending"));
        value.eventAbsenceDefeatPendingChanged = false;
        if (tag.contains("damage")) value.damage(tag.getDouble("damage"));
        value.damageChanged = false;
        if (tag.contains("safeX")) value.safeX(tag.getDouble("safeX"));
        value.safeXChanged = false;
        if (tag.contains("safeY")) value.safeY(tag.getDouble("safeY"));
        value.safeYChanged = false;
        if (tag.contains("safeZ")) value.safeZ(tag.getDouble("safeZ"));
        value.safeZChanged = false;
        if (tag.contains("originX")) value.originX(tag.getDouble("originX"));
        value.originXChanged = false;
        if (tag.contains("originY")) value.originY(tag.getDouble("originY"));
        value.originYChanged = false;
        if (tag.contains("originZ")) value.originZ(tag.getDouble("originZ"));
        value.originZChanged = false;
        if (tag.contains("originYaw")) value.originYaw(tag.getFloat("originYaw"));
        value.originYawChanged = false;
        if (tag.contains("originPitch")) value.originPitch(tag.getFloat("originPitch"));
        value.originPitchChanged = false;
        if (tag.contains("waves")) value.waves(tag.getInt("waves"));
        value.wavesChanged = false;
        if (tag.contains("safeInventory")) value.safeInventory(new NativeNbtPayload(tag.get("safeInventory")));
        value.safeInventoryChanged = false;
        if (tag.contains("safeDrops")) value.safeDrops(new NativeNbtPayload(tag.get("safeDrops")));
        value.safeDropsChanged = false;
        return value;
    }
    static CompoundTag encode(EventMember value) {
        var tag = value.source.original.copy();
        if (value.nativeDeath()) tag.putBoolean("nativeDeath", true); else tag.remove("nativeDeath");
        if (value.raidArenaExitPendingChanged) { if (value.hasRaidArenaExitPending()) tag.putBoolean("raidArenaExitPending",value.raidArenaExitPending()); else tag.remove("raidArenaExitPending"); }
        if (value.nameChanged) { if (value.hasName()) tag.putString("name", value.name()); else tag.remove("name"); }
        if (value.originDimensionChanged) { if (value.hasOriginDimension()) tag.putString("originDimension", value.originDimension()); else tag.remove("originDimension"); }
        if (value.actualDeathsChanged) { if (value.hasActualDeaths()) tag.putInt("actualDeaths", value.actualDeaths()); else tag.remove("actualDeaths"); }
        if (value.killsChanged) { if (value.hasKills()) tag.putInt("kills", value.kills()); else tag.remove("kills"); }
        if (value.revivesChanged) { if (value.hasRevives()) tag.putInt("revives", value.revives()); else tag.remove("revives"); }
        if (value.joinedAtChanged) { if (value.hasJoinedAt()) tag.putLong("joinedAt", value.joinedAt()); else tag.remove("joinedAt"); }
        if (value.encounterArrivalChanged) { if (value.hasEncounterArrival()) tag.putLong("encounterArrival", value.encounterArrival()); else tag.remove("encounterArrival"); }
        if (value.eventDisconnectedAtChanged) { if (value.hasEventDisconnectedAt()) tag.putLong("eventDisconnectedAt", value.eventDisconnectedAt()); else tag.remove("eventDisconnectedAt"); }
        if (value.downedAtChanged) { if (value.hasDownedAt()) tag.putLong("downedAt", value.downedAt()); else tag.remove("downedAt"); }
        if (value.acceptedChanged) { if (value.hasAccepted()) tag.putBoolean("accepted", value.accepted()); else tag.remove("accepted"); }
        if (value.readyChanged) { if (value.hasReady()) tag.putBoolean("ready", value.ready()); else tag.remove("ready"); }
        if (value.eliminatedChanged) { if (value.hasEliminated()) tag.putBoolean("eliminated", value.eliminated()); else tag.remove("eliminated"); }
        if (value.contributedChanged) { if (value.hasContributed()) tag.putBoolean("contributed", value.contributed()); else tag.remove("contributed"); }
        if (value.supportedChanged) { if (value.hasSupported()) tag.putBoolean("supported", value.supported()); else tag.remove("supported"); }
        if (value.safeRespawnChanged) { if (value.hasSafeRespawn()) tag.putBoolean("safeRespawn", value.safeRespawn()); else tag.remove("safeRespawn"); }
        if (value.awaitingReturnChanged) { if (value.hasAwaitingReturn()) tag.putBoolean("awaitingReturn", value.awaitingReturn()); else tag.remove("awaitingReturn"); }
        if (value.encounterTeleportedChanged) { if (value.hasEncounterTeleported()) tag.putBoolean("encounterTeleported", value.encounterTeleported()); else tag.remove("encounterTeleported"); }
        if (value.eventReturnPendingChanged) { if (value.hasEventReturnPending()) tag.putBoolean("eventReturnPending", value.eventReturnPending()); else tag.remove("eventReturnPending"); }
        if (value.eventAbsenceDefeatPendingChanged) { if (value.hasEventAbsenceDefeatPending()) tag.putBoolean("eventAbsenceDefeatPending", value.eventAbsenceDefeatPending()); else tag.remove("eventAbsenceDefeatPending"); }
        if (value.damageChanged) { if (value.hasDamage()) tag.putDouble("damage", value.damage()); else tag.remove("damage"); }
        if (value.safeXChanged) { if (value.hasSafeX()) tag.putDouble("safeX", value.safeX()); else tag.remove("safeX"); }
        if (value.safeYChanged) { if (value.hasSafeY()) tag.putDouble("safeY", value.safeY()); else tag.remove("safeY"); }
        if (value.safeZChanged) { if (value.hasSafeZ()) tag.putDouble("safeZ", value.safeZ()); else tag.remove("safeZ"); }
        if (value.originXChanged) { if (value.hasOriginX()) tag.putDouble("originX", value.originX()); else tag.remove("originX"); }
        if (value.originYChanged) { if (value.hasOriginY()) tag.putDouble("originY", value.originY()); else tag.remove("originY"); }
        if (value.originZChanged) { if (value.hasOriginZ()) tag.putDouble("originZ", value.originZ()); else tag.remove("originZ"); }
        if (value.originYawChanged) { if (value.hasOriginYaw()) tag.putFloat("originYaw", value.originYaw()); else tag.remove("originYaw"); }
        if (value.originPitchChanged) { if (value.hasOriginPitch()) tag.putFloat("originPitch", value.originPitch()); else tag.remove("originPitch"); }
        if (value.wavesChanged) { if (value.hasWaves()) tag.putInt("waves", value.waves()); else tag.remove("waves"); }
        if (value.safeInventoryChanged) { if (value.hasSafeInventory()) tag.put("safeInventory", value.safeInventory().copyNativeTag()); else tag.remove("safeInventory"); }
        if (value.safeDropsChanged) { if (value.hasSafeDrops()) tag.put("safeDrops", value.safeDrops().copyNativeTag()); else tag.remove("safeDrops"); }
        return tag;
    }
    static EventWeather decodeEventWeather(CompoundTag tag) {
        var value = new EventWeather();
        value.source = new Source(tag);
        if (tag.contains("clear")) value.clear(tag.getInt("clear"));
        value.clearChanged = false;
        if (tag.contains("rain")) value.rain(tag.getInt("rain"));
        value.rainChanged = false;
        if (tag.contains("thunder")) value.thunder(tag.getInt("thunder"));
        value.thunderChanged = false;
        if (tag.contains("raining")) value.raining(tag.getBoolean("raining"));
        value.rainingChanged = false;
        if (tag.contains("thundering")) value.thundering(tag.getBoolean("thundering"));
        value.thunderingChanged = false;
        return value;
    }
    static CompoundTag encode(EventWeather value) {
        var tag = value.source.original.copy();
        if (value.clearChanged) { if (value.hasClear()) tag.putInt("clear", value.clear()); else tag.remove("clear"); }
        if (value.rainChanged) { if (value.hasRain()) tag.putInt("rain", value.rain()); else tag.remove("rain"); }
        if (value.thunderChanged) { if (value.hasThunder()) tag.putInt("thunder", value.thunder()); else tag.remove("thunder"); }
        if (value.rainingChanged) { if (value.hasRaining()) tag.putBoolean("raining", value.raining()); else tag.remove("raining"); }
        if (value.thunderingChanged) { if (value.hasThundering()) tag.putBoolean("thundering", value.thundering()); else tag.remove("thundering"); }
        return tag;
    }
    static EventVote decodeEventVote(CompoundTag tag) {
        var value = new EventVote();
        value.source = new Source(tag);
        if (tag.contains("wave")) value.wave(tag.getInt("wave"));
        value.waveChanged = false;
        if (tag.contains("ends")) value.ends(tag.getLong("ends"));
        value.endsChanged = false;
        if (tag.contains("remindAt")) value.remindAt(tag.getLong("remindAt"));
        value.remindAtChanged = false;
        if (tag.contains("resolved")) value.resolved(tag.getBoolean("resolved"));
        value.resolvedChanged = false;
        if (tag.contains("decision")) value.decision(tag.getString("decision"));
        value.decisionChanged = false;
        if (tag.contains("decisionAt")) value.decisionAt(tag.getLong("decisionAt"));
        value.decisionAtChanged = false;
        if (tag.contains("votes", Tag.TAG_COMPOUND)) value.votes(map(tag.getCompound("votes"), UUID::fromString, Tag::getAsString));
        value.votesChanged = false;
        return value;
    }
    static CompoundTag encode(EventVote value) {
        var tag = value.source.original.copy();
        if (value.waveChanged) { if (value.hasWave()) tag.putInt("wave", value.wave()); else tag.remove("wave"); }
        if (value.endsChanged) { if (value.hasEnds()) tag.putLong("ends", value.ends()); else tag.remove("ends"); }
        if (value.remindAtChanged) { if (value.hasRemindAt()) tag.putLong("remindAt", value.remindAt()); else tag.remove("remindAt"); }
        if (value.resolvedChanged) { if (value.hasResolved()) tag.putBoolean("resolved", value.resolved()); else tag.remove("resolved"); }
        if (value.decisionChanged) { if (value.hasDecision()) tag.putString("decision", value.decision()); else tag.remove("decision"); }
        if (value.decisionAtChanged) { if (value.hasDecisionAt()) tag.putLong("decisionAt", value.decisionAt()); else tag.remove("decisionAt"); }
        if (value.votesChanged) { if (value.hasVotes()) tag.put("votes", map(value.votes(), StringTag::valueOf, value.source.original.getCompound("votes"))); else tag.remove("votes"); }
        return tag;
    }
    static EventRewardGroup decodeEventRewardGroup(CompoundTag tag) {
        var value = new EventRewardGroup();
        value.source = new Source(tag);
        if (tag.contains("wave")) value.wave(tag.getInt("wave"));
        value.waveChanged = false;
        if (tag.contains("titleKey")) value.titleKey(tag.getString("titleKey"));
        value.titleKeyChanged = false;
        if (tag.contains("items")) value.items(new NativeNbtPayload(tag.get("items")));
        value.itemsChanged = false;
        return value;
    }
    static CompoundTag encode(EventRewardGroup value) {
        var tag = value.source.original.copy();
        if (value.waveChanged) { if (value.hasWave()) tag.putInt("wave", value.wave()); else tag.remove("wave"); }
        if (value.titleKeyChanged) { if (value.hasTitleKey()) tag.putString("titleKey", value.titleKey()); else tag.remove("titleKey"); }
        if (value.itemsChanged) { if (value.hasItems()) tag.put("items", value.items().copyNativeTag()); else tag.remove("items"); }
        return tag;
    }
    static EventTerrainChange decodeEventTerrainChange(CompoundTag tag) {
        var value = new EventTerrainChange();
        if (!tag.contains("Original") && !tag.isEmpty()) value.legacyOriginal = new NativeNbtPayload(tag);
        value.source = new Source(tag);
        if (tag.contains("Original")) value.original(new NativeNbtPayload(tag.get("Original")));
        value.originalChanged = false;
        if (tag.contains("Temporary")) value.temporary(new NativeNbtPayload(tag.get("Temporary")));
        value.temporaryChanged = false;
        return value;
    }
    static CompoundTag encode(EventTerrainChange value) {
        var tag = value.source.original.copy();
        if (value.originalChanged) { if (value.hasOriginal()) tag.put("Original", value.original().copyNativeTag()); else tag.remove("Original"); }
        if (value.temporaryChanged) { if (value.hasTemporary()) tag.put("Temporary", value.temporary().copyNativeTag()); else tag.remove("Temporary"); }
        return tag;
    }
    static EventCookingProgress decodeEventCookingProgress(CompoundTag tag) {
        var value = new EventCookingProgress();
        value.source = new Source(tag);
        if (tag.contains("rewarded")) value.rewarded(tag.getBoolean("rewarded"));
        value.rewardedChanged = false;
        if (true) value.quantities(integerEntries(tag, "rewarded"));
        value.quantitiesChanged = false;
        return value;
    }
    static CompoundTag encode(EventCookingProgress value) {
        var tag = value.source.original.copy();
        if (value.rewardedChanged) { if (value.hasRewarded()) tag.putBoolean("rewarded", value.rewarded()); else tag.remove("rewarded"); }
        if (value.quantitiesChanged) {
            for (String key : value.source.original.getAllKeys()) if (!key.equals("rewarded") && value.source.original.get(key) instanceof NumericTag) tag.remove(key);
            value.quantities().forEach((key, quantity) -> tag.put(key, preserveNumber(value.source.original.get(key), IntTag.valueOf(quantity))));
        }
        return tag;
    }
    private static <K,V> Map<K,V> map(CompoundTag tag, Function<String,K> keys, Function<Tag,V> values) {
        var result = new LinkedHashMap<K,V>();
        for (String key : tag.getAllKeys()) result.put(keys.apply(key), values.apply(tag.get(key)));
        return result;
    }
    private static <K,V> CompoundTag map(Map<K,V> values, Function<V,Tag> encoder, CompoundTag original) {
        var tag = new CompoundTag(); values.forEach((key,value) -> tag.put(key.toString(), preserveNumber(original.get(key.toString()), encoder.apply(value)))); return tag;
    }
    private static <V> List<V> stringList(Tag tag, Function<String,V> parser) {
        var result = new ArrayList<V>(); if (tag instanceof ListTag list) for (Tag item : list) result.add(parser.apply(item.getAsString())); return result;
    }
    private static <V> List<V> compoundList(ListTag tag, Function<CompoundTag,V> parser) {
        var result = new ArrayList<V>(); for (Tag item : tag) result.add(parser.apply((CompoundTag)item)); return result;
    }
    private static <V> ListTag list(Collection<V> values, Function<V,Tag> encoder) {
        var tag = new ListTag(); values.forEach(value -> tag.add(encoder.apply(value))); return tag;
    }
    private static Map<String,Integer> integerEntries(CompoundTag tag, String excluded) {
        var result = new LinkedHashMap<String,Integer>(); for (String key : tag.getAllKeys()) if (!key.equals(excluded) && tag.get(key) instanceof NumericTag number) result.put(key,number.getAsInt()); return result;
    }
    private static StructureBounds bounds(int[] values) { return values.length == 6 ? new StructureBounds(values[0],values[1],values[2],values[3],values[4],values[5]) : null; }
    private static List<UUID> uuidArrayList(ListTag list) { var result = new ArrayList<UUID>(); for (Tag tag : list) result.add(NbtUtils.loadUUID(tag)); return result; }
    private static Set<Long> longKeys(CompoundTag tag) { var result = new LinkedHashSet<Long>(); for (String key : tag.getAllKeys()) result.add(Long.valueOf(key)); return result; }
    private static Set<UUID> uuidKeys(CompoundTag tag) { var result = new LinkedHashSet<UUID>(); for (String key : tag.getAllKeys()) if (tag.getBoolean(key)) result.add(UUID.fromString(key)); return result; }
    private static CompoundTag set(Collection<?> values, CompoundTag original, boolean truthOnly) {
        var tag = original.copy(); var keys = new HashSet<String>(); values.forEach(value -> keys.add(value.toString()));
        for (String key : original.getAllKeys()) if ((!truthOnly || original.getBoolean(key)) && !keys.contains(key)) tag.remove(key);
        for (String key : keys) if (!original.contains(key) || (truthOnly && !original.getBoolean(key))) tag.putBoolean(key,true);
        return tag;
    }
    private static List<Long> longList(ListTag tag) { var result = new ArrayList<Long>(); for (Tag value : tag) result.add(((NumericTag)value).getAsLong()); return result; }
    private static Tag preserveNumber(Tag original, Tag encoded) {
        if (original instanceof NumericTag before && encoded instanceof NumericTag after) {
            try { if (new java.math.BigDecimal(before.getAsNumber().toString()).compareTo(new java.math.BigDecimal(after.getAsNumber().toString())) == 0) return original.copy(); }
            catch (NumberFormatException ignored) { if (original.equals(encoded)) return original.copy(); }
        }
        return encoded;
    }
    private static EventFishingScore decodeFishingScore(CompoundTag tag) {
        var value = new EventFishingScore(); value.source = new Source(tag);
        if (tag.contains("catches")) value.catches(tag.getInt("catches")); value.catchesChanged = false;
        if (tag.contains("points")) value.points(tag.getInt("points")); value.pointsChanged = false;
        if (tag.contains("fishingXp")) value.fishingXp(tag.getInt("fishingXp")); value.fishingXpChanged = false;
        return value;
    }
    static CompoundTag encode(EventFishingScore value) {
        var tag = value.source.original.copy();
        if (value.catchesChanged) tag.putInt("catches",value.catches());
        if (value.pointsChanged) tag.putInt("points",value.points());
        if (value.fishingXpChanged) tag.putInt("fishingXp",value.fishingXp());
        return tag;
    }
}
