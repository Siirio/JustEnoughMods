package com.siirio.jemserver.smp.events;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CookingShowTest {
    private static final String DISH = "bakery:vegetable_sandwich";

    @Test
    void missingTypeRegistrationPreservesTheNativeStationRecipe() {
        var serializer = new net.minecraft.resources.ResourceLocation("bakery", "baking_station");
        String station = CookingRecipes.station(null, serializer, getClass());
        var graph = new CookingRecipeGraph(java.util.Map.of("flour", new CookingRecipeGraph.Ingredient(0, 0, true)),
                java.util.Map.of("bread", java.util.List.of(new CookingRecipeGraph.Step("bake", station,
                        java.util.List.of(java.util.List.of("flour"))))), 0);
        var dish = graph.dish("bread");
        assertNotNull(dish);
        assertEquals(java.util.Set.of("bakery:baking_station"), dish.stations());
    }

    @Test
    void stationIdentityPrefersRegisteredTypeAndHasAStableUnregisteredFallback() {
        var type = new net.minecraft.resources.ResourceLocation("minecraft", "crafting");
        var serializer = new net.minecraft.resources.ResourceLocation("minecraft", "crafting_shaped");
        assertEquals("minecraft:crafting", CookingRecipes.station(type, serializer, getClass()));
        assertEquals(getClass().getName(), CookingRecipes.station(null, null, getClass()));
    }

    @Test
    void partitionsCurrentRecipeRankingWithoutLosingDishes() {
        var dishes = java.util.stream.IntStream.range(0, 8)
                .mapToObj(i -> new CookingRecipes.Dish(new net.minecraft.resources.ResourceLocation("test", "dish_" + i), i))
                .toList();
        var combined = new java.util.ArrayList<CookingRecipes.Dish>();
        for (int difficulty = 0; difficulty < 3; difficulty++) combined.addAll(CookingRecipes.tier(dishes, difficulty));
        assertEquals(dishes, combined);
        assertEquals(dishes.subList(0, 1), CookingRecipes.tier(dishes.subList(0, 1), 2));
    }

    @Test
    void dimensionFlagsSurviveSaveLoadIndependently() {
        var tag = new CompoundTag();
        tag.putBoolean("netherUnlocked", true);
        var data = DimensionProgress.load(tag);
        assertTrue(data.netherUnlocked());
        assertFalse(data.endUnlocked());
        var saved = data.save(new CompoundTag());
        var restored = DimensionProgress.load(saved);
        assertTrue(restored.netherUnlocked());
        assertFalse(restored.endUnlocked());
    }

    @Test
    void eachDifficultyOffersAllBookPowersWithSpecifiedWeights() throws Exception {
        String[] levels = {"easy", "medium", "hard"};
        int[][] expected = {{70, 25, 5}, {35, 45, 20}, {10, 35, 55}};
        for (int difficulty = 0; difficulty < levels.length; difficulty++) {
            try (var stream = getClass().getResourceAsStream("/data/jem_server/loot_tables/events/cooking_" + levels[difficulty] + ".json")) {
                assertNotNull(stream);
                var root = com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(stream, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
                var entries = root.getAsJsonArray("pools").get(2).getAsJsonObject().getAsJsonArray("entries");
                assertEquals(3, entries.size());
                for (int index = 0; index < 3; index++) {
                    var entry = entries.get(index).getAsJsonObject();
                    assertEquals(expected[difficulty][index], entry.get("weight").getAsInt());
                    var enchant = entry.getAsJsonArray("functions").get(0).getAsJsonObject();
                    assertEquals((index + 1) * 10, enchant.get("levels").getAsInt());
                    assertFalse(enchant.get("treasure").getAsBoolean());
                }
            }
        }
    }

    @Test
    void repeatedRecipeBatchesCreditTheirOutputAndCapAtRequirement() {
        var requirements = new CompoundTag();
        requirements.putInt(DISH, 10);
        var score = new CompoundTag();
        assertTrue(CookingShow.advance(requirements, score, DISH, 4));
        assertEquals(4, score.getInt(DISH));
        assertTrue(CookingShow.advance(requirements, score, DISH, 4));
        assertEquals(8, score.getInt(DISH));
        assertTrue(CookingShow.advance(requirements, score, DISH, 4));
        assertEquals(10, score.getInt(DISH));
        assertFalse(CookingShow.advance(requirements, score, DISH, 4));
    }

    @Test
    void ignoresUnlistedDishesEmptyEventsAndPreviouslyRewardedPlayers() {
        var requirements = new CompoundTag();
        requirements.putInt(DISH, 8);
        var score = new CompoundTag();
        assertFalse(CookingShow.advance(requirements, score, "minecraft:bread", 4));
        assertFalse(CookingShow.advance(requirements, score, DISH, 0));
        assertFalse(CookingShow.advance(requirements, score, DISH, -1));
        assertTrue(score.isEmpty());
        score.putBoolean("rewarded", true);
        assertFalse(CookingShow.advance(requirements, score, DISH, 4));
        assertEquals(0, score.getInt(DISH));
    }

    @Test
    void quantityCannotOverflowIntoNegativeProgress() {
        var requirements = new CompoundTag();
        requirements.putInt(DISH, 64);
        var score = new CompoundTag();
        score.putInt(DISH, 1);
        assertTrue(CookingShow.advance(requirements, score, DISH, Integer.MAX_VALUE));
        assertEquals(64, score.getInt(DISH));
    }
}
