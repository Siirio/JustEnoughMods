package com.siirio.jemserver.smp;

import java.util.*;

/** Typed event state. Frozen snapshots cannot mutate canonical or nested state. */
public final class EventSetPieceState {
    private boolean frozen;
    EventNbtCodec.Source source = new EventNbtCodec.Source();
    private String attack;
    boolean attackChanged;
    private String attackPhase;
    boolean attackPhaseChanged;
    private String lastAttack;
    boolean lastAttackChanged;
    private String previousAttack;
    boolean previousAttackChanged;
    private Integer arenaWave;
    boolean arenaWaveChanged;
    private Integer castParity;
    boolean castParityChanged;
    private Integer comboIndex;
    boolean comboIndexChanged;
    private Integer congregation;
    boolean congregationChanged;
    private Integer debuffProfileCursor;
    boolean debuffProfileCursorChanged;
    private Integer floorIndex;
    boolean floorIndexChanged;
    private Integer safeQuarter;
    boolean safeQuarterChanged;
    private Integer twinPattern;
    boolean twinPatternChanged;
    private Long attackBlocksUntil;
    boolean attackBlocksUntilChanged;
    private Long attackStart;
    boolean attackStartChanged;
    private Long comboNext;
    boolean comboNextChanged;
    private Long debuffNextCast;
    boolean debuffNextCastChanged;
    private Long impact;
    boolean impactChanged;
    private Long phaseUntil;
    boolean phaseUntilChanged;
    private Long recoveryUntil;
    boolean recoveryUntilChanged;
    private Boolean comboActive;
    boolean comboActiveChanged;
    private Boolean finalUsed;
    boolean finalUsedChanged;
    private Boolean restoring;
    boolean restoringChanged;
    private Boolean reverse;
    boolean reverseChanged;
    private Double initialHealth;
    boolean initialHealthChanged;
    private Double originX;
    boolean originXChanged;
    private Double originZ;
    boolean originZChanged;
    private Double secondX;
    boolean secondXChanged;
    private Double secondZ;
    boolean secondZChanged;
    private Double targetX;
    boolean targetXChanged;
    private Double targetZ;
    boolean targetZChanged;
    private List<UUID> mobs;
    boolean mobsChanged;
    private List<UUID> bosses;
    boolean bossesChanged;
    private Map<String, Long> cooldowns;
    boolean cooldownsChanged;
    private Map<Long, EventTerrainChange> terrain;
    boolean terrainChanged;
    private Map<Long, EventTerrainChange> attackTerrain;
    boolean attackTerrainChanged;
    private List<Long> attackBlocks;
    boolean attackBlocksChanged;
    private List<Long> floorQueue;
    boolean floorQueueChanged;
    private List<String> comboQueue;
    boolean comboQueueChanged;

