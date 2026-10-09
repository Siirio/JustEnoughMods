package com.siirio.jemworldbosstiers.balance;

public final class EquipmentRefreshPolicy {
    private EquipmentRefreshPolicy() {
    }

    public static boolean required(int previousTier, int currentTier) {
        return previousTier != currentTier;
    }
}
