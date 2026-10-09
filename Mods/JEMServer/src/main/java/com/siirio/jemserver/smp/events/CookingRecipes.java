package com.siirio.jemserver.smp.events;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.*;

public final class CookingRecipes {
    private static final int NETHER_DIMENSION = 1;
    private static final int END_DIMENSION = 2;
    private static final int RARE_INGREDIENT_WEIGHT = 4;
    private static MinecraftServer cachedServer;
    private static Object cachedManager;
    private static int cachedProgression = -1;
    private static List<Dish> cachedDishes = List.of();
    private static final class IngredientTags {
        private static final TagKey<Item> NETHER = tag("nether_ingredients");
        private static final TagKey<Item> END = tag("end_ingredients");
        private static final TagKey<Item> RARE = tag("rare_ingredients");
        private static final TagKey<Item> WORLD = tag("world_ingredients");
        private static TagKey<Item> tag(String name) {
            return TagKey.create(Registries.ITEM, new ResourceLocation("jem_server", "cooking/" + name));
        }
    }
    public record Dish(ResourceLocation item, int difficultyScore) {}

    public static List<Dish> available(MinecraftServer server) {
        var progression = DimensionProgress.get(server);
        int unlocked = (progression.netherUnlocked() ? NETHER_DIMENSION : 0) | (progression.endUnlocked() ? END_DIMENSION : 0);
        if (cachedServer == server && cachedManager == server.getRecipeManager() && cachedProgression == unlocked) return cachedDishes;
        var recipes = new HashMap<String, List<CookingRecipeGraph.Step>>();
        var ingredients = new HashMap<String, CookingRecipeGraph.Ingredient>();
        var foods = new HashSet<String>();
        for (var recipe : server.getRecipeManager().getRecipes()) {
            ItemStack output = recipe.getResultItem(server.registryAccess());
            if (output.isEmpty() || recipe.getIngredients().isEmpty()) continue;
            String id = describe(output, ingredients);
            var inputs = new ArrayList<List<String>>();
            for (var ingredient : recipe.getIngredients()) {
                if (ingredient == net.minecraft.world.item.crafting.Ingredient.EMPTY) continue;
                inputs.add(Arrays.stream(ingredient.getItems()).map(stack -> describe(stack, ingredients)).distinct().toList());
            }
            if (inputs.isEmpty()) continue;
            recipes.computeIfAbsent(id, ignored -> new ArrayList<>()).add(new CookingRecipeGraph.Step(
                    recipe.getId().toString(), station(ForgeRegistries.RECIPE_TYPES.getKey(recipe.getType()),
                    ForgeRegistries.RECIPE_SERIALIZERS.getKey(recipe.getSerializer()), recipe.getType().getClass()), inputs));
            if (food(output.getItem())) foods.add(id);
        }
        var graph = new CookingRecipeGraph(ingredients, recipes, unlocked);
        var result = new ArrayList<Dish>();
        for (String id : new TreeSet<>(foods)) {
            var cost = graph.dish(id);
            if (cost != null) result.add(new Dish(new ResourceLocation(id), cost.score()));
        }
        result.sort(Comparator.comparingInt(Dish::difficultyScore).thenComparing(dish -> dish.item().toString()));
        cachedServer = server;
        cachedManager = server.getRecipeManager();
        cachedProgression = unlocked;
        cachedDishes = List.copyOf(result);
        return cachedDishes;
    }

    public static void reset() {
        cachedServer = null;
        cachedManager = null;
        cachedProgression = -1;
        cachedDishes = List.of();
    }

    static String station(ResourceLocation type, ResourceLocation serializer, Class<?> implementation) {
        if (type != null) return type.toString();
        if (serializer != null) return serializer.toString();
        return implementation.getName();
    }

    private static String describe(ItemStack stack, Map<String, CookingRecipeGraph.Ingredient> ingredients) {
        String id = ForgeRegistries.ITEMS.getKey(stack.getItem()).toString();
        ingredients.computeIfAbsent(id, ignored -> new CookingRecipeGraph.Ingredient(
                stack.getRarity().ordinal() + (stack.is(IngredientTags.RARE) ? RARE_INGREDIENT_WEIGHT : 0),
                (stack.is(IngredientTags.NETHER) ? NETHER_DIMENSION : 0) | (stack.is(IngredientTags.END) ? END_DIMENSION : 0),
                stack.is(IngredientTags.WORLD)));
        return id;
    }

    static boolean food(Item item) {
        if (item.isEdible() || item.getUseAnimation(item.getDefaultInstance()) == net.minecraft.world.item.UseAnim.DRINK) return true;
        if (!(item instanceof net.minecraft.world.item.BlockItem block)) return false;
        return block.getBlock() instanceof net.minecraft.world.level.block.CakeBlock
                || block.getBlock().getClass().getName().startsWith("net.satisfy.bakery.core.block.cake.")
                || block.getBlock().getClass().getName().equals("net.satisfy.bakery.core.block.CupcakeBlock");
    }

    static List<Dish> tier(List<Dish> dishes, int difficulty) {
        if (dishes.isEmpty()) return List.of();
        int start = dishes.size() * difficulty / 3;
        int end = Math.max(start + 1, dishes.size() * (difficulty + 1) / 3);
        return dishes.subList(start, Math.min(dishes.size(), end));
    }

    private CookingRecipes() {}
}