    private void requireMutable() { if (frozen) throw new IllegalStateException("Immutable event snapshot"); }
    public EventSetPieceState copy() {
        var copy = new EventSetPieceState();
        copy.source = source.copy();
        copy.attack = attack;
        copy.attackChanged = attackChanged;
        copy.attackPhase = attackPhase;
        copy.attackPhaseChanged = attackPhaseChanged;
        copy.lastAttack = lastAttack;
        copy.lastAttackChanged = lastAttackChanged;
        copy.previousAttack = previousAttack;
        copy.previousAttackChanged = previousAttackChanged;
        copy.arenaWave = arenaWave;
        copy.arenaWaveChanged = arenaWaveChanged;
        copy.castParity = castParity;
        copy.castParityChanged = castParityChanged;
        copy.comboIndex = comboIndex;
        copy.comboIndexChanged = comboIndexChanged;
        copy.congregation = congregation;
        copy.congregationChanged = congregationChanged;
        copy.debuffProfileCursor = debuffProfileCursor;
        copy.debuffProfileCursorChanged = debuffProfileCursorChanged;
        copy.floorIndex = floorIndex;
        copy.floorIndexChanged = floorIndexChanged;
        copy.safeQuarter = safeQuarter;
        copy.safeQuarterChanged = safeQuarterChanged;
        copy.twinPattern = twinPattern;
        copy.twinPatternChanged = twinPatternChanged;
        copy.attackBlocksUntil = attackBlocksUntil;
        copy.attackBlocksUntilChanged = attackBlocksUntilChanged;
        copy.attackStart = attackStart;
        copy.attackStartChanged = attackStartChanged;
        copy.comboNext = comboNext;
        copy.comboNextChanged = comboNextChanged;
        copy.debuffNextCast = debuffNextCast;
        copy.debuffNextCastChanged = debuffNextCastChanged;
        copy.impact = impact;
        copy.impactChanged = impactChanged;
        copy.phaseUntil = phaseUntil;
        copy.phaseUntilChanged = phaseUntilChanged;
        copy.recoveryUntil = recoveryUntil;
        copy.recoveryUntilChanged = recoveryUntilChanged;
        copy.comboActive = comboActive;
        copy.comboActiveChanged = comboActiveChanged;
        copy.finalUsed = finalUsed;
        copy.finalUsedChanged = finalUsedChanged;
        copy.restoring = restoring;
        copy.restoringChanged = restoringChanged;
        copy.reverse = reverse;
        copy.reverseChanged = reverseChanged;
        copy.initialHealth = initialHealth;
        copy.initialHealthChanged = initialHealthChanged;
        copy.originX = originX;
        copy.originXChanged = originXChanged;
        copy.originZ = originZ;
        copy.originZChanged = originZChanged;
        copy.secondX = secondX;
        copy.secondXChanged = secondXChanged;
        copy.secondZ = secondZ;
        copy.secondZChanged = secondZChanged;
        copy.targetX = targetX;
        copy.targetXChanged = targetXChanged;
        copy.targetZ = targetZ;
        copy.targetZChanged = targetZChanged;
        copy.mobs = mobs;
        copy.mobsChanged = mobsChanged;
        copy.bosses = bosses;
        copy.bossesChanged = bossesChanged;
        copy.cooldowns = cooldowns;
        copy.cooldownsChanged = cooldownsChanged;
        if (terrain != null) {
            var values = new LinkedHashMap<Long, EventTerrainChange>();
            terrain.forEach((id, value) -> values.put(id, value.copy()));
            copy.terrain = Collections.unmodifiableMap(values);
        }
        copy.terrainChanged = terrainChanged;
        if (attackTerrain != null) {
            var values = new LinkedHashMap<Long, EventTerrainChange>();
            attackTerrain.forEach((id, value) -> values.put(id, value.copy()));
            copy.attackTerrain = Collections.unmodifiableMap(values);
        }
        copy.attackTerrainChanged = attackTerrainChanged;
        copy.attackBlocks = attackBlocks;
        copy.attackBlocksChanged = attackBlocksChanged;
        copy.floorQueue = floorQueue;
        copy.floorQueueChanged = floorQueueChanged;
        copy.comboQueue = comboQueue;
        copy.comboQueueChanged = comboQueueChanged;
        return copy;
    }
    public EventSetPieceState snapshot() { var copy = copy(); copy.freeze(); return copy; }
    void freeze() {
        frozen = true;
        if (terrain != null) terrain.values().forEach(value -> value.freeze());
        if (attackTerrain != null) attackTerrain.values().forEach(value -> value.freeze());
    }

