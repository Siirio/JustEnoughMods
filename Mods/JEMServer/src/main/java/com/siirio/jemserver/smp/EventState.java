package com.siirio.jemserver.smp;

import java.util.*;

/** Typed event state. Frozen snapshots cannot mutate canonical or nested state. */
public final class EventState implements SmpRecord {
    private Boolean wave3SetPieceSpawned;
    boolean wave3SetPieceSpawnedChanged;
    private Boolean wave5SetPieceSpawned;
    boolean wave5SetPieceSpawnedChanged;
    private String structureBoss;
    boolean structureBossChanged;
    private Long nextBloodSpawn;
    boolean nextBloodSpawnChanged;
    private String description;
    boolean descriptionChanged;
    private Map<UUID,EventFishingScore> scores;
    boolean scoresChanged;
    private boolean frozen;
    EventNbtCodec.Source source = new EventNbtCodec.Source();
    private UUID id;
    boolean idChanged;
    private UUID owner;
    boolean ownerChanged;
    private UUID attemptId;
    boolean attemptIdChanged;
    private UUID bossEntity;
    boolean bossEntityChanged;
    private UUID encounterHost;
    boolean encounterHostChanged;
    private UUID party;
    boolean partyChanged;
    private UUID structureId;
    boolean structureIdChanged;
    private UUID targetBoss;
    boolean targetBossChanged;
    private String activity;
    boolean activityChanged;
    private String arenaId;
    boolean arenaIdChanged;
    private String bossType;
    boolean bossTypeChanged;
    private String cookingDifficulty;
    boolean cookingDifficultyChanged;
    private String cookingReward;
    boolean cookingRewardChanged;
    private String dimension;
    boolean dimensionChanged;
    private String encounterState;
    boolean encounterStateChanged;
    private String name;
    boolean nameChanged;
    private String raidArenaId;
    boolean raidArenaIdChanged;
    private String slot;
    boolean slotChanged;
    private String state;
    boolean stateChanged;
    private String title;
    boolean titleChanged;
    private String wave3SetPiece;
    boolean wave3SetPieceChanged;
    private String wave5SetPiece;
    boolean wave5SetPieceChanged;
    private Integer revision;
    boolean revisionChanged;
    private Integer batchNumber;
    boolean batchNumberChanged;
    private Integer batchSpawned;
    boolean batchSpawnedChanged;
    private Integer batchTarget;
    boolean batchTargetChanged;
    private Integer continuedWave;
    boolean continuedWaveChanged;
    private Integer fullEquipped;
    boolean fullEquippedChanged;
    private Integer maxX;
    boolean maxXChanged;
    private Integer maxY;
    boolean maxYChanged;
    private Integer maxZ;
    boolean maxZChanged;
    private Integer minX;
    boolean minXChanged;
    private Integer minY;
    boolean minYChanged;
    private Integer minZ;
    boolean minZChanged;
    private Integer radius;
    boolean radiusChanged;
    private Integer raidAlliesSpawned;
    boolean raidAlliesSpawnedChanged;
    private Integer raidReinforcementStage;
    boolean raidReinforcementStageChanged;
    private Integer rewardedWave;
    boolean rewardedWaveChanged;
    private Integer specialEquipped;
    boolean specialEquippedChanged;
    private Integer wave;
    boolean waveChanged;
    private Integer waveKilled;
    boolean waveKilledChanged;
    private Integer waveSpawned;
    boolean waveSpawnedChanged;
    private Integer waveTotal;
    boolean waveTotalChanged;
    private Integer worldTier;
    boolean worldTierChanged;
    private Long created;
    boolean createdChanged;
    private Long arenaRespawnPoint;
    boolean arenaRespawnPointChanged;
    private Long encounterIntroEnds;
    boolean encounterIntroEndsChanged;
    private Long encounterStateSince;
    boolean encounterStateSinceChanged;
    private Long ends;
    boolean endsChanged;
    private Long nextRegionAttempt;
    boolean nextRegionAttemptChanged;
    private Long nextWave;
    boolean nextWaveChanged;
    private Long planned;
    boolean plannedChanged;
    private Long position;
    boolean positionChanged;
    private Long preparationQueued;
    boolean preparationQueuedChanged;
    private Long requestedPosition;
    boolean requestedPositionChanged;
    private Long started;
    boolean startedChanged;
    private Long waveStartedTick;
    boolean waveStartedTickChanged;
    private Boolean catchup;
    boolean catchupChanged;
    private Boolean combatStarted;
    boolean combatStartedChanged;
    private Boolean encounterWalkIn;
    boolean encounterWalkInChanged;
    private Boolean nativePending;
    boolean nativePendingChanged;
    private Boolean ordinaryRaid;
    boolean ordinaryRaidChanged;
    private Boolean preparing;
    boolean preparingChanged;
    private Boolean raid;
    boolean raidChanged;
    private Boolean rushTrackingFull;
    boolean rushTrackingFullChanged;
    private Boolean singleBatchWaveThree;
    boolean singleBatchWaveThreeChanged;
    private Boolean solo;
    boolean soloChanged;
    private Double observedPartyDps;
    boolean observedPartyDpsChanged;
    private Double waveDamageStart;
    boolean waveDamageStartChanged;
    private Map<UUID, EventMember> members;
    boolean membersChanged;
    private EventSetPieceState setPiece = new EventSetPieceState();
    boolean setPieceChanged;
    private EventWeather bloodMoonWeather;
    boolean bloodMoonWeatherChanged;
    private EventVote voting;
    boolean votingChanged;
    private List<UUID> mobs;
    boolean mobsChanged;
    private List<UUID> encounterParticipants;
    boolean encounterParticipantsChanged;
    private List<UUID> invalidatedLoot;
    boolean invalidatedLootChanged;
    private List<UUID> raidAllies;
    boolean raidAlliesChanged;
    private List<UUID> raidBosses;
    boolean raidBossesChanged;
    private Map<String, Integer> remaining;
    boolean remainingChanged;
    private Map<String, Integer> spawnFailures;
    boolean spawnFailuresChanged;
    private Map<String, Boolean> eliteTypes;
    boolean eliteTypesChanged;
    private Map<UUID, Double> raidBossInitialHealth;
    boolean raidBossInitialHealthChanged;
    private Set<Long> placements;
    boolean placementsChanged;
    private Set<UUID> rushParticipants;
    boolean rushParticipantsChanged;
    private Map<String, Integer> cookingRequirements;
    boolean cookingRequirementsChanged;
    private Map<UUID, EventCookingProgress> cookingScores;
    boolean cookingScoresChanged;
    private Map<UUID, List<EventRewardGroup>> pendingWaveRewards;
    boolean pendingWaveRewardsChanged;
    private Map<Long, Boolean> bloodMoonCover;
    boolean bloodMoonCoverChanged;
    private StructureBounds structureBounds;
    boolean structureBoundsChanged;

