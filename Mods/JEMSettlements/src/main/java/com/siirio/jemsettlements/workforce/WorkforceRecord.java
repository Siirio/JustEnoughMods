package com.siirio.jemsettlements.workforce;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

/** One native NPC assignment. Defense is a filtered view of this record. */
public final class WorkforceRecord {
    private final UUID entityId;
    private final UUID townId;
    private final UUID ownerId;
    private final WorkforceType type;
    private final String nativeModId;
    private ResourceLocation nativeEntityType;
    private final long enlistedGameTime;
    private boolean coinWageEnabled;
    private boolean living = true;
    private long lastSeenGameTime;
    private long lastPaidPeriod = -1L;
    private long lastPenaltyPeriod = -1L;
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
    }
    public UUID entityId() { return entityId; }
    public UUID townId() { return townId; }
    public UUID ownerId() { return ownerId; }
    public WorkforceType type() { return type; }
    public String nativeModId() { return nativeModId; }
    public ResourceLocation nativeEntityType() { return nativeEntityType; }
    public boolean coinWageEnabled() { return coinWageEnabled; }
    public boolean living() { return living; }
    public long enlistedGameTime() { return enlistedGameTime; }
    public long lastSeenGameTime() { return lastSeenGameTime; }
    public long lastPaidPeriod() { return lastPaidPeriod; }
    public long lastPenaltyPeriod() { return lastPenaltyPeriod; }
    public UUID lastWorkAreaId() { return lastWorkAreaId; }
    public long lastWorkEvidenceGameTime() { return lastWorkEvidenceGameTime; }
    public void seen(long gameTime) { living = true; lastSeenGameTime = Math.max(lastSeenGameTime, gameTime); }
    public void verifyNativeEntityType(ResourceLocation value, long gameTime) { if (value != null && value.getNamespace().equals(nativeModId)) nativeEntityType = value; seen(gameTime); }
    public void markDead() { living = false; }
    public void paid(long period) { lastPaidPeriod = Math.max(lastPaidPeriod, period); }
    public void penalized(long period) { lastPenaltyPeriod = Math.max(lastPenaltyPeriod, period); }
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
        if (lastWorkAreaId != null) tag.putUUID("LastWorkArea", lastWorkAreaId);
        tag.putLong("LastWorkEvidenceGameTime", lastWorkEvidenceGameTime); return tag;
    }
    public static WorkforceRecord load(CompoundTag tag) {
        String nativeMod = tag.getString("NativeMod"); ResourceLocation nativeType = ResourceLocation.tryParse(tag.getString("NativeEntityType"));
        if (nativeType == null) nativeType = new ResourceLocation(nativeMod.equals("workers") ? "workers" : "recruits",
                tag.getString("Type").equals("RECRUIT") ? "unverified_legacy" : tag.getString("Type").toLowerCase(java.util.Locale.ROOT));
        boolean coinWage = nativeMod.equals("recruits") || tag.getBoolean("CoinWage");
        WorkforceRecord record = new WorkforceRecord(tag.getUUID("Entity"), tag.getUUID("Town"), tag.getUUID("Owner"),
                WorkforceType.valueOf(tag.getString("Type")), nativeMod, nativeType, coinWage, tag.contains("EnlistedGameTime") ? tag.getLong("EnlistedGameTime") : 0L);
        record.living = !tag.contains("Living") || tag.getBoolean("Living"); record.lastSeenGameTime = tag.getLong("LastSeenGameTime");
        record.lastPaidPeriod = tag.contains("LastPaidPeriod") ? tag.getLong("LastPaidPeriod") : -1L;
        record.lastPenaltyPeriod = tag.contains("LastPenaltyPeriod") ? tag.getLong("LastPenaltyPeriod") : -1L;
        record.lastWorkAreaId = tag.hasUUID("LastWorkArea") ? tag.getUUID("LastWorkArea") : null;
        record.lastWorkEvidenceGameTime = tag.contains("LastWorkEvidenceGameTime") ? tag.getLong("LastWorkEvidenceGameTime") : Long.MIN_VALUE; return record;
    }
}
