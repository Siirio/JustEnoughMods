package com.siirio.jemcompat.feature.worldtier;

import java.util.Map;
import java.util.Set;

public final class EnhancedAiTierPolicy {
    private static final String MODULE_NAMESPACE = "enhancedai";
    private static final Map<String, Integer> MINIMUM_TIERS = Map.ofEntries(
            Map.entry("Targeting", 2),
            Map.entry("Sprint", 2),
            Map.entry("StuckFix", 2),
            Map.entry("SkeletonShoot", 2),
            Map.entry("PillagerShoot", 2),
            Map.entry("BlazeAttack", 3),
            Map.entry("GhastShooting", 3),
            Map.entry("ShulkerAttack", 3),
            Map.entry("Jump", 3),
            Map.entry("Climbing", 3),
            Map.entry("AvoidExplosions", 3),
            Map.entry("DrowningTargets", 3),
            Map.entry("ThrowingWeb", 3),
            Map.entry("MinerMobs", 3),
            Map.entry("ItemDisruption", 3),
            Map.entry("Pathfinding", 3),
            Map.entry("MeleeAttacking", 3),
            Map.entry("RandomStroll", 3),
            Map.entry("PearlerMobs", 3),
            Map.entry("Leaders", 4),
            Map.entry("OpenDoors", 4),
            Map.entry("Swimmers", 4),
            Map.entry("TeleportAntiCheese", 4),
            Map.entry("VehicleAntiCheese", 4),
            Map.entry("CreeperSwell", 5),
            Map.entry("CreeperLaunch", 5),
            Map.entry("ShulkerBullets", 5),
            Map.entry("WitchPotionThrowing", 5),
            Map.entry("AlliedMonsters", 5)
    );
    private static final Set<String> EXCLUDED_NAMESPACES = Set.of(
            "cataclysm",
            "block_factorys_bosses",
            "legendary_monsters",
            "aquamirae",
            "luminous_beasts",
            "luminous_nether",
            "alexscaves",
            "alexsmobs",
            "companions",
            "deep_dark_regrowth",
            "unearthed_journey"
    );

    private EnhancedAiTierPolicy() {
    }

    public static boolean enabledAt(String featureName, int tier) {
        Integer minimumTier = MINIMUM_TIERS.get(featureName);
        return minimumTier != null && tier >= minimumTier;
    }

    public static boolean excludedNamespace(String namespace) {
        return EXCLUDED_NAMESPACES.contains(namespace);
    }

    public static boolean managesModule(String namespace) {
        return MODULE_NAMESPACE.equals(namespace);
    }
}
