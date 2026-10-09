package com.siirio.jemworldbosstiers.network;

import com.siirio.jemworldbosstiers.client.TierAdvancementDescriptions;
import net.minecraft.resources.ResourceLocation;

import java.util.Set;

public final class ClientTierState {
    private static volatile int tier = 1;
    private static volatile int defeatCount;
    private static volatile int nextThreshold = -1;
    private static volatile Set<ResourceLocation> bossWeapons = Set.of();

    private ClientTierState() {
    }

    public static void clear() {
        tier = 1; defeatCount = 0; nextThreshold = -1;
        bossWeapons = Set.of();
    }

    public static int tier() {
        return tier;
    }

    public static int defeatCount() {
        return defeatCount;
    }

    public static int nextThreshold() {
        return nextThreshold;
    }

    public static boolean isBossWeapon(ResourceLocation itemId) {
        return bossWeapons.contains(itemId);
    }

    static void update(int value, int defeats, int threshold, Set<ResourceLocation> weaponIds) {
        tier = Math.max(1, Math.min(5, value));
        defeatCount = Math.max(0, defeats);
        nextThreshold = threshold;
        bossWeapons = Set.copyOf(weaponIds);
        TierAdvancementDescriptions.refresh(defeatCount, nextThreshold);
    }
}
