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
    private boolean needsEnrolled;
    private String displayName;
    public boolean needsEnrolled() { return needsEnrolled; }
    public void enrollNeeds() { needsEnrolled = true; }
    public String displayName() { return displayName; }
    public void observeName(net.minecraft.world.entity.Entity entity) { displayName = net.minecraft.network.chat.Component.Serializer.toJson(entity.getName()); }
    private long lastSeenGameTime;
    private final PayrollStatus coinPayroll = new PayrollStatus();
    private final PayrollStatus emeraldPayroll = new PayrollStatus();
    private PayrollStatus payrollStatus() { return com.siirio.jemsettlements.currency.SettlementCurrency.coins() ? coinPayroll : emeraldPayroll; }
    private static final class PayrollStatus {
        long lastPaidPeriod = -1L, lastPenaltyPeriod = -1L;
        int missedPayrolls;
        final java.util.Set<Long> unpaidPenaltyPeriods = new java.util.LinkedHashSet<>();
        boolean penaltyPeriodsKnown = true;
        void reset() { lastPaidPeriod = -1L; lastPenaltyPeriod = -1L; missedPayrolls = 0; unpaidPenaltyPeriods.clear(); penaltyPeriodsKnown = true; }
        CompoundTag save() {
            CompoundTag tag = new CompoundTag(); tag.putLong("LastPaidPeriod", lastPaidPeriod); tag.putLong("LastPenaltyPeriod", lastPenaltyPeriod); tag.putInt("MissedPayrolls", missedPayrolls);
            if (penaltyPeriodsKnown) tag.putLongArray("UnpaidPenaltyPeriods", unpaidPenaltyPeriods.stream().mapToLong(Long::longValue).toArray()); return tag;
        }
        void load(CompoundTag tag) {
            lastPaidPeriod = tag.contains("LastPaidPeriod") ? tag.getLong("LastPaidPeriod") : -1L;
            lastPenaltyPeriod = tag.contains("LastPenaltyPeriod") ? tag.getLong("LastPenaltyPeriod") : -1L;
            missedPayrolls = Math.max(0, Math.min(3, tag.getInt("MissedPayrolls"))); penaltyPeriodsKnown = tag.contains("UnpaidPenaltyPeriods");
            unpaidPenaltyPeriods.clear(); for (long period : tag.getLongArray("UnpaidPenaltyPeriods")) unpaidPenaltyPeriods.add(period);
        }
    }
    private com.siirio.jemsettlements.resident.ResidentMood foodHistory;
    public com.siirio.jemsettlements.resident.ResidentMood foodHistory() {
        if (foodHistory == null) foodHistory = new com.siirio.jemsettlements.resident.ResidentMood(entityId, townId, null, null);
        return foodHistory;
    }
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
    public void assignTown(UUID value) { townId = java.util.Objects.requireNonNull(value); needsEnrolled = false; hireGeneration++; coinPayroll.reset(); emeraldPayroll.reset(); }
    public long nextWageGameTime() { return nextWageGameTime; }
    public long hireGeneration() { return hireGeneration; }
    public void scheduleWage(long due) { nextWageGameTime = com.siirio.jemsettlements.payroll.PayrollClock.nextDue(due); }
    public void migrateWageClock(long latestDue) { nextWageGameTime = Math.max(com.siirio.jemsettlements.payroll.PayrollClock.firstDue(enlistedGameTime), com.siirio.jemsettlements.payroll.PayrollClock.nextDue(latestDue)); }
    public UUID ownerId() { return ownerId; }
    public void employer(UUID value) { ownerId = java.util.Objects.requireNonNull(value); needsEnrolled = false; }
    public void dismissed() { coinWageEnabled = false; }
    public WorkforceType type() { return type; }
    public String nativeModId() { return nativeModId; }
    public ResourceLocation nativeEntityType() { return nativeEntityType; }
    /** Removed Workers assignments remain readable history, never active payroll. */
    public boolean coinWageEnabled() { return nativeModId.equals("recruits") && type == WorkforceType.RECRUIT && coinWageEnabled; }
    public boolean living() { return living; }
    public long enlistedGameTime() { return enlistedGameTime; }
    public long lastSeenGameTime() { return lastSeenGameTime; }
    public long lastPaidPeriod() { return payrollStatus().lastPaidPeriod; }
    public long lastPenaltyPeriod() { return payrollStatus().lastPenaltyPeriod; }
    public int missedPayrolls() { return payrollStatus().missedPayrolls; }
    public Loyalty loyalty() { return loyalty; }
    public boolean settlementActive() { return living && loyalty == Loyalty.LOYAL && coinWageEnabled(); }
    public UUID lastWorkAreaId() { return lastWorkAreaId; }
    public long lastWorkEvidenceGameTime() { return lastWorkEvidenceGameTime; }
    public void seen(long gameTime) { living = true; lastSeenGameTime = Math.max(lastSeenGameTime, gameTime); }
    public void verifyNativeEntityType(ResourceLocation value, long gameTime) { if (value != null && value.getNamespace().equals(nativeModId)) nativeEntityType = value; seen(gameTime); }
    public void markDead() { living = false; }
    public void paid(long period) {
        var state = payrollStatus(); state.lastPaidPeriod = Math.max(state.lastPaidPeriod, period);
        if (state.unpaidPenaltyPeriods.remove(period) && loyalty == Loyalty.LOYAL) state.missedPayrolls = Math.min(3, state.unpaidPenaltyPeriods.size());
    }
    public void initializePenaltyPeriods(java.util.Collection<Long> periods) {
        var state = payrollStatus(); if (state.penaltyPeriodsKnown) return;
        for (long period : periods) if (period <= state.lastPenaltyPeriod) state.unpaidPenaltyPeriods.add(period);
        if (loyalty == Loyalty.LOYAL) state.missedPayrolls = Math.min(3, state.unpaidPenaltyPeriods.size()); state.penaltyPeriodsKnown = true;
    }
    public void penalized(long period) {
        var state = payrollStatus(); if (period <= state.lastPenaltyPeriod) return; state.lastPenaltyPeriod = period;
        if (loyalty == Loyalty.LOYAL) { state.unpaidPenaltyPeriods.add(period); state.missedPayrolls = Math.min(3, state.unpaidPenaltyPeriods.size()); }
    }
    public void rebel() { if (loyalty == Loyalty.LOYAL) loyalty = Loyalty.REBELLIOUS; }
    public void defeated() { if (loyalty == Loyalty.REBELLIOUS) loyalty = Loyalty.DEFEATED; }
    public void rehired(long now) { loyalty = Loyalty.LOYAL; coinWageEnabled = true; living = true; hireGeneration++; coinPayroll.reset(); emeraldPayroll.reset(); nextWageGameTime = com.siirio.jemsettlements.payroll.PayrollClock.firstDue(now); }
    public void observeWorkArea(UUID workAreaId, long gameTime) {
        if (workAreaId == null) return;
        lastWorkAreaId = workAreaId;
        lastWorkEvidenceGameTime = Math.max(lastWorkEvidenceGameTime, gameTime);
        seen(gameTime);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag(); tag.putUUID("Entity", entityId); tag.putUUID("Town", townId); tag.putUUID("Owner", ownerId);
        tag.putString("Type", type.name()); tag.putString("NativeMod", nativeModId); tag.putString("NativeEntityType", nativeEntityType.toString());
        tag.putBoolean("NeedsEnrolled", needsEnrolled);
        if (displayName != null) tag.putString("DisplayName", displayName);
        tag.putBoolean("CoinWage", coinWageEnabled); tag.putBoolean("Living", living); tag.putLong("EnlistedGameTime", enlistedGameTime);
        tag.putLong("LastSeenGameTime", lastSeenGameTime);
        tag.put("CoinPayroll", coinPayroll.save()); tag.put("EmeraldPayroll", emeraldPayroll.save());
        tag.putLong("NextWageGameTime", nextWageGameTime); tag.putLong("HireGeneration", hireGeneration); tag.putString("Loyalty", loyalty.name());
        if (lastWorkAreaId != null) tag.putUUID("LastWorkArea", lastWorkAreaId);
        tag.putLong("LastWorkEvidenceGameTime", lastWorkEvidenceGameTime);
        if (foodHistory != null) {
            CompoundTag history = foodHistory.save();
            history.remove("Mood"); history.remove("Satiety"); // Native entity NBT is the sole needs owner.
            tag.put("FoodHistory", history);
        }
        return tag;
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
        record.needsEnrolled = tag.getBoolean("NeedsEnrolled");
        record.displayName = tag.contains("DisplayName") ? tag.getString("DisplayName") : null;
        record.living = !tag.contains("Living") || tag.getBoolean("Living"); record.lastSeenGameTime = tag.getLong("LastSeenGameTime");
        // Untagged historical penalty state belongs exclusively to Coins.
        record.coinPayroll.load(tag.contains("CoinPayroll") ? tag.getCompound("CoinPayroll") : tag);
        if (tag.contains("EmeraldPayroll")) record.emeraldPayroll.load(tag.getCompound("EmeraldPayroll"));
        try { record.loyalty = Loyalty.valueOf(tag.getString("Loyalty")); } catch (IllegalArgumentException ignored) { }
        if (tag.contains("FoodHistory")) record.foodHistory = com.siirio.jemsettlements.resident.ResidentMood.load(tag.getCompound("FoodHistory"));
        record.lastWorkAreaId = tag.hasUUID("LastWorkArea") ? tag.getUUID("LastWorkArea") : null;
        record.lastWorkEvidenceGameTime = tag.contains("LastWorkEvidenceGameTime") ? tag.getLong("LastWorkEvidenceGameTime") : Long.MIN_VALUE; return record;
    }
}
