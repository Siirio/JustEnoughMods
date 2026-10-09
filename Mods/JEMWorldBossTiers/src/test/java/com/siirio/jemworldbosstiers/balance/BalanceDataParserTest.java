package com.siirio.jemworldbosstiers.balance;

import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BalanceDataParserTest {
    @Test
    void luminousPrerequisiteVictoriesResolveToTheirExistingUniqueBossKeys() throws Exception {
        java.util.Map<String, String> victories = java.util.Map.of(
                "mummy", "kill_mummy",
                "golden_hermit_king", "kill_hermit_king",
                "bone_stalker", "kill_bone_stalker",
                "sea_viper", "kill_sea_viper",
                "yeti", "kill_yeti",
                "piglin_executioner", "kill_piglin_executioner",
                "the_furnace", "kill_the_furnace"
        );
        try (InputStreamReader input = new InputStreamReader(getClass().getClassLoader().getResourceAsStream(
                "data/jem_world_boss_tiers/jem_world_boss_tiers/bosses/expanded_bosses.json"), StandardCharsets.UTF_8)) {
            java.util.List<BossProfile> profiles = BalanceDataParser.parseBosses(JsonParser.parseReader(input).getAsJsonObject()).profiles();
            victories.forEach((boss, victory) -> {
                ResourceLocation key = ResourceLocation.fromNamespaceAndPath("luminous_beasts", boss);
                ResourceLocation advancement = ResourceLocation.fromNamespaceAndPath("luminous_beasts", victory);
                java.util.List<BossProfile> matches = profiles.stream()
                        .filter(profile -> profile.victoryAdvancementIds().contains(advancement)).toList();
                assertEquals(1, matches.size());
                assertEquals(key, matches.get(0).key());
                assertEquals(java.util.List.of(key), matches.get(0).entityIds());
                assertTrue(matches.get(0).countsTowardWorldTier());
            });
        }
    }

    @Test
    void productionDataContainsTheApprovedRosterAndRewardSet() throws Exception {
        ClassLoader loader = getClass().getClassLoader();
        try (InputStreamReader bosses = new InputStreamReader(loader.getResourceAsStream("data/jem_world_boss_tiers/jem_world_boss_tiers/bosses/major_bosses.json"), StandardCharsets.UTF_8);
             InputStreamReader expandedBosses = new InputStreamReader(loader.getResourceAsStream("data/jem_world_boss_tiers/jem_world_boss_tiers/bosses/expanded_bosses.json"), StandardCharsets.UTF_8);
             InputStreamReader rewards = new InputStreamReader(loader.getResourceAsStream("data/jem_world_boss_tiers/jem_world_boss_tiers/rewards/major_rewards.json"), StandardCharsets.UTF_8)) {
            ParsedBossData bossData = BalanceDataParser.parseBosses(JsonParser.parseReader(bosses).getAsJsonObject());
            ParsedBossData expandedBossData = BalanceDataParser.parseBosses(JsonParser.parseReader(expandedBosses).getAsJsonObject());
            java.util.List<RewardProfile> rewardData = BalanceDataParser.parseRewards(JsonParser.parseReader(rewards).getAsJsonObject());
            java.util.List<BossProfile> profiles = new java.util.ArrayList<>(bossData.profiles());
            profiles.addAll(expandedBossData.profiles());

            assertEquals(56, profiles.size());
            assertEquals(56, profiles.stream().filter(BossProfile::countsTowardWorldTier).count());
            assertEquals(5, profiles.stream().filter(profile -> profile.key().getNamespace().equals("block_factorys_bosses")).count());
            assertEquals(11, profiles.stream().filter(profile -> profile.key().getNamespace().equals("cataclysm")).count());
            assertEquals(27, profiles.stream().filter(profile -> profile.key().getNamespace().equals("legendary_monsters")).count());
            assertEquals(java.util.List.of(0, 3, 10, 18, 32), bossData.thresholds());
            assertEquals(42, rewardData.size());
        }
    }

    @Test
    void expandedRewardsHaveRealStatContributionsAndIncludeTheGreatFrost() throws Exception {
        try (InputStreamReader input = new InputStreamReader(getClass().getClassLoader().getResourceAsStream(
                "data/jem_world_boss_tiers/jem_world_boss_tiers/rewards/expanded_rewards.json"), StandardCharsets.UTF_8)) {
            var profiles = BalanceDataParser.parseRewards(JsonParser.parseReader(input).getAsJsonObject());
            assertEquals(26, profiles.size());
            assertEquals(profiles.size(), profiles.stream().map(RewardProfile::itemId).distinct().count());
            assertTrue(profiles.stream().allMatch(profile -> !profile.attributeContributions().isEmpty()));
            var frost = profiles.stream().filter(profile -> profile.itemId().getPath().equals("the_great_frost")).findFirst().orElseThrow();
            assertEquals("legendary_monsters:frostbitten_golem", frost.sourceBoss().toString());
            assertEquals(1.5, frost.attributeContributions().get(ResourceLocation.fromNamespaceAndPath("minecraft", "generic.attack_damage")).valueAt(5));
        }
    }

    @Test
    void parsesExplicitBossTiersAndIndependentProgressionFlags() {
        String json = """
                {
                  "tier_thresholds": [0, 3, 6, 10, 15],
                  "bosses": [{
                    "key": "test:phoenix",
                    "display_name": "Phoenix",
                    "entity_ids": ["test:phoenix"],
                    "victory_advancement_ids": [],
                    "scales_with_world_tier": true,
                    "counts_toward_world_tier": false,
                    "native_threat": 7.0,
                    "hp_scaling_safety": "SAFE",
                    "attributes": {"minecraft:generic.max_health": [100, 120, 150, 220, 280]},
                    "special_damage_multiplier": [0.8, 1.0, 1.1, 1.2, 1.3],
                    "damage_adapter": "standard",
                    "healing_adapter": "none",
                    "stagger_adapter": "none",
                    "arena_strategy": "native_anchor",
                    "revival_strategy": "native_egg",
                    "anti_cheese_profile": "mobile",
                    "destruction_rules": "arena_safe",
                    "arena_radius": 80,
                    "revival_offering": "minecraft:nether_star",
                    "revival_xp_levels": 20
                  }]
                }
                """;

        ParsedBossData parsed = BalanceDataParser.parseBosses(JsonParser.parseString(json).getAsJsonObject());
        BossProfile profile = parsed.profiles().get(0);

        assertEquals(java.util.List.of(0, 3, 6, 10, 15), parsed.thresholds());
        assertTrue(profile.scalesWithWorldTier());
        assertFalse(profile.countsTowardWorldTier());
        assertEquals(220.0D, profile.attributes().get(ResourceLocation.fromNamespaceAndPath("minecraft", "generic.max_health")).valueAt(4));
        assertEquals("native_egg", profile.revivalStrategy());
        assertEquals(80, profile.arenaRadius());
        assertEquals(ResourceLocation.fromNamespaceAndPath("minecraft", "nether_star"), profile.revivalOffering());
    }

    @Test
    void parsesSignedCurrentTierRewardContributions() {
        String json = """
                {"rewards": [{
                  "item_id": "test:sword",
                  "source_boss": "test:phoenix",
                  "category": "weapon",
                  "attribute_contributions": {"minecraft:generic.attack_damage": [-1, 0, 1, 2, 3]},
                  "ability_multipliers": {"shockwave": [0.9, 1.0, 1.05, 1.1, 1.2]}
                }]}
                """;

        RewardProfile profile = BalanceDataParser.parseRewards(JsonParser.parseString(json).getAsJsonObject()).get(0);

        assertEquals(-1.0D, profile.attributeContributions().get(ResourceLocation.fromNamespaceAndPath("minecraft", "generic.attack_damage")).valueAt(1));
        assertEquals(1.2D, profile.abilityMultipliers().get("shockwave").valueAt(5));
    }
}