    public String attack() { return attack == null ? "" : attack; }
    public void attack(String value) { requireMutable(); attackChanged = true; attack = value; }
    public boolean hasAttack() { return attack != null; }
    public void clearAttack() { requireMutable(); attackChanged = true; attack = null; }
    public String attackPhase() { return attackPhase == null ? "" : attackPhase; }
    public void attackPhase(String value) { requireMutable(); attackPhaseChanged = true; attackPhase = value; }
    public boolean hasAttackPhase() { return attackPhase != null; }
    public void clearAttackPhase() { requireMutable(); attackPhaseChanged = true; attackPhase = null; }
    public String lastAttack() { return lastAttack == null ? "" : lastAttack; }
    public void lastAttack(String value) { requireMutable(); lastAttackChanged = true; lastAttack = value; }
    public boolean hasLastAttack() { return lastAttack != null; }
    public void clearLastAttack() { requireMutable(); lastAttackChanged = true; lastAttack = null; }
    public String previousAttack() { return previousAttack == null ? "" : previousAttack; }
    public void previousAttack(String value) { requireMutable(); previousAttackChanged = true; previousAttack = value; }
    public boolean hasPreviousAttack() { return previousAttack != null; }
    public void clearPreviousAttack() { requireMutable(); previousAttackChanged = true; previousAttack = null; }
    public int arenaWave() { return arenaWave == null ? 0 : arenaWave; }
    public void arenaWave(int value) { requireMutable(); arenaWaveChanged = true; arenaWave = value; }
    public boolean hasArenaWave() { return arenaWave != null; }
    public void clearArenaWave() { requireMutable(); arenaWaveChanged = true; arenaWave = null; }
    public int castParity() { return castParity == null ? 0 : castParity; }
    public void castParity(int value) { requireMutable(); castParityChanged = true; castParity = value; }
    public boolean hasCastParity() { return castParity != null; }
    public void clearCastParity() { requireMutable(); castParityChanged = true; castParity = null; }
    public int comboIndex() { return comboIndex == null ? 0 : comboIndex; }
    public void comboIndex(int value) { requireMutable(); comboIndexChanged = true; comboIndex = value; }
    public boolean hasComboIndex() { return comboIndex != null; }
    public void clearComboIndex() { requireMutable(); comboIndexChanged = true; comboIndex = null; }
    public int congregation() { return congregation == null ? 0 : congregation; }
    public void congregation(int value) { requireMutable(); congregationChanged = true; congregation = value; }
    public boolean hasCongregation() { return congregation != null; }
    public void clearCongregation() { requireMutable(); congregationChanged = true; congregation = null; }
    public int debuffProfileCursor() { return debuffProfileCursor == null ? 0 : debuffProfileCursor; }
    public void debuffProfileCursor(int value) { requireMutable(); debuffProfileCursorChanged = true; debuffProfileCursor = value; }
    public boolean hasDebuffProfileCursor() { return debuffProfileCursor != null; }
    public void clearDebuffProfileCursor() { requireMutable(); debuffProfileCursorChanged = true; debuffProfileCursor = null; }
    public int floorIndex() { return floorIndex == null ? 0 : floorIndex; }
    public void floorIndex(int value) { requireMutable(); floorIndexChanged = true; floorIndex = value; }
    public boolean hasFloorIndex() { return floorIndex != null; }
    public void clearFloorIndex() { requireMutable(); floorIndexChanged = true; floorIndex = null; }
    public int safeQuarter() { return safeQuarter == null ? 0 : safeQuarter; }
    public void safeQuarter(int value) { requireMutable(); safeQuarterChanged = true; safeQuarter = value; }
    public boolean hasSafeQuarter() { return safeQuarter != null; }
    public void clearSafeQuarter() { requireMutable(); safeQuarterChanged = true; safeQuarter = null; }
    public int twinPattern() { return twinPattern == null ? 0 : twinPattern; }
    public void twinPattern(int value) { requireMutable(); twinPatternChanged = true; twinPattern = value; }
    public boolean hasTwinPattern() { return twinPattern != null; }
    public void clearTwinPattern() { requireMutable(); twinPatternChanged = true; twinPattern = null; }
    public long attackBlocksUntil() { return attackBlocksUntil == null ? 0L : attackBlocksUntil; }
    public void attackBlocksUntil(long value) { requireMutable(); attackBlocksUntilChanged = true; attackBlocksUntil = value; }
    public boolean hasAttackBlocksUntil() { return attackBlocksUntil != null; }
    public void clearAttackBlocksUntil() { requireMutable(); attackBlocksUntilChanged = true; attackBlocksUntil = null; }
    public long attackStart() { return attackStart == null ? 0L : attackStart; }
    public void attackStart(long value) { requireMutable(); attackStartChanged = true; attackStart = value; }
    public boolean hasAttackStart() { return attackStart != null; }
    public void clearAttackStart() { requireMutable(); attackStartChanged = true; attackStart = null; }
    public long comboNext() { return comboNext == null ? 0L : comboNext; }
    public void comboNext(long value) { requireMutable(); comboNextChanged = true; comboNext = value; }
    public boolean hasComboNext() { return comboNext != null; }
    public void clearComboNext() { requireMutable(); comboNextChanged = true; comboNext = null; }
    public long debuffNextCast() { return debuffNextCast == null ? 0L : debuffNextCast; }
    public void debuffNextCast(long value) { requireMutable(); debuffNextCastChanged = true; debuffNextCast = value; }
    public boolean hasDebuffNextCast() { return debuffNextCast != null; }
    public void clearDebuffNextCast() { requireMutable(); debuffNextCastChanged = true; debuffNextCast = null; }
    public long impact() { return impact == null ? 0L : impact; }
    public void impact(long value) { requireMutable(); impactChanged = true; impact = value; }
    public boolean hasImpact() { return impact != null; }
    public void clearImpact() { requireMutable(); impactChanged = true; impact = null; }
    public long phaseUntil() { return phaseUntil == null ? 0L : phaseUntil; }
    public void phaseUntil(long value) { requireMutable(); phaseUntilChanged = true; phaseUntil = value; }
    public boolean hasPhaseUntil() { return phaseUntil != null; }
    public void clearPhaseUntil() { requireMutable(); phaseUntilChanged = true; phaseUntil = null; }
    public long recoveryUntil() { return recoveryUntil == null ? 0L : recoveryUntil; }
    public void recoveryUntil(long value) { requireMutable(); recoveryUntilChanged = true; recoveryUntil = value; }
    public boolean hasRecoveryUntil() { return recoveryUntil != null; }
    public void clearRecoveryUntil() { requireMutable(); recoveryUntilChanged = true; recoveryUntil = null; }
    public boolean comboActive() { return comboActive == null ? false : comboActive; }
    public void comboActive(boolean value) { requireMutable(); comboActiveChanged = true; comboActive = value; }
    public boolean hasComboActive() { return comboActive != null; }
    public void clearComboActive() { requireMutable(); comboActiveChanged = true; comboActive = null; }
    public boolean finalUsed() { return finalUsed == null ? false : finalUsed; }
    public void finalUsed(boolean value) { requireMutable(); finalUsedChanged = true; finalUsed = value; }
    public boolean hasFinalUsed() { return finalUsed != null; }
    public void clearFinalUsed() { requireMutable(); finalUsedChanged = true; finalUsed = null; }
    public boolean restoring() { return restoring == null ? false : restoring; }
    public void restoring(boolean value) { requireMutable(); restoringChanged = true; restoring = value; }
    public boolean hasRestoring() { return restoring != null; }
    public void clearRestoring() { requireMutable(); restoringChanged = true; restoring = null; }
    public boolean reverse() { return reverse == null ? false : reverse; }
    public void reverse(boolean value) { requireMutable(); reverseChanged = true; reverse = value; }
    public boolean hasReverse() { return reverse != null; }
    public void clearReverse() { requireMutable(); reverseChanged = true; reverse = null; }
    public double initialHealth() { return initialHealth == null ? 0D : initialHealth; }
    public void initialHealth(double value) { requireMutable(); initialHealthChanged = true; initialHealth = value; }
    public boolean hasInitialHealth() { return initialHealth != null; }
    public void clearInitialHealth() { requireMutable(); initialHealthChanged = true; initialHealth = null; }
    public double originX() { return originX == null ? 0D : originX; }
    public void originX(double value) { requireMutable(); originXChanged = true; originX = value; }
    public boolean hasOriginX() { return originX != null; }
    public void clearOriginX() { requireMutable(); originXChanged = true; originX = null; }
    public double originZ() { return originZ == null ? 0D : originZ; }
    public void originZ(double value) { requireMutable(); originZChanged = true; originZ = value; }
    public boolean hasOriginZ() { return originZ != null; }
    public void clearOriginZ() { requireMutable(); originZChanged = true; originZ = null; }
    public double secondX() { return secondX == null ? 0D : secondX; }
    public void secondX(double value) { requireMutable(); secondXChanged = true; secondX = value; }
    public boolean hasSecondX() { return secondX != null; }
    public void clearSecondX() { requireMutable(); secondXChanged = true; secondX = null; }
    public double secondZ() { return secondZ == null ? 0D : secondZ; }
    public void secondZ(double value) { requireMutable(); secondZChanged = true; secondZ = value; }
    public boolean hasSecondZ() { return secondZ != null; }
    public void clearSecondZ() { requireMutable(); secondZChanged = true; secondZ = null; }
    public double targetX() { return targetX == null ? 0D : targetX; }
    public void targetX(double value) { requireMutable(); targetXChanged = true; targetX = value; }
    public boolean hasTargetX() { return targetX != null; }
    public void clearTargetX() { requireMutable(); targetXChanged = true; targetX = null; }
    public double targetZ() { return targetZ == null ? 0D : targetZ; }
    public void targetZ(double value) { requireMutable(); targetZChanged = true; targetZ = value; }
    public boolean hasTargetZ() { return targetZ != null; }
    public void clearTargetZ() { requireMutable(); targetZChanged = true; targetZ = null; }
    public List<UUID> mobs() { return mobs == null ? List.of() : mobs; }
    public void mobs(List<UUID> value) { requireMutable(); mobsChanged = true; mobs = List.copyOf(value); }
    public boolean hasMobs() { return mobs != null; }
    public void clearMobs() { requireMutable(); mobsChanged = true; mobs = null; }
    public List<UUID> bosses() { return bosses == null ? List.of() : bosses; }
    public void bosses(List<UUID> value) { requireMutable(); bossesChanged = true; bosses = List.copyOf(value); }
    public boolean hasBosses() { return bosses != null; }
    public void clearBosses() { requireMutable(); bossesChanged = true; bosses = null; }
    public Map<String, Long> cooldowns() { return cooldowns == null ? Map.of() : cooldowns; }
    public void cooldowns(Map<String, Long> value) { requireMutable(); cooldownsChanged = true; cooldowns = Collections.unmodifiableMap(new LinkedHashMap<>(value)); }
    public boolean hasCooldowns() { return cooldowns != null; }
    public void clearCooldowns() { requireMutable(); cooldownsChanged = true; cooldowns = null; }
    public Map<Long, EventTerrainChange> terrain() { return terrain == null ? Map.of() : terrain; }
    public void terrain(Map<Long, EventTerrainChange> value) { requireMutable(); terrainChanged = true; terrain = Collections.unmodifiableMap(new LinkedHashMap<>(value)); }
    public boolean hasTerrain() { return terrain != null; }
    public void clearTerrain() { requireMutable(); terrainChanged = true; terrain = null; }
    public Map<Long, EventTerrainChange> attackTerrain() { return attackTerrain == null ? Map.of() : attackTerrain; }
    public void attackTerrain(Map<Long, EventTerrainChange> value) { requireMutable(); attackTerrainChanged = true; attackTerrain = Collections.unmodifiableMap(new LinkedHashMap<>(value)); }
    public boolean hasAttackTerrain() { return attackTerrain != null; }
    public void clearAttackTerrain() { requireMutable(); attackTerrainChanged = true; attackTerrain = null; }
    public List<Long> attackBlocks() { return attackBlocks == null ? List.of() : attackBlocks; }
    public void attackBlocks(List<Long> value) { requireMutable(); attackBlocksChanged = true; attackBlocks = List.copyOf(value); }
    public boolean hasAttackBlocks() { return attackBlocks != null; }
    public void clearAttackBlocks() { requireMutable(); attackBlocksChanged = true; attackBlocks = null; }
    public List<Long> floorQueue() { return floorQueue == null ? List.of() : floorQueue; }
    public void floorQueue(List<Long> value) { requireMutable(); floorQueueChanged = true; floorQueue = List.copyOf(value); }
    public boolean hasFloorQueue() { return floorQueue != null; }
    public void clearFloorQueue() { requireMutable(); floorQueueChanged = true; floorQueue = null; }
    public List<String> comboQueue() { return comboQueue == null ? List.of() : comboQueue; }
    public void comboQueue(List<String> value) { requireMutable(); comboQueueChanged = true; comboQueue = List.copyOf(value); }
    public boolean hasComboQueue() { return comboQueue != null; }
    public void clearComboQueue() { requireMutable(); comboQueueChanged = true; comboQueue = null; }
}