    private void requireMutable() { if (frozen) throw new IllegalStateException("Immutable event snapshot"); }
    public EventState copy() {
        var copy = new EventState();
        copy.source = source.copy();
        if (scores != null) { var values = new LinkedHashMap<UUID,EventFishingScore>(); scores.forEach((id,value) -> values.put(id,value.copy())); copy.scores = Collections.unmodifiableMap(values); }
        copy.scoresChanged = scoresChanged;
        copy.description = description; copy.descriptionChanged = descriptionChanged;
        copy.nextBloodSpawn = nextBloodSpawn; copy.nextBloodSpawnChanged = nextBloodSpawnChanged;
        copy.structureBoss = structureBoss; copy.structureBossChanged = structureBossChanged;
        copy.wave5SetPieceSpawned = wave5SetPieceSpawned; copy.wave5SetPieceSpawnedChanged = wave5SetPieceSpawnedChanged;
        copy.wave3SetPieceSpawned = wave3SetPieceSpawned; copy.wave3SetPieceSpawnedChanged = wave3SetPieceSpawnedChanged;
        copy.id = id;
        copy.idChanged = idChanged;
        copy.owner = owner;
        copy.ownerChanged = ownerChanged;
        copy.attemptId = attemptId;
        copy.attemptIdChanged = attemptIdChanged;
        copy.bossEntity = bossEntity;
        copy.bossEntityChanged = bossEntityChanged;
        copy.encounterHost = encounterHost;
        copy.encounterHostChanged = encounterHostChanged;
        copy.party = party;
        copy.partyChanged = partyChanged;
        copy.structureId = structureId;
        copy.structureIdChanged = structureIdChanged;
        copy.targetBoss = targetBoss;
        copy.targetBossChanged = targetBossChanged;
        copy.activity = activity;
        copy.activityChanged = activityChanged;
        copy.arenaId = arenaId;
        copy.arenaIdChanged = arenaIdChanged;
        copy.bossType = bossType;
        copy.bossTypeChanged = bossTypeChanged;
        copy.cookingDifficulty = cookingDifficulty;
        copy.cookingDifficultyChanged = cookingDifficultyChanged;
        copy.cookingReward = cookingReward;
        copy.cookingRewardChanged = cookingRewardChanged;
        copy.dimension = dimension;
        copy.dimensionChanged = dimensionChanged;
        copy.encounterState = encounterState;
        copy.encounterStateChanged = encounterStateChanged;
        copy.name = name;
        copy.nameChanged = nameChanged;
        copy.raidArenaId = raidArenaId;
        copy.raidArenaIdChanged = raidArenaIdChanged;
        copy.slot = slot;
        copy.slotChanged = slotChanged;
        copy.state = state;
        copy.stateChanged = stateChanged;
        copy.title = title;
        copy.titleChanged = titleChanged;
        copy.wave3SetPiece = wave3SetPiece;
        copy.wave3SetPieceChanged = wave3SetPieceChanged;
        copy.wave5SetPiece = wave5SetPiece;
        copy.wave5SetPieceChanged = wave5SetPieceChanged;
        copy.revision = revision;
        copy.revisionChanged = revisionChanged;
        copy.batchNumber = batchNumber;
        copy.batchNumberChanged = batchNumberChanged;
        copy.batchSpawned = batchSpawned;
        copy.batchSpawnedChanged = batchSpawnedChanged;
        copy.batchTarget = batchTarget;
        copy.batchTargetChanged = batchTargetChanged;
        copy.continuedWave = continuedWave;
        copy.continuedWaveChanged = continuedWaveChanged;
        copy.fullEquipped = fullEquipped;
        copy.fullEquippedChanged = fullEquippedChanged;
        copy.maxX = maxX;
        copy.maxXChanged = maxXChanged;
        copy.maxY = maxY;
        copy.maxYChanged = maxYChanged;
        copy.maxZ = maxZ;
        copy.maxZChanged = maxZChanged;
        copy.minX = minX;
        copy.minXChanged = minXChanged;
        copy.minY = minY;
        copy.minYChanged = minYChanged;
        copy.minZ = minZ;
        copy.minZChanged = minZChanged;
        copy.radius = radius;
        copy.radiusChanged = radiusChanged;
        copy.raidAlliesSpawned = raidAlliesSpawned;
        copy.raidAlliesSpawnedChanged = raidAlliesSpawnedChanged;
        copy.raidReinforcementStage = raidReinforcementStage;
        copy.raidReinforcementStageChanged = raidReinforcementStageChanged;
        copy.rewardedWave = rewardedWave;
        copy.rewardedWaveChanged = rewardedWaveChanged;
        copy.specialEquipped = specialEquipped;
        copy.specialEquippedChanged = specialEquippedChanged;
        copy.wave = wave;
        copy.waveChanged = waveChanged;
        copy.waveKilled = waveKilled;
        copy.waveKilledChanged = waveKilledChanged;
        copy.waveSpawned = waveSpawned;
        copy.waveSpawnedChanged = waveSpawnedChanged;
        copy.waveTotal = waveTotal;
        copy.waveTotalChanged = waveTotalChanged;
        copy.worldTier = worldTier;
        copy.worldTierChanged = worldTierChanged;
        copy.created = created;
        copy.createdChanged = createdChanged;
        copy.arenaRespawnPoint = arenaRespawnPoint;
        copy.arenaRespawnPointChanged = arenaRespawnPointChanged;
        copy.encounterIntroEnds = encounterIntroEnds;
        copy.encounterIntroEndsChanged = encounterIntroEndsChanged;
        copy.encounterStateSince = encounterStateSince;
        copy.encounterStateSinceChanged = encounterStateSinceChanged;
        copy.ends = ends;
        copy.endsChanged = endsChanged;
        copy.nextRegionAttempt = nextRegionAttempt;
        copy.nextRegionAttemptChanged = nextRegionAttemptChanged;
        copy.nextWave = nextWave;
        copy.nextWaveChanged = nextWaveChanged;
        copy.planned = planned;
        copy.plannedChanged = plannedChanged;
        copy.position = position;
        copy.positionChanged = positionChanged;
        copy.preparationQueued = preparationQueued;
        copy.preparationQueuedChanged = preparationQueuedChanged;
        copy.requestedPosition = requestedPosition;
        copy.requestedPositionChanged = requestedPositionChanged;
        copy.started = started;
        copy.startedChanged = startedChanged;
        copy.waveStartedTick = waveStartedTick;
        copy.waveStartedTickChanged = waveStartedTickChanged;
        copy.catchup = catchup;
        copy.catchupChanged = catchupChanged;
        copy.combatStarted = combatStarted;
        copy.combatStartedChanged = combatStartedChanged;
        copy.encounterWalkIn = encounterWalkIn;
        copy.encounterWalkInChanged = encounterWalkInChanged;
        copy.nativePending = nativePending;
        copy.nativePendingChanged = nativePendingChanged;
        copy.ordinaryRaid = ordinaryRaid;
        copy.ordinaryRaidChanged = ordinaryRaidChanged;
        copy.preparing = preparing;
        copy.preparingChanged = preparingChanged;
        copy.raid = raid;
        copy.raidChanged = raidChanged;
        copy.rushTrackingFull = rushTrackingFull;
        copy.rushTrackingFullChanged = rushTrackingFullChanged;
        copy.singleBatchWaveThree = singleBatchWaveThree;
        copy.singleBatchWaveThreeChanged = singleBatchWaveThreeChanged;
        copy.solo = solo;
        copy.soloChanged = soloChanged;
        copy.observedPartyDps = observedPartyDps;
        copy.observedPartyDpsChanged = observedPartyDpsChanged;
        copy.waveDamageStart = waveDamageStart;
        copy.waveDamageStartChanged = waveDamageStartChanged;
        if (members != null) {
            var values = new LinkedHashMap<UUID, EventMember>();
            members.forEach((id, value) -> values.put(id, value.copy()));
            copy.members = Collections.unmodifiableMap(values);
        }
        copy.membersChanged = membersChanged;
        copy.setPiece = setPiece == null ? null : setPiece.copy();
        copy.setPieceChanged = setPieceChanged;
        copy.bloodMoonWeather = bloodMoonWeather == null ? null : bloodMoonWeather.copy();
        copy.bloodMoonWeatherChanged = bloodMoonWeatherChanged;
        copy.voting = voting == null ? null : voting.copy();
        copy.votingChanged = votingChanged;
        copy.mobs = mobs;
        copy.mobsChanged = mobsChanged;
        copy.encounterParticipants = encounterParticipants;
        copy.encounterParticipantsChanged = encounterParticipantsChanged;
        copy.invalidatedLoot = invalidatedLoot;
        copy.invalidatedLootChanged = invalidatedLootChanged;
        copy.raidAllies = raidAllies;
        copy.raidAlliesChanged = raidAlliesChanged;
        copy.raidBosses = raidBosses;
        copy.raidBossesChanged = raidBossesChanged;
        copy.remaining = remaining;
        copy.remainingChanged = remainingChanged;
        copy.spawnFailures = spawnFailures;
        copy.spawnFailuresChanged = spawnFailuresChanged;
        copy.eliteTypes = eliteTypes;
        copy.eliteTypesChanged = eliteTypesChanged;
        copy.raidBossInitialHealth = raidBossInitialHealth;
        copy.raidBossInitialHealthChanged = raidBossInitialHealthChanged;
        copy.placements = placements;
        copy.placementsChanged = placementsChanged;
        copy.rushParticipants = rushParticipants;
        copy.rushParticipantsChanged = rushParticipantsChanged;
        copy.cookingRequirements = cookingRequirements;
        copy.cookingRequirementsChanged = cookingRequirementsChanged;
        if (cookingScores != null) {
            var values = new LinkedHashMap<UUID, EventCookingProgress>();
            cookingScores.forEach((id, value) -> values.put(id, value.copy()));
            copy.cookingScores = Collections.unmodifiableMap(values);
        }
        copy.cookingScoresChanged = cookingScoresChanged;
        if (pendingWaveRewards != null) {
            var values = new LinkedHashMap<UUID, List<EventRewardGroup>>();
            pendingWaveRewards.forEach((id, value) -> values.put(id, value.stream().map(EventRewardGroup::copy).toList()));
            copy.pendingWaveRewards = Collections.unmodifiableMap(values);
        }
        copy.pendingWaveRewardsChanged = pendingWaveRewardsChanged;
        copy.bloodMoonCover = bloodMoonCover;
        copy.bloodMoonCoverChanged = bloodMoonCoverChanged;
        copy.structureBounds = structureBounds;
        copy.structureBoundsChanged = structureBoundsChanged;
        return copy;
    }
    public EventState snapshot() { var copy = copy(); copy.freeze(); return copy; }
    void freeze() {
        frozen = true;
        if (scores != null) scores.values().forEach(EventFishingScore::freeze);
        if (members != null) members.values().forEach(value -> value.freeze());
        if (setPiece != null) setPiece.freeze();
        if (bloodMoonWeather != null) bloodMoonWeather.freeze();
        if (voting != null) voting.freeze();
        if (cookingScores != null) cookingScores.values().forEach(value -> value.freeze());
        if (pendingWaveRewards != null) pendingWaveRewards.values().forEach(rows -> rows.forEach(EventRewardGroup::freeze));
    }

