package com.siirio.jemcompat.compat.jade;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class BreedingFoodCatalog {
    private static final Map<EntityType<?>, List<ResourceLocation>> CACHE = new ConcurrentHashMap<>();

    private BreedingFoodCatalog() {
    }

    public static List<ResourceLocation> itemsFor(Animal animal) {
        return CACHE.computeIfAbsent(animal.getType(), ignored -> BuiltInRegistries.ITEM.keySet().stream()
                .filter(BuiltInRegistries.ITEM::containsKey)
                .filter(id -> BuiltInRegistries.ITEM.get(id) != Items.AIR)
                .filter(id -> animal.isFood(new ItemStack(BuiltInRegistries.ITEM.get(id))))
                .toList());
    }

    public static void clear() {
        CACHE.clear();
    }
}
