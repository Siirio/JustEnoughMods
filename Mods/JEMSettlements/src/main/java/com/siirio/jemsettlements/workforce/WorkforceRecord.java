package com.siirio.jemsettlements.workforce;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;

public record WorkforceRecord(UUID entityId, UUID townId, UUID ownerId, WorkforceType type,
                              String nativeModId, boolean coinWageEnabled) {
    public WorkforceRecord {
        if (entityId == null || townId == null || ownerId == null || type == null || nativeModId == null) {
            throw new IllegalArgumentException("Workforce identity is incomplete");
        }
        if (!nativeModId.equals("workers") && !nativeModId.equals("recruits")) {
            throw new IllegalArgumentException("Unsupported native workforce owner " + nativeModId);
        }
        if (type == WorkforceType.RECRUIT && !nativeModId.equals("recruits")) {
            throw new IllegalArgumentException("Recruit workforce entries must be native Recruits entities");
        }
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("Entity", entityId);
        tag.putUUID("Town", townId);
        tag.putUUID("Owner", ownerId);
        tag.putString("Type", type.name());
        tag.putString("NativeMod", nativeModId);
        tag.putBoolean("CoinWage", coinWageEnabled);
        return tag;
    }

    public static WorkforceRecord load(CompoundTag tag) {
        return new WorkforceRecord(tag.getUUID("Entity"), tag.getUUID("Town"), tag.getUUID("Owner"),
                WorkforceType.valueOf(tag.getString("Type")), tag.getString("NativeMod"), tag.getBoolean("CoinWage"));
    }
}
