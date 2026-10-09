package com.siirio.jemserver.smp.events;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class CookingRecipeGraphTest {
    @Test
    void denseReversibleGraphsResolveWithinABoundedTime() {
        assertTimeoutPreemptively(java.time.Duration.ofSeconds(5), () -> {
            var recipes = new HashMap<String, List<CookingRecipeGraph.Step>>();
            int size = 512;
            for (int index = 0; index < size; index++) {
                var closed = new ArrayList<String>();
                var rooted = new ArrayList<String>();
                for (int offset = 1; offset <= 32; offset++) {
                    closed.add("closed_" + (index + offset) % size);
                    rooted.add("rooted_" + (index + offset) % size);
                }
                if (index % 32 == 0) rooted.add("seed");
                recipes.put("closed_" + index, List.of(new CookingRecipeGraph.Step("closed_" + index, "minecraft:crafting", List.of(closed))));
                recipes.put("rooted_" + index, List.of(new CookingRecipeGraph.Step("rooted_" + index, "minecraft:crafting", List.of(rooted))));
            }
            var graph = new CookingRecipeGraph(Map.of("seed", new CookingRecipeGraph.Ingredient(0, 0, true)), recipes, 0);
            for (int index = 0; index < size; index++) {
                assertNull(graph.dish("closed_" + index));
                assertNotNull(graph.dish("rooted_" + index));
                assertEquals(Set.of("seed"), graph.dish("rooted_" + index).ingredients());
            }
        });
    }

    @Test
    void laterCheaperAlternativeReplacesTheInitiallyReachableRecipe() {
        var graph = new CookingRecipeGraph(Map.of(
                "rare", new CookingRecipeGraph.Ingredient(30, 0, true),
                "seed", new CookingRecipeGraph.Ingredient(0, 0, true)), Map.of(
                "filling", List.of(step("prepare", "seed")),
                "dish", List.of(step("expensive", "rare"), step("cheap", "filling"))), 0);
        assertEquals(Set.of("seed"), graph.dish("dish").ingredients());
        assertEquals(Set.of("prepare", "cheap"), graph.dish("dish").stages());
    }

    @Test
    void equalCostAlternativesIgnoreRecipeAndIngredientIterationOrder() {
        var ingredients = Map.of("apple", new CookingRecipeGraph.Ingredient(0, 0, true),
                "pear", new CookingRecipeGraph.Ingredient(0, 0, true));
        var first = new CookingRecipeGraph.Step("bake", "minecraft:crafting", List.of(List.of("pear", "apple")));
        var second = new CookingRecipeGraph.Step("bake", "minecraft:crafting", List.of(List.of("apple", "pear")));
        var left = new CookingRecipeGraph(ingredients, Map.of("dish", List.of(step("other", "pear"), first)), 0).dish("dish");
        var right = new CookingRecipeGraph(ingredients, Map.of("dish", List.of(second, step("other", "pear"))), 0).dish("dish");
        assertEquals(left, right);
        assertEquals(Set.of("apple"), left.ingredients());
    }

    @Test
    void limitsRecipeDepthWithoutRecursiveTraversal() {
        var recipes = new HashMap<String, List<CookingRecipeGraph.Step>>();
        for (int index = 1; index <= 40; index++)
            recipes.put("item_" + index, List.of(step("step_" + index, "item_" + (index - 1))));
        var graph = new CookingRecipeGraph(Map.of("item_0", new CookingRecipeGraph.Ingredient(0, 0, true)), recipes, 0);
        assertNotNull(graph.dish("item_16"));
        assertNull(graph.dish("item_17"));
    }

    private CookingRecipeGraph.Step step(String id, String... inputs) {
        return new CookingRecipeGraph.Step(id, "minecraft:crafting", Arrays.stream(inputs).map(List::of).toList());
    }

    @Test
    void circularRecipesDoNotProveAnIngredientObtainable() {
        var graph = new CookingRecipeGraph(Map.of(), Map.of("a", List.of(step("a", "b")), "b", List.of(step("b", "a"))), 0);
        assertNull(graph.dish("a"));
        assertNull(graph.dish("b"));
    }

    @Test
    void harvestedWheatRemainsAvailableDespiteHayConversionCycle() {
        var graph = new CookingRecipeGraph(Map.of("wheat", new CookingRecipeGraph.Ingredient(0, 0, true)), Map.of(
                "wheat", List.of(step("unpack", "hay")), "hay", List.of(step("pack", "wheat")), "bread", List.of(step("bake", "wheat"))), 0);
        var bread = graph.dish("bread");
        assertNotNull(bread);
        assertEquals(Set.of("wheat"), bread.ingredients());
        assertEquals(Set.of("bake"), bread.stages());
    }

    @Test
    void gatedIngredientCanUseAnAccessibleAlternativeRecipe() {
        var graph = new CookingRecipeGraph(Map.of(
                "end_fruit", new CookingRecipeGraph.Ingredient(0, 2, true),
                "apple", new CookingRecipeGraph.Ingredient(0, 0, true)), Map.of(
                "end_fruit", List.of(step("alternative", "apple")), "dish", List.of(step("dish", "end_fruit"))), 0);
        var dish = graph.dish("dish");
        assertNotNull(dish);
        assertEquals(Set.of("apple"), dish.ingredients());
        assertEquals(0, dish.dimensions());
    }

    @Test
    void nestedExclusiveIngredientRequiresActualDimensionUnlock() {
        var sources = Map.of("end_fruit", new CookingRecipeGraph.Ingredient(0, 2, true));
        var recipes = Map.of("filling", List.of(step("filling", "end_fruit")), "dish", List.of(step("dish", "filling")));
        assertNull(new CookingRecipeGraph(sources, recipes, 0).dish("dish"));
        assertNotNull(new CookingRecipeGraph(sources, recipes, 2).dish("dish"));
    }

    @Test
    void ingredientTagAlternativesChooseAvailableBranch() {
        var sources = Map.of("end_fruit", new CookingRecipeGraph.Ingredient(0, 2, true), "apple", new CookingRecipeGraph.Ingredient(0, 0, true));
        var recipe = new CookingRecipeGraph.Step("dish", "test:oven", List.of(List.of("end_fruit", "apple")));
        var dish = new CookingRecipeGraph(sources, Map.of("dish", List.of(recipe)), 0).dish("dish");
        assertNotNull(dish);
        assertEquals(Set.of("apple"), dish.ingredients());
        assertEquals(Set.of("test:oven"), dish.stations());
    }
}