    public UUID id() { return id == null ? null : id; }
    public void id(UUID value) { requireMutable(); idChanged = true; id = value; }
    public boolean hasId() { return id != null; }
    public void clearId() { requireMutable(); idChanged = true; id = null; }
    public UUID owner() { return owner == null ? null : owner; }
    public void owner(UUID value) { requireMutable(); ownerChanged = true; owner = value; }
    public boolean hasOwner() { return owner != null; }
    public void clearOwner() { requireMutable(); ownerChanged = true; owner = null; }
    public UUID attemptId() { return attemptId == null ? null : attemptId; }
    public void attemptId(UUID value) { requireMutable(); attemptIdChanged = true; attemptId = value; }
    public boolean hasAttemptId() { return attemptId != null; }
    public void clearAttemptId() { requireMutable(); attemptIdChanged = true; attemptId = null; }
    public UUID bossEntity() { return bossEntity == null ? null : bossEntity; }
    public void bossEntity(UUID value) { requireMutable(); bossEntityChanged = true; bossEntity = value; }
    public boolean hasBossEntity() { return bossEntity != null; }
    public void clearBossEntity() { requireMutable(); bossEntityChanged = true; bossEntity = null; }
    public UUID encounterHost() { return encounterHost == null ? null : encounterHost; }
    public void encounterHost(UUID value) { requireMutable(); encounterHostChanged = true; encounterHost = value; }
    public boolean hasEncounterHost() { return encounterHost != null; }
    public void clearEncounterHost() { requireMutable(); encounterHostChanged = true; encounterHost = null; }
    public UUID party() { return party == null ? null : party; }
    public void party(UUID value) { requireMutable(); partyChanged = true; party = value; }
    public boolean hasParty() { return party != null; }
    public void clearParty() { requireMutable(); partyChanged = true; party = null; }
    public UUID structureId() { return structureId == null ? null : structureId; }
    public void structureId(UUID value) { requireMutable(); structureIdChanged = true; structureId = value; }
    public boolean hasStructureId() { return structureId != null; }
    public void clearStructureId() { requireMutable(); structureIdChanged = true; structureId = null; }
    public UUID targetBoss() { return targetBoss == null ? null : targetBoss; }
    public void targetBoss(UUID value) { requireMutable(); targetBossChanged = true; targetBoss = value; }
    public boolean hasTargetBoss() { return targetBoss != null; }
    public void clearTargetBoss() { requireMutable(); targetBossChanged = true; targetBoss = null; }
    public String activity() { return activity == null ? "" : activity; }
    public void activity(String value) { requireMutable(); activityChanged = true; activity = value; }
    public boolean hasActivity() { return activity != null; }
    public void clearActivity() { requireMutable(); activityChanged = true; activity = null; }
    public String arenaId() { return arenaId == null ? "" : arenaId; }
    public void arenaId(String value) { requireMutable(); arenaIdChanged = true; arenaId = value; }
    public boolean hasArenaId() { return arenaId != null; }
    public void clearArenaId() { requireMutable(); arenaIdChanged = true; arenaId = null; }
    public String bossType() { return bossType == null ? "" : bossType; }
    public void bossType(String value) { requireMutable(); bossTypeChanged = true; bossType = value; }
    public boolean hasBossType() { return bossType != null; }
    public void clearBossType() { requireMutable(); bossTypeChanged = true; bossType = null; }
    public String cookingDifficulty() { return cookingDifficulty == null ? "" : cookingDifficulty; }
    public void cookingDifficulty(String value) { requireMutable(); cookingDifficultyChanged = true; cookingDifficulty = value; }
    public boolean hasCookingDifficulty() { return cookingDifficulty != null; }
    public void clearCookingDifficulty() { requireMutable(); cookingDifficultyChanged = true; cookingDifficulty = null; }
    public String cookingReward() { return cookingReward == null ? "" : cookingReward; }
    public void cookingReward(String value) { requireMutable(); cookingRewardChanged = true; cookingReward = value; }
    public boolean hasCookingReward() { return cookingReward != null; }
    public void clearCookingReward() { requireMutable(); cookingRewardChanged = true; cookingReward = null; }
    public String dimension() { return dimension == null ? "" : dimension; }
    public void dimension(String value) { requireMutable(); dimensionChanged = true; dimension = value; }
    public boolean hasDimension() { return dimension != null; }
    public void clearDimension() { requireMutable(); dimensionChanged = true; dimension = null; }
    public String encounterState() { return encounterState == null ? "" : encounterState; }
    public void encounterState(String value) { requireMutable(); encounterStateChanged = true; encounterState = value; }
    public boolean hasEncounterState() { return encounterState != null; }
    public void clearEncounterState() { requireMutable(); encounterStateChanged = true; encounterState = null; }
    public String name() { return name == null ? "" : name; }
    public void name(String value) { requireMutable(); nameChanged = true; name = value; }
    public boolean hasName() { return name != null; }
    public void clearName() { requireMutable(); nameChanged = true; name = null; }
    public String raidArenaId() { return raidArenaId == null ? "" : raidArenaId; }
    public void raidArenaId(String value) { requireMutable(); raidArenaIdChanged = true; raidArenaId = value; }
    public boolean hasRaidArenaId() { return raidArenaId != null; }
    public void clearRaidArenaId() { requireMutable(); raidArenaIdChanged = true; raidArenaId = null; }
    public String slot() { return slot == null ? "" : slot; }
    public void slot(String value) { requireMutable(); slotChanged = true; slot = value; }
    public boolean hasSlot() { return slot != null; }
    public void clearSlot() { requireMutable(); slotChanged = true; slot = null; }
    public String state() { return state == null ? "" : state; }
    public void state(String value) { requireMutable(); stateChanged = true; state = value; }
    public boolean hasState() { return state != null; }
    public void clearState() { requireMutable(); stateChanged = true; state = null; }
    public String title() { return title == null ? "" : title; }
    public void title(String value) { requireMutable(); titleChanged = true; title = value; }
    public boolean hasTitle() { return title != null; }
    public void clearTitle() { requireMutable(); titleChanged = true; title = null; }
    public String wave3SetPiece() { return wave3SetPiece == null ? "" : wave3SetPiece; }
    public void wave3SetPiece(String value) { requireMutable(); wave3SetPieceChanged = true; wave3SetPiece = value; }
    public boolean hasWave3SetPiece() { return wave3SetPiece != null; }
    public void clearWave3SetPiece() { requireMutable(); wave3SetPieceChanged = true; wave3SetPiece = null; }
    public String wave5SetPiece() { return wave5SetPiece == null ? "" : wave5SetPiece; }
    public void wave5SetPiece(String value) { requireMutable(); wave5SetPieceChanged = true; wave5SetPiece = value; }
    public boolean hasWave5SetPiece() { return wave5SetPiece != null; }
    public void clearWave5SetPiece() { requireMutable(); wave5SetPieceChanged = true; wave5SetPiece = null; }
    public int revision() { return revision == null ? 0 : revision; }
    public void revision(int value) { requireMutable(); revisionChanged = true; revision = value; }
    public boolean hasRevision() { return revision != null; }
    public void clearRevision() { requireMutable(); revisionChanged = true; revision = null; }
    public int batchNumber() { return batchNumber == null ? 0 : batchNumber; }
    public void batchNumber(int value) { requireMutable(); batchNumberChanged = true; batchNumber = value; }
    public boolean hasBatchNumber() { return batchNumber != null; }
    public void clearBatchNumber() { requireMutable(); batchNumberChanged = true; batchNumber = null; }
    public int batchSpawned() { return batchSpawned == null ? 0 : batchSpawned; }
    public void batchSpawned(int value) { requireMutable(); batchSpawnedChanged = true; batchSpawned = value; }
    public boolean hasBatchSpawned() { return batchSpawned != null; }
    public void clearBatchSpawned() { requireMutable(); batchSpawnedChanged = true; batchSpawned = null; }
    public int batchTarget() { return batchTarget == null ? 0 : batchTarget; }
    public void batchTarget(int value) { requireMutable(); batchTargetChanged = true; batchTarget = value; }
    public boolean hasBatchTarget() { return batchTarget != null; }
    public void clearBatchTarget() { requireMutable(); batchTargetChanged = true; batchTarget = null; }
    public int continuedWave() { return continuedWave == null ? 0 : continuedWave; }
    public void continuedWave(int value) { requireMutable(); continuedWaveChanged = true; continuedWave = value; }
    public boolean hasContinuedWave() { return continuedWave != null; }
    public void clearContinuedWave() { requireMutable(); continuedWaveChanged = true; continuedWave = null; }
    public int fullEquipped() { return fullEquipped == null ? 0 : fullEquipped; }
    public void fullEquipped(int value) { requireMutable(); fullEquippedChanged = true; fullEquipped = value; }
    public boolean hasFullEquipped() { return fullEquipped != null; }
    public void clearFullEquipped() { requireMutable(); fullEquippedChanged = true; fullEquipped = null; }
    public int maxX() { return maxX == null ? 0 : maxX; }
    public void maxX(int value) { requireMutable(); maxXChanged = true; maxX = value; }
    public boolean hasMaxX() { return maxX != null; }
    public void clearMaxX() { requireMutable(); maxXChanged = true; maxX = null; }
    public int maxY() { return maxY == null ? 0 : maxY; }
    public void maxY(int value) { requireMutable(); maxYChanged = true; maxY = value; }
    public boolean hasMaxY() { return maxY != null; }
    public void clearMaxY() { requireMutable(); maxYChanged = true; maxY = null; }
    public int maxZ() { return maxZ == null ? 0 : maxZ; }
    public void maxZ(int value) { requireMutable(); maxZChanged = true; maxZ = value; }
    public boolean hasMaxZ() { return maxZ != null; }
    public void clearMaxZ() { requireMutable(); maxZChanged = true; maxZ = null; }
    public int minX() { return minX == null ? 0 : minX; }
    public void minX(int value) { requireMutable(); minXChanged = true; minX = value; }
    public boolean hasMinX() { return minX != null; }
    public void clearMinX() { requireMutable(); minXChanged = true; minX = null; }
    public int minY() { return minY == null ? 0 : minY; }
    public void minY(int value) { requireMutable(); minYChanged = true; minY = value; }
    public boolean hasMinY() { return minY != null; }
    public void clearMinY() { requireMutable(); minYChanged = true; minY = null; }
    public int minZ() { return minZ == null ? 0 : minZ; }
    public void minZ(int value) { requireMutable(); minZChanged = true; minZ = value; }
    public boolean hasMinZ() { return minZ != null; }
    public void clearMinZ() { requireMutable(); minZChanged = true; minZ = null; }
    public int radius() { return radius == null ? 0 : radius; }
    public void radius(int value) { requireMutable(); radiusChanged = true; radius = value; }
    public boolean hasRadius() { return radius != null; }
    public void clearRadius() { requireMutable(); radiusChanged = true; radius = null; }
    public int raidAlliesSpawned() { return raidAlliesSpawned == null ? 0 : raidAlliesSpawned; }
    public void raidAlliesSpawned(int value) { requireMutable(); raidAlliesSpawnedChanged = true; raidAlliesSpawned = value; }
    public boolean hasRaidAlliesSpawned() { return raidAlliesSpawned != null; }
    public void clearRaidAlliesSpawned() { requireMutable(); raidAlliesSpawnedChanged = true; raidAlliesSpawned = null; }
    public int raidReinforcementStage() { return raidReinforcementStage == null ? 0 : raidReinforcementStage; }
    public void raidReinforcementStage(int value) { requireMutable(); raidReinforcementStageChanged = true; raidReinforcementStage = value; }
    public boolean hasRaidReinforcementStage() { return raidReinforcementStage != null; }
    public void clearRaidReinforcementStage() { requireMutable(); raidReinforcementStageChanged = true; raidReinforcementStage = null; }
    public int rewardedWave() { return rewardedWave == null ? 0 : rewardedWave; }
    public void rewardedWave(int value) { requireMutable(); rewardedWaveChanged = true; rewardedWave = value; }
    public boolean hasRewardedWave() { return rewardedWave != null; }
    public void clearRewardedWave() { requireMutable(); rewardedWaveChanged = true; rewardedWave = null; }
    public int specialEquipped() { return specialEquipped == null ? 0 : specialEquipped; }
    public void specialEquipped(int value) { requireMutable(); specialEquippedChanged = true; specialEquipped = value; }
    public boolean hasSpecialEquipped() { return specialEquipped != null; }
    public void clearSpecialEquipped() { requireMutable(); specialEquippedChanged = true; specialEquipped = null; }
    public int wave() { return wave == null ? 0 : wave; }
    public void wave(int value) { requireMutable(); waveChanged = true; wave = value; }
    public boolean hasWave() { return wave != null; }
    public void clearWave() { requireMutable(); waveChanged = true; wave = null; }
    public int waveKilled() { return waveKilled == null ? 0 : waveKilled; }
    public void waveKilled(int value) { requireMutable(); waveKilledChanged = true; waveKilled = value; }
    public boolean hasWaveKilled() { return waveKilled != null; }
    public void clearWaveKilled() { requireMutable(); waveKilledChanged = true; waveKilled = null; }
    public int waveSpawned() { return waveSpawned == null ? 0 : waveSpawned; }
    public void waveSpawned(int value) { requireMutable(); waveSpawnedChanged = true; waveSpawned = value; }
    public boolean hasWaveSpawned() { return waveSpawned != null; }
    public void clearWaveSpawned() { requireMutable(); waveSpawnedChanged = true; waveSpawned = null; }
    public int waveTotal() { return waveTotal == null ? 0 : waveTotal; }
    public void waveTotal(int value) { requireMutable(); waveTotalChanged = true; waveTotal = value; }
    public boolean hasWaveTotal() { return waveTotal != null; }
    public void clearWaveTotal() { requireMutable(); waveTotalChanged = true; waveTotal = null; }
    public int worldTier() { return worldTier == null ? 0 : worldTier; }
    public void worldTier(int value) { requireMutable(); worldTierChanged = true; worldTier = value; }
    public boolean hasWorldTier() { return worldTier != null; }
    public void clearWorldTier() { requireMutable(); worldTierChanged = true; worldTier = null; }
    public long created() { return created == null ? 0L : created; }
    public void created(long value) { requireMutable(); createdChanged = true; created = value; }
    public boolean hasCreated() { return created != null; }
    public void clearCreated() { requireMutable(); createdChanged = true; created = null; }
    public long arenaRespawnPoint() { return arenaRespawnPoint == null ? 0L : arenaRespawnPoint; }
    public void arenaRespawnPoint(long value) { requireMutable(); arenaRespawnPointChanged = true; arenaRespawnPoint = value; }
    public boolean hasArenaRespawnPoint() { return arenaRespawnPoint != null; }
    public void clearArenaRespawnPoint() { requireMutable(); arenaRespawnPointChanged = true; arenaRespawnPoint = null; }
    public long encounterIntroEnds() { return encounterIntroEnds == null ? 0L : encounterIntroEnds; }
    public void encounterIntroEnds(long value) { requireMutable(); encounterIntroEndsChanged = true; encounterIntroEnds = value; }
    public boolean hasEncounterIntroEnds() { return encounterIntroEnds != null; }
    public void clearEncounterIntroEnds() { requireMutable(); encounterIntroEndsChanged = true; encounterIntroEnds = null; }
    public long encounterStateSince() { return encounterStateSince == null ? 0L : encounterStateSince; }
    public void encounterStateSince(long value) { requireMutable(); encounterStateSinceChanged = true; encounterStateSince = value; }
    public boolean hasEncounterStateSince() { return encounterStateSince != null; }
    public void clearEncounterStateSince() { requireMutable(); encounterStateSinceChanged = true; encounterStateSince = null; }
    public long ends() { return ends == null ? 0L : ends; }
    public void ends(long value) { requireMutable(); endsChanged = true; ends = value; }
    public boolean hasEnds() { return ends != null; }
    public void clearEnds() { requireMutable(); endsChanged = true; ends = null; }
    public long nextRegionAttempt() { return nextRegionAttempt == null ? 0L : nextRegionAttempt; }
    public void nextRegionAttempt(long value) { requireMutable(); nextRegionAttemptChanged = true; nextRegionAttempt = value; }
    public boolean hasNextRegionAttempt() { return nextRegionAttempt != null; }
    public void clearNextRegionAttempt() { requireMutable(); nextRegionAttemptChanged = true; nextRegionAttempt = null; }
    public long nextWave() { return nextWave == null ? 0L : nextWave; }
    public void nextWave(long value) { requireMutable(); nextWaveChanged = true; nextWave = value; }
    public boolean hasNextWave() { return nextWave != null; }
    public void clearNextWave() { requireMutable(); nextWaveChanged = true; nextWave = null; }
    public long planned() { return planned == null ? 0L : planned; }
    public void planned(long value) { requireMutable(); plannedChanged = true; planned = value; }
    public boolean hasPlanned() { return planned != null; }
    public void clearPlanned() { requireMutable(); plannedChanged = true; planned = null; }
    public long position() { return position == null ? 0L : position; }
    public void position(long value) { requireMutable(); positionChanged = true; position = value; }
    public boolean hasPosition() { return position != null; }
    public void clearPosition() { requireMutable(); positionChanged = true; position = null; }
    public long preparationQueued() { return preparationQueued == null ? 0L : preparationQueued; }
    public void preparationQueued(long value) { requireMutable(); preparationQueuedChanged = true; preparationQueued = value; }
    public boolean hasPreparationQueued() { return preparationQueued != null; }
    public void clearPreparationQueued() { requireMutable(); preparationQueuedChanged = true; preparationQueued = null; }
    public long requestedPosition() { return requestedPosition == null ? 0L : requestedPosition; }
    public void requestedPosition(long value) { requireMutable(); requestedPositionChanged = true; requestedPosition = value; }
    public boolean hasRequestedPosition() { return requestedPosition != null; }
    public void clearRequestedPosition() { requireMutable(); requestedPositionChanged = true; requestedPosition = null; }
    public long started() { return started == null ? 0L : started; }
    public void started(long value) { requireMutable(); startedChanged = true; started = value; }
    public boolean hasStarted() { return started != null; }
    public void clearStarted() { requireMutable(); startedChanged = true; started = null; }
    public long waveStartedTick() { return waveStartedTick == null ? 0L : waveStartedTick; }
    public void waveStartedTick(long value) { requireMutable(); waveStartedTickChanged = true; waveStartedTick = value; }
    public boolean hasWaveStartedTick() { return waveStartedTick != null; }
    public void clearWaveStartedTick() { requireMutable(); waveStartedTickChanged = true; waveStartedTick = null; }
    public boolean catchup() { return catchup == null ? false : catchup; }
    public void catchup(boolean value) { requireMutable(); catchupChanged = true; catchup = value; }
    public boolean hasCatchup() { return catchup != null; }
    public void clearCatchup() { requireMutable(); catchupChanged = true; catchup = null; }
    public boolean combatStarted() { return combatStarted == null ? false : combatStarted; }
    public void combatStarted(boolean value) { requireMutable(); combatStartedChanged = true; combatStarted = value; }
    public boolean hasCombatStarted() { return combatStarted != null; }
    public void clearCombatStarted() { requireMutable(); combatStartedChanged = true; combatStarted = null; }
    public boolean encounterWalkIn() { return encounterWalkIn == null ? false : encounterWalkIn; }
    public void encounterWalkIn(boolean value) { requireMutable(); encounterWalkInChanged = true; encounterWalkIn = value; }
    public boolean hasEncounterWalkIn() { return encounterWalkIn != null; }
    public void clearEncounterWalkIn() { requireMutable(); encounterWalkInChanged = true; encounterWalkIn = null; }
    public boolean nativePending() { return nativePending == null ? false : nativePending; }
    public void nativePending(boolean value) { requireMutable(); nativePendingChanged = true; nativePending = value; }
    public boolean hasNativePending() { return nativePending != null; }
    public void clearNativePending() { requireMutable(); nativePendingChanged = true; nativePending = null; }
    public boolean ordinaryRaid() { return ordinaryRaid == null ? false : ordinaryRaid; }
    public void ordinaryRaid(boolean value) { requireMutable(); ordinaryRaidChanged = true; ordinaryRaid = value; }
    public boolean hasOrdinaryRaid() { return ordinaryRaid != null; }
    public void clearOrdinaryRaid() { requireMutable(); ordinaryRaidChanged = true; ordinaryRaid = null; }
    public boolean preparing() { return preparing == null ? false : preparing; }
    public void preparing(boolean value) { requireMutable(); preparingChanged = true; preparing = value; }
    public boolean hasPreparing() { return preparing != null; }
    public void clearPreparing() { requireMutable(); preparingChanged = true; preparing = null; }
    public boolean raid() { return raid == null ? false : raid; }
    public void raid(boolean value) { requireMutable(); raidChanged = true; raid = value; }
    public boolean hasRaid() { return raid != null; }
    public void clearRaid() { requireMutable(); raidChanged = true; raid = null; }
    public boolean rushTrackingFull() { return rushTrackingFull == null ? false : rushTrackingFull; }
    public void rushTrackingFull(boolean value) { requireMutable(); rushTrackingFullChanged = true; rushTrackingFull = value; }
    public boolean hasRushTrackingFull() { return rushTrackingFull != null; }
    public void clearRushTrackingFull() { requireMutable(); rushTrackingFullChanged = true; rushTrackingFull = null; }
    public boolean singleBatchWaveThree() { return singleBatchWaveThree == null ? false : singleBatchWaveThree; }
    public void singleBatchWaveThree(boolean value) { requireMutable(); singleBatchWaveThreeChanged = true; singleBatchWaveThree = value; }
    public boolean hasSingleBatchWaveThree() { return singleBatchWaveThree != null; }
    public void clearSingleBatchWaveThree() { requireMutable(); singleBatchWaveThreeChanged = true; singleBatchWaveThree = null; }
    public boolean solo() { return solo == null ? false : solo; }
    public void solo(boolean value) { requireMutable(); soloChanged = true; solo = value; }
    public boolean hasSolo() { return solo != null; }
    public void clearSolo() { requireMutable(); soloChanged = true; solo = null; }
    public double observedPartyDps() { return observedPartyDps == null ? 0D : observedPartyDps; }
    public void observedPartyDps(double value) { requireMutable(); observedPartyDpsChanged = true; observedPartyDps = value; }
    public boolean hasObservedPartyDps() { return observedPartyDps != null; }
    public void clearObservedPartyDps() { requireMutable(); observedPartyDpsChanged = true; observedPartyDps = null; }
    public double waveDamageStart() { return waveDamageStart == null ? 0D : waveDamageStart; }
    public void waveDamageStart(double value) { requireMutable(); waveDamageStartChanged = true; waveDamageStart = value; }
    public boolean hasWaveDamageStart() { return waveDamageStart != null; }
    public void clearWaveDamageStart() { requireMutable(); waveDamageStartChanged = true; waveDamageStart = null; }
    public Map<UUID, EventMember> members() { return members == null ? Map.of() : members; }
    public void members(Map<UUID, EventMember> value) { requireMutable(); membersChanged = true; members = Collections.unmodifiableMap(new LinkedHashMap<>(value)); }
    public boolean hasMembers() { return members != null; }
    public void clearMembers() { requireMutable(); membersChanged = true; members = null; }
    public EventSetPieceState setPiece() { if (setPiece != null) return setPiece; var empty = new EventSetPieceState(); empty.freeze(); return empty; }
    public EventSetPieceState editSetPiece() { requireMutable(); if (setPiece == null) { setPiece = new EventSetPieceState(); setPieceChanged = true; } return setPiece; }
    public void setPiece(EventSetPieceState value) { requireMutable(); setPieceChanged = true; setPiece = Objects.requireNonNull(value); }
    public boolean hasSetPiece() { return setPiece != null; }
    public void clearSetPiece() { requireMutable(); setPieceChanged = true; setPiece = null; }
    public EventWeather bloodMoonWeather() { if (bloodMoonWeather != null) return bloodMoonWeather; var empty = new EventWeather(); empty.freeze(); return empty; }
    public void bloodMoonWeather(EventWeather value) { requireMutable(); bloodMoonWeatherChanged = true; bloodMoonWeather = value; }
    public boolean hasBloodMoonWeather() { return bloodMoonWeather != null; }
    public void clearBloodMoonWeather() { requireMutable(); bloodMoonWeatherChanged = true; bloodMoonWeather = null; }
    public EventVote voting() { if (voting != null) return voting; var empty = new EventVote(); empty.freeze(); return empty; }
    public void voting(EventVote value) { requireMutable(); votingChanged = true; voting = value; }
    public boolean hasVoting() { return voting != null; }
    public void clearVoting() { requireMutable(); votingChanged = true; voting = null; }
    public List<UUID> mobs() { return mobs == null ? List.of() : mobs; }
    public void mobs(List<UUID> value) { requireMutable(); mobsChanged = true; mobs = List.copyOf(value); }
    public boolean hasMobs() { return mobs != null; }
    public void clearMobs() { requireMutable(); mobsChanged = true; mobs = null; }
    public List<UUID> encounterParticipants() { return encounterParticipants == null ? List.of() : encounterParticipants; }
    public void encounterParticipants(List<UUID> value) { requireMutable(); encounterParticipantsChanged = true; encounterParticipants = List.copyOf(value); }
    public boolean hasEncounterParticipants() { return encounterParticipants != null; }
    public void clearEncounterParticipants() { requireMutable(); encounterParticipantsChanged = true; encounterParticipants = null; }
    public List<UUID> invalidatedLoot() { return invalidatedLoot == null ? List.of() : invalidatedLoot; }
    public void invalidatedLoot(List<UUID> value) { requireMutable(); invalidatedLootChanged = true; invalidatedLoot = List.copyOf(value); }
    public boolean hasInvalidatedLoot() { return invalidatedLoot != null; }
    public void clearInvalidatedLoot() { requireMutable(); invalidatedLootChanged = true; invalidatedLoot = null; }
    public List<UUID> raidAllies() { return raidAllies == null ? List.of() : raidAllies; }
    public void raidAllies(List<UUID> value) { requireMutable(); raidAlliesChanged = true; raidAllies = List.copyOf(value); }
    public boolean hasRaidAllies() { return raidAllies != null; }
    public void clearRaidAllies() { requireMutable(); raidAlliesChanged = true; raidAllies = null; }
    public List<UUID> raidBosses() { return raidBosses == null ? List.of() : raidBosses; }
    public void raidBosses(List<UUID> value) { requireMutable(); raidBossesChanged = true; raidBosses = List.copyOf(value); }
    public boolean hasRaidBosses() { return raidBosses != null; }
    public void clearRaidBosses() { requireMutable(); raidBossesChanged = true; raidBosses = null; }
    public Map<String, Integer> remaining() { return remaining == null ? Map.of() : remaining; }
    public void remaining(Map<String, Integer> value) { requireMutable(); remainingChanged = true; remaining = Collections.unmodifiableMap(new LinkedHashMap<>(value)); }
    public boolean hasRemaining() { return remaining != null; }
    public void clearRemaining() { requireMutable(); remainingChanged = true; remaining = null; }
    public Map<String, Integer> spawnFailures() { return spawnFailures == null ? Map.of() : spawnFailures; }
    public void spawnFailures(Map<String, Integer> value) { requireMutable(); spawnFailuresChanged = true; spawnFailures = Collections.unmodifiableMap(new LinkedHashMap<>(value)); }
    public boolean hasSpawnFailures() { return spawnFailures != null; }
    public void clearSpawnFailures() { requireMutable(); spawnFailuresChanged = true; spawnFailures = null; }
    public Map<String, Boolean> eliteTypes() { return eliteTypes == null ? Map.of() : eliteTypes; }
    public void eliteTypes(Map<String, Boolean> value) { requireMutable(); eliteTypesChanged = true; eliteTypes = Collections.unmodifiableMap(new LinkedHashMap<>(value)); }
    public boolean hasEliteTypes() { return eliteTypes != null; }
    public void clearEliteTypes() { requireMutable(); eliteTypesChanged = true; eliteTypes = null; }
    public Map<UUID, Double> raidBossInitialHealth() { return raidBossInitialHealth == null ? Map.of() : raidBossInitialHealth; }
    public void raidBossInitialHealth(Map<UUID, Double> value) { requireMutable(); raidBossInitialHealthChanged = true; raidBossInitialHealth = Collections.unmodifiableMap(new LinkedHashMap<>(value)); }
    public boolean hasRaidBossInitialHealth() { return raidBossInitialHealth != null; }
    public void clearRaidBossInitialHealth() { requireMutable(); raidBossInitialHealthChanged = true; raidBossInitialHealth = null; }
    public Set<Long> placements() { return placements == null ? Set.of() : placements; }
    public void placements(Set<Long> value) { requireMutable(); placementsChanged = true; placements = Collections.unmodifiableSet(new LinkedHashSet<>(value)); }
    public boolean hasPlacements() { return placements != null; }
    public void clearPlacements() { requireMutable(); placementsChanged = true; placements = null; }
    public Set<UUID> rushParticipants() { return rushParticipants == null ? Set.of() : rushParticipants; }
    public void rushParticipants(Set<UUID> value) { requireMutable(); rushParticipantsChanged = true; rushParticipants = Collections.unmodifiableSet(new LinkedHashSet<>(value)); }
    public boolean hasRushParticipants() { return rushParticipants != null; }
    public void clearRushParticipants() { requireMutable(); rushParticipantsChanged = true; rushParticipants = null; }
    public Map<String, Integer> cookingRequirements() { return cookingRequirements == null ? Map.of() : cookingRequirements; }
    public void cookingRequirements(Map<String, Integer> value) { requireMutable(); cookingRequirementsChanged = true; cookingRequirements = Collections.unmodifiableMap(new LinkedHashMap<>(value)); }
    public boolean hasCookingRequirements() { return cookingRequirements != null; }
    public void clearCookingRequirements() { requireMutable(); cookingRequirementsChanged = true; cookingRequirements = null; }
    public Map<UUID, EventCookingProgress> cookingScores() { return cookingScores == null ? Map.of() : cookingScores; }
    public void cookingScores(Map<UUID, EventCookingProgress> value) { requireMutable(); cookingScoresChanged = true; cookingScores = Collections.unmodifiableMap(new LinkedHashMap<>(value)); }
    public boolean hasCookingScores() { return cookingScores != null; }
    public void clearCookingScores() { requireMutable(); cookingScoresChanged = true; cookingScores = null; }
    public Map<UUID, List<EventRewardGroup>> pendingWaveRewards() { return pendingWaveRewards == null ? Map.of() : pendingWaveRewards; }
    public void pendingWaveRewards(Map<UUID, List<EventRewardGroup>> value) { requireMutable(); pendingWaveRewardsChanged = true; var copied = new LinkedHashMap<UUID,List<EventRewardGroup>>(); value.forEach((id, rows) -> copied.put(id, List.copyOf(rows))); pendingWaveRewards = Collections.unmodifiableMap(copied); }
    public boolean hasPendingWaveRewards() { return pendingWaveRewards != null; }
    public void clearPendingWaveRewards() { requireMutable(); pendingWaveRewardsChanged = true; pendingWaveRewards = null; }
    public Map<Long, Boolean> bloodMoonCover() { return bloodMoonCover == null ? Map.of() : bloodMoonCover; }
    public void bloodMoonCover(Map<Long, Boolean> value) { requireMutable(); bloodMoonCoverChanged = true; bloodMoonCover = Collections.unmodifiableMap(new LinkedHashMap<>(value)); }
    public boolean hasBloodMoonCover() { return bloodMoonCover != null; }
    public void clearBloodMoonCover() { requireMutable(); bloodMoonCoverChanged = true; bloodMoonCover = null; }
    public StructureBounds structureBounds() { return structureBounds; }
    public void structureBounds(StructureBounds value) { requireMutable(); structureBoundsChanged = true; structureBounds = value; }
    public boolean hasStructureBounds() { return structureBounds != null; }
    public void clearStructureBounds() { requireMutable(); structureBoundsChanged = true; structureBounds = null; }
    public boolean closed() { return Set.of("CLOSED", "CANCELLED", "COMPLETED", "FAILED", "SKIPPED").contains(state()); }
    public EventMember member(UUID id) { return members().get(id); }
    public void addMember(UUID id, EventMember member) { var values = new LinkedHashMap<>(members()); values.put(id, member); members(values); }
    public void removeMember(UUID id) { var values = new LinkedHashMap<>(members()); values.remove(id); members(values); }
    public boolean wave3SetPieceSpawned() { return wave3SetPieceSpawned == null ? false : wave3SetPieceSpawned; }
    public void wave3SetPieceSpawned(boolean value) { requireMutable(); wave3SetPieceSpawnedChanged = true; wave3SetPieceSpawned = value; }
    public boolean hasWave3SetPieceSpawned() { return wave3SetPieceSpawned != null; }
    public void clearWave3SetPieceSpawned() { requireMutable(); wave3SetPieceSpawnedChanged = true; wave3SetPieceSpawned = null; }
    public boolean wave5SetPieceSpawned() { return wave5SetPieceSpawned == null ? false : wave5SetPieceSpawned; }
    public void wave5SetPieceSpawned(boolean value) { requireMutable(); wave5SetPieceSpawnedChanged = true; wave5SetPieceSpawned = value; }
    public boolean hasWave5SetPieceSpawned() { return wave5SetPieceSpawned != null; }
    public void clearWave5SetPieceSpawned() { requireMutable(); wave5SetPieceSpawnedChanged = true; wave5SetPieceSpawned = null; }
    public String structureBoss() { return structureBoss == null ? "" : structureBoss; }
    public void structureBoss(String value) { requireMutable(); structureBossChanged = true; structureBoss = value; }
    public boolean hasStructureBoss() { return structureBoss != null; }
    public void clearStructureBoss() { requireMutable(); structureBossChanged = true; structureBoss = null; }
    public EventMember memberOrEmpty(UUID id) { var member = members().get(id); if (member != null) return member; var empty = new EventMember(); if (frozen) empty.freeze(); return empty; }
    public long nextBloodSpawn() { return nextBloodSpawn == null ? 0L : nextBloodSpawn; }
    public void nextBloodSpawn(long value) { requireMutable(); nextBloodSpawn = value; nextBloodSpawnChanged = true; }
    public boolean hasNextBloodSpawn() { return nextBloodSpawn != null; }
    public void clearNextBloodSpawn() { requireMutable(); nextBloodSpawn = null; nextBloodSpawnChanged = true; }
    public String description() { return description == null ? "" : description; }
    public void description(String value) { requireMutable(); description = value; descriptionChanged = true; }
    public boolean hasDescription() { return description != null; }
    public void clearDescription() { requireMutable(); description = null; descriptionChanged = true; }
    public Map<UUID,EventFishingScore> scores() { return scores == null ? Map.of() : scores; }
    public void scores(Map<UUID,EventFishingScore> value) { requireMutable(); scores = Collections.unmodifiableMap(new LinkedHashMap<>(value)); scoresChanged = true; }
    public boolean hasScores() { return scores != null; }
    public void clearScores() { requireMutable(); scores = null; scoresChanged = true; }
}
