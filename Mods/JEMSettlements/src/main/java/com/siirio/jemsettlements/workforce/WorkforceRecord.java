package com.siirio.jemsettlements.workforce;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

/** One native NPC assignment. Defense is a filtered view of this record. */
public final class WorkforceRecord {
    public enum Loyalty { LOYAL, REBELLIOUS, DEFEATED }
    private final UUID entityId;
    private UUID townId;
    private UUID ownerId;
    private final WorkforceType type;
    private final String nativeModId;
    private ResourceLocation nativeEntityType;
    private final long enlistedGameTime;
    private long nextWageGameTime;
    private long hireGeneration;
    private boolean coinWageEnabled;
    private boolean living = true;
    private long lastSeenGameTime;
    private long lastPaidPeriod = -1L;
    private long lastPenaltyPeriod = -1L;
    private int missedPayrolls;
    private final java.util.Set<Long> unpaidPenaltyPeriods = new java.util.LinkedHashSet<>();
    private boolean penaltyPeriodsKnown = true;
    private Loyalty loyalty = Loyalty.LOYAL;
    private UUID lastWorkAreaId;
    private long lastWorkEvidenceGameTime = Long.MIN_VALUE;

    public WorkforceRecord(UUID entityId, UUID townId, UUID ownerId, WorkforceType type, String nativeModId,
                           ResourceLocation nativeEntityType, boolean coinWageEnabled, long enlistedGameTime) {
        if (entityId == null || townId == null || ownerId == null || type == null || nativeModId == null || nativeEntityType == null) {
            throw new IllegalArgumentException("Workforce identity is incomplete");
        }
        if (!nativeModId.equals("workers") && !nativeModId.equals("recruits")) throw new IllegalArgumentException("Unsupported native workforce owner " + nativeModId);
        if (type == WorkforceType.RECRUIT && !nativeModId.equals("recruits")) throw new IllegalArgumentException("Recruit workforce entries must be native Recruits entities");
        this.entityId = entityId; this.townId = townId; this.ownerId = ownerId; this.type = type; this.nativeModId = nativeModId;
        this.nativeEntityType = nativeEntityType; this.coinWageEnabled = coinWageEnabled; this.enlistedGameTime = Math.max(0L, enlistedGameTime);
        this.lastSeenGameTime = this.enlistedGameTime;
        this.nextWageGameTime = com.siirio.jemsettlements.payroll.PayrollClock.firstDue(this.enlistedGameTime);
    }
    public UUID entityId() { return entityId; }
    public UUID townId() { return townId; }
    public void assignTown(UUID value) { townId = java.util.Objects.requireNonNull(value); hireGeneration++; lastPaidPeriod = -1L; lastPenaltyPeriod = -1L; unpaidPenaltyPeriods.clear(); penaltyPeriodsKnown = true; }
    public long nextWageGameTime() { return nextWageGameTime; }
    public long hireGeneration() { return hireGeneration; }
    public void scheduleWage(long due) { nextWageGameTime = com.siirio.jemsettlements.payroll.PayrollClock.nextDue(due); }
    public void migrateWageClock(long latestDue) { nextWageGameTime = Math.max(com.siirio.jemsettlements.payroll.PayrollClock.firstDue(enlistedGameTime), com.siirio.jemsettlements.payroll.PayrollClock.nextDue(latestDue)); }
    public UUID ownerId() { return ownerId; }
    public void employer(UUID value) { ownerId = java.util.Objects.requireNonNull(value); }
    public void dismissed() { coinWageEnabled = false; }
    public WorkforceType type() { return type; }
    public String nativeModId() { return nativeModId; }
    public ResourceLocation nativeEntityType() { return nativeEntityType; }
    public boolean coinWageEnabled() { return coinWageEnabled; }
    public boolean living() { return living; }
    public long enlistedGameTime() { return enlistedGameTime; }
    public long lastSeenGameTime() { return lastSeenGameTime; }
    public long lastPaidPeriod() { return lastPaidPeriod; }
    public long lastPenaltyPeriod() { return lastPenaltyPeriod; }
    public int missedPayrolls() { return missedPayrolls; }
    public Loyalty loyalty() { return loyalty; }
    public boolean settlementActive() { return living && loyalty == Loyalty.LOYAL && (type != WorkforceType.RECRUIT || coinWageEnabled); }
    public UUID lastWorkAreaId() { return lastWorkAreaId; }
    public long lastWorkEvidenceGameTime() { return lastWorkEvidenceGameTime; }
    public void seen(long gameTime) { living = true; lastSeenGameTime = Math.max(lastSeenGameTime, gameTime); }
    public void verifyNativeEntityType(ResourceLocation value, long gameTime) { if (value != null && value.getNamespace().equals(nativeModId)) nativeEntityType = value; seen(gameTime); }
    public void markDead() { living = false; }
    public void paid(long period) {
        lastPaidPeriod = Math.max(lastPaidPeriod, period);
        if (unpaidPenaltyPeriods.remove(period) && loyalty == Loyalty.LOYAL) missedPayrolls = Math.min(3, unpaidPenaltyPeriods.size());
    }
    public void initializePenaltyPeriods(java.util.Collection<Long> periods) {
        if (penaltyPeriodsKnown) return;
        for (long period : periods) if (period <= lastPenaltyPeriod) unpaidPenaltyPeriods.add(period);
        if (loyalty == Loyalty.LOYAL) missedPayrolls = Math.min(3, unpaidPenaltyPeriods.size());
        penaltyPeriodsKnown = true;
    }
    public void penalized(long period) { if (period <= lastPenaltyPeriod) return; lastPenaltyPeriod = period; if (loyalty == Loyalty.LOYAL) { unpaidPenaltyPeriods.add(period); missedPayrolls = Math.min(3, unpaidPenaltyPeriods.size()); } }
    public void rebel() { if (loyalty == Loyalty.LOYAL) loyalty = Loyalty.REBELLIOUS; }
    public void defeated() { if (loyalty == Loyalty.REBELLIOUS) loyalty = Loyalty.DEFEATED; }
    public void rehired(long now) { loyalty = Loyalty.LOYAL; coinWageEnabled = true; missedPayrolls = 0; living = true; hireGeneration++; lastPaidPeriod = -1L; lastPenaltyPeriod = -1L; unpaidPenaltyPeriods.clear(); penaltyPeriodsKnown = true; nextWageGameTime = com.siirio.jemsettlements.payroll.PayrollClock.firstDue(now); }
    public void observeWorkArea(UUID workAreaId, long gameTime) {
        if (workAreaId == null) return;
        lastWorkAreaId = workAreaId;
        lastWorkEvidenceGameTime = Math.max(lastWorkEvidenceGameTime, gameTime);
        seen(gameTime);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag(); tag.putUUID("Entity", entityId); tag.putUUID("Town", townId); tag.putUUID("Owner", ownerId);
        tag.putString("Type", type.name()); tag.putString("NativeMod", nativeModId); tag.putString("NativeEntityType", nativeEntityType.toString());
        tag.putBoolean("CoinWage", coinWageEnabled); tag.putBoolean("Living", living); tag.putLong("EnlistedGameTime", enlistedGameTime);
        tag.putLong("LastSeenGameTime", lastSeenGameTime); tag.putLong("LastPaidPeriod", lastPaidPeriod); tag.putLong("LastPenaltyPeriod", lastPenaltyPeriod);
        tag.putLong("NextWageGameTime", nextWageGameTime); tag.putLong("HireGeneration", hireGeneration); tag.putInt("MissedPayrolls", missedPayrolls); if (penaltyPeriodsKnown) tag.putLongArray("UnpaidPenaltyPeriods", unpaidPenaltyPeriods.stream().mapToLong(Long::longValue).toArray()); tag.putString("Loyalty", loyalty.name());
        if (lastWorkAreaId != null) tag.putUUID("LastWorkArea", lastWorkAreaId);
        tag.putLong("LastWorkEvidenceGameTime", lastWorkEvidenceGameTime); return tag;
    }
    public static WorkforceRecord load(CompoundTag tag) {
        String nativeMod = tag.getString("NativeMod"); ResourceLocation nativeType = ResourceLocation.tryParse(tag.getString("NativeEntityType"));
        if (nativeType == null) nativeType = new ResourceLocation(nativeMod.equals("workers") ? "workers" : "recruits",
                tag.getString("Type").equals("RECRUIT") ? "unverified_legacy" : tag.getString("Type").toLowerCase(java.util.Locale.ROOT));
        boolean coinWage = tag.contains("CoinWage") ? tag.getBoolean("CoinWage") : nativeMod.equals("recruits");
        WorkforceRecord record = new WorkforceRecord(tag.getUUID("Entity"), tag.getUUID("Town"), tag.getUUID("Owner"),
                WorkforceType.valueOf(tag.getString("Type")), nativeMod, nativeType, coinWage, tag.contains("EnlistedGameTime") ? tag.getLong("EnlistedGameTime") : 0L);
        record.nextWageGameTime = tag.contains("NextWageGameTime") ? tag.getLong("NextWageGameTime") : 0L;
        record.hireGeneration = tag.getLong("HireGeneration");
        record.living = !tag.contains("Living") || tag.getBoolean("Living"); record.lastSeenGameTime = tag.getLong("LastSeenGameTime");
        record.lastPaidPeriod = tag.contains("LastPaidPeriod") ? tag.getLong("LastPaidPeriod") : -1L;
        record.lastPenaltyPeriod = tag.contains("LastPenaltyPeriod") ? tag.getLong("LastPenaltyPeriod") : -1L;
        record.penaltyPeriodsKnown = tag.contains("UnpaidPenaltyPeriods");
        for (long period : tag.getLongArray("UnpaidPenaltyPeriods")) record.unpaidPenaltyPeriods.add(period);
        record.missedPayrolls = Math.max(0, Math.min(3, tag.getInt("MissedPayrolls")));
        try { record.loyalty = Loyalty.valueOf(tag.getString("Loyalty")); } catch (IllegalArgumentException ignored) { }
        record.lastWorkAreaId = tag.hasUUID("LastWorkArea") ? tag.getUUID("LastWorkArea") : null;
        record.lastWorkEvidenceGameTime = tag.contains("LastWorkEvidenceGameTime") ? tag.getLong("LastWorkEvidenceGameTime") : Long.MIN_VALUE; return record;
    }
}
