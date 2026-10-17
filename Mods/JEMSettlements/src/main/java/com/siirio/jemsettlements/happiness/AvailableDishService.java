package com.siirio.jemsettlements.happiness;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;

/** Uses the optional dedicated SMP recipe owner, otherwise local dimension visits and recipes. */
public final class AvailableDishService {
    private AvailableDishService() {}

    public static List<ResourceLocation> moddedDishes(MinecraftServer server) {
        if (!server.isDedicatedServer() || !net.minecraftforge.fml.ModList.get().isLoaded("jem_server")) return standalone(server);
        try {
            Class<?> recipes = Class.forName("com.siirio.jemserver.smp.events.CookingRecipes");
            Method available = recipes.getMethod("available", MinecraftServer.class);
            var result = new ArrayList<ResourceLocation>();
            for (Object dish : (List<?>) available.invoke(null, server)) {
                Object value = dish.getClass().getMethod("item").invoke(dish);
                if (value instanceof ResourceLocation id) {
                    var item = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(id);
                    if (item != null && FoodValueRegistry.classify(item.getDefaultInstance()).filter(food -> food.mood() > 0).isPresent()) result.add(id);
                }
            }
            return List.copyOf(result);
        } catch (ReflectiveOperationException | LinkageError unavailable) {
            return standalone(server);
        }
    }
    private static MinecraftServer cachedServer;
    private static long cachedAt = Long.MIN_VALUE;
    private static Object cachedRecipes;
    private static List<ResourceLocation> cached = List.of();
    private static List<ResourceLocation> standalone(MinecraftServer server) {
        if (cachedServer == server && cachedRecipes == server.getRecipeManager() && server.overworld().getGameTime() - cachedAt < 200L) return cached;
        DishProgression progress = DishProgression.get(server);
        for (var player : server.getPlayerList().getPlayers()) progress.visit(player.level().dimension().location());
        var recipes = new java.util.HashMap<ResourceLocation, java.util.List<java.util.List<net.minecraft.world.item.crafting.Ingredient>>>();
        var reachable = new java.util.HashSet<ResourceLocation>();
        var foods = new java.util.TreeSet<ResourceLocation>();
        for (var item : net.minecraftforge.registries.ForgeRegistries.ITEMS) {
            var id = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(item);
            if (id != null && permitted(item.getDefaultInstance(), progress)) reachable.add(id);
        }
        for (var recipe : server.getRecipeManager().getRecipes()) {
            var stack = recipe.getResultItem(server.registryAccess()); if (stack.isEmpty() || recipe.getIngredients().isEmpty()) continue;
            ResourceLocation id = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem());
            if (id == null) continue;
            recipes.computeIfAbsent(id, ignored -> new java.util.ArrayList<>()).add(recipe.getIngredients());
            if (FoodValueRegistry.classify(stack).filter(value -> value.mood() > 0).isPresent()) foods.add(id);
        }
        // Crafted intermediates must have a reachable recipe; alternatives are OR,
        // ingredient slots are AND. This also prevents a locked intermediate from leaking into a dish.
        reachable.removeIf(id -> recipes.containsKey(id) && !net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(id).getDefaultInstance().is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, new ResourceLocation("jem_settlements", "cooking/world_ingredients"))));
        boolean changed;
        do { changed = false;
            for (var entry : recipes.entrySet()) if (!reachable.contains(entry.getKey())) {
                var item = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(entry.getKey());
                if (item == null || !permitted(item.getDefaultInstance(), progress)) continue;
                if (entry.getValue().stream().anyMatch(inputs -> inputs.stream().filter(ingredient -> ingredient != net.minecraft.world.item.crafting.Ingredient.EMPTY).allMatch(ingredient -> java.util.Arrays.stream(ingredient.getItems()).anyMatch(stack -> reachable.contains(net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem())))))) changed |= reachable.add(entry.getKey());
            }
        } while (changed);
        cached = foods.stream().filter(reachable::contains).toList(); cachedServer = server; cachedRecipes = server.getRecipeManager(); cachedAt = server.overworld().getGameTime(); return cached;
    }
    private static boolean permitted(net.minecraft.world.item.ItemStack stack, DishProgression progress) {
        ResourceLocation id = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem());
        boolean nether = stack.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, new ResourceLocation("jem_settlements", "cooking/nether_ingredients")))
            || id != null && java.util.Set.of("kaleidoscope_nether", "netherexp", "luminous_nether").contains(id.getNamespace());
        boolean end = stack.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, new ResourceLocation("jem_settlements", "cooking/end_ingredients")))
            || id != null && id.getNamespace().equals("kaleidoscope_end");
        return (!nether || progress.visited("minecraft:the_nether")) && (!end || progress.visited("minecraft:the_end"));
    }

}
