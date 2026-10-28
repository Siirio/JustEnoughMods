package com.justenoughmods.achievementguide.integration.food;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Observes installed Beachparty recipes without changing native processing or collecting output. */
public final class ColdSuppliesOutputProof {
    private ColdSuppliesOutputProof() {}

    public record Pending(FoodOutputProof.Before output, ItemStack result, List<Ingredient> ingredients,
                          List<ItemStack> inputs, String recipe, String action, boolean cooling) {}

    public static Pending before(BlockEntity owner, Recipe<Container> recipe, RegistryAccess registries, boolean cooling) {
        if (owner.getLevel() == null || owner.getLevel().isClientSide || recipe == null) return null;
        String id = recipe.getId().toString();
        String expected = cooling ? switch (id) {
            case "beachparty:mini_fridge_freezing/ice" -> "minecraft:ice";
            case "beachparty:mini_fridge_freezing/packed_ice" -> "minecraft:packed_ice";
            case "beachparty:mini_fridge_freezing/blue_ice" -> "minecraft:blue_ice";
            case "beachparty:mini_fridge_freezing/powder_snow_bucket" -> "minecraft:powder_snow_bucket";
            default -> "";
        } : switch (id) {
            case "beachparty:palm_bar_mixing/cocoa_cocktail" -> "beachparty:cocoa_cocktail";
            case "beachparty:palm_bar_mixing/coconut_cocktail" -> "beachparty:coconut_cocktail";
            case "beachparty:palm_bar_mixing/honey_cocktail" -> "beachparty:honey_cocktail";
            case "beachparty:palm_bar_mixing/melon_cocktail" -> "beachparty:melon_cocktail";
            case "beachparty:palm_bar_mixing/pumpkin_cocktail" -> "beachparty:pumpkin_cocktail";
            case "beachparty:palm_bar_mixing/sweetberries_cocktail" -> "beachparty:sweetberries_cocktail";
            default -> "";
        };
        if (expected.isEmpty()) return null;
        ItemStack result = recipe.getResultItem(registries).copy();
        if (!FoodOutputProof.item(result, expected)) return null;
        Container inventory = (Container)owner;
        int inputSlots = cooling ? 1 : 4;
        if (recipe.getIngredients().isEmpty() || recipe.getIngredients().size() > inputSlots) return null;
        List<ItemStack> inputs = new ArrayList<>(inputSlots);
        for (int slot = 1; slot <= inputSlots; slot++) inputs.add(inventory.getItem(slot).copy());
        if (cooling && !coolingInput(id, inputs.get(0))) return null;
        List<Ingredient> ingredients = List.copyOf(recipe.getIngredients());
        for (Ingredient ingredient : ingredients) if (matching(inputs, ingredient) == 0) return null;
        return new Pending(FoodOutputProof.before(owner, 0), result, ingredients, List.copyOf(inputs), id,
                cooling ? "food_cold_supplies_cooled" : "v3_food_cold_supplies", cooling);
    }

    public static void completed(BlockEntity owner, Pending pending) {
        if (pending == null || owner.getLevel() == null || owner.getLevel().isClientSide) return;
        Container inventory = (Container)owner;
        ItemStack output = inventory.getItem(0);
        ItemStack previous = pending.output.output();
        int oldCount = previous.isEmpty() ? 0 : previous.getCount();
        if (!ItemStack.isSameItemSameTags(output, pending.result)
                || !previous.isEmpty() && !ItemStack.isSameItemSameTags(output, previous)
                || output.getCount() - oldCount != pending.result.getCount()) return;
        List<ItemStack> after = new ArrayList<>(pending.inputs.size());
        for (int slot = 1; slot <= pending.inputs.size(); slot++) after.add(inventory.getItem(slot));
        // Installed recipes have distinct ingredient domains. Require an actual native debit for each.
        for (Ingredient ingredient : pending.ingredients) {
            if (matching(pending.inputs, ingredient) <= matching(after, ingredient)) return;
        }
        if (pending.cooling) {
            ItemStack input = pending.inputs.get(0);
            ItemStack remaining = after.get(0);
            if (input.getCount() - (remaining.isEmpty() ? 0 : remaining.getCount()) != 1
                    || !remaining.isEmpty() && !ItemStack.isSameItemSameTags(input, remaining)) return;
        }
        FoodOutputProof.completed(owner, 0, pending.recipe, pending.action, pending.output);
    }

    private static int matching(List<ItemStack> inputs, Ingredient ingredient) {
        int count = 0;
        for (ItemStack input : inputs) if (!input.isEmpty() && ingredient.test(input)) count += input.getCount();
        return count;
    }

    private static boolean coolingInput(String recipe, ItemStack input) {
        return switch (recipe) {
            case "beachparty:mini_fridge_freezing/ice" -> FoodOutputProof.item(input, "minecraft:potion")
                    && PotionUtils.getPotion(input) == Potions.WATER;
            case "beachparty:mini_fridge_freezing/packed_ice" -> FoodOutputProof.item(input, "minecraft:ice");
            case "beachparty:mini_fridge_freezing/blue_ice" -> FoodOutputProof.item(input, "minecraft:packed_ice");
            case "beachparty:mini_fridge_freezing/powder_snow_bucket" -> FoodOutputProof.item(input, "minecraft:water_bucket");
            default -> false;
        };
    }
}
