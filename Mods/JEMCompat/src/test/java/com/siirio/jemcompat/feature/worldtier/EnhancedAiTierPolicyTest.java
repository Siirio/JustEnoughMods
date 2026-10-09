package com.siirio.jemcompat.feature.worldtier;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnhancedAiTierPolicyTest {
    @Test
    void tierOneDisablesEveryEnhancedAiFeature() {
        assertFalse(EnhancedAiTierPolicy.enabledAt("Targeting", 1));
        assertFalse(EnhancedAiTierPolicy.enabledAt("Sprint", 1));
    }

    @Test
    void stagesEnhancedAiFeaturesAcrossWorldTiers() {
        assertTrue(EnhancedAiTierPolicy.enabledAt("Targeting", 2));
        assertTrue(EnhancedAiTierPolicy.enabledAt("Climbing", 3));
        assertTrue(EnhancedAiTierPolicy.enabledAt("MinerMobs", 3));
        assertTrue(EnhancedAiTierPolicy.enabledAt("ItemDisruption", 3));
        assertTrue(EnhancedAiTierPolicy.enabledAt("Pathfinding", 3));
        assertTrue(EnhancedAiTierPolicy.enabledAt("MeleeAttacking", 3));
        assertTrue(EnhancedAiTierPolicy.enabledAt("RandomStroll", 3));
        assertTrue(EnhancedAiTierPolicy.enabledAt("PearlerMobs", 3));
        assertTrue(EnhancedAiTierPolicy.enabledAt("Leaders", 4));
        assertTrue(EnhancedAiTierPolicy.enabledAt("CreeperLaunch", 5));
    }

    @Test
    void excludesScriptedBossNamespacesFromGlobalMixins() {
        assertTrue(EnhancedAiTierPolicy.excludedNamespace("cataclysm"));
        assertTrue(EnhancedAiTierPolicy.excludedNamespace("luminous_beasts"));
        assertTrue(EnhancedAiTierPolicy.excludedNamespace("alexsmobs"));
        assertFalse(EnhancedAiTierPolicy.excludedNamespace("minecraft"));
    }

    @Test
    void managesOnlyEnhancedAiFeatures() {
        assertTrue(EnhancedAiTierPolicy.managesModule("enhancedai"));
        assertFalse(EnhancedAiTierPolicy.managesModule("insanelib"));
        assertFalse(EnhancedAiTierPolicy.managesModule("scalinghealth"));
    }
}
