package com.siirio.jemcompat.client.jei;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;

public final class CuratedJeiCatalog {
    private static final List<CatalogCategory> TOP_LEVEL = List.of(CatalogCategory.WEAPONS, CatalogCategory.WEARABLES, CatalogCategory.FOOD,
            CatalogCategory.ORES, CatalogCategory.COMPONENTS,
            CatalogCategory.CREATE_REDSTONE, CatalogCategory.BUILDING, CatalogCategory.FISHING, CatalogCategory.BOOKS,
            CatalogCategory.POTIONS, CatalogCategory.TRANSPORT, CatalogCategory.COMPANIONS,
            CatalogCategory.SPAWN_EGGS, CatalogCategory.OTHER);
    private static final List<CatalogCategory> BUILDING = List.of(CatalogCategory.BUILDING_BLOCKS, CatalogCategory.BUILDING_GLASS,
            CatalogCategory.BUILDING_LIGHT, CatalogCategory.BUILDING_FURNITURE, CatalogCategory.BUILDING_CONTAINERS,
            CatalogCategory.BUILDING_NATURE, CatalogCategory.BUILDING_HOLIDAY, CatalogCategory.BUILDING_TOOLS);
    private static final List<CatalogCategory> WEAPONS = List.of(CatalogCategory.WEAPON_TOTEMS,
            CatalogCategory.WEAPON_RANGED, CatalogCategory.WEAPON_MELEE, CatalogCategory.WEAPON_SHIELDS,
            CatalogCategory.WEAPON_SMITHING_TEMPLATES, CatalogCategory.WEAPON_TRAINING);
    private static final List<CatalogCategory> WEARABLES = List.of(CatalogCategory.WEARABLE_ARMOR,
            CatalogCategory.WEARABLE_MOB_ARMOR, CatalogCategory.WEARABLE_DOLLS);
    private static final List<CatalogCategory> FOOD = List.of(CatalogCategory.FOOD_EDIBLES,
            CatalogCategory.FOOD_COOKING_EQUIPMENT);
    private static final EnumMap<CatalogCategory, List<ItemStack>> STACKS = new EnumMap<>(CatalogCategory.class);

    private CuratedJeiCatalog() {
    }

    public static void rebuild(IJeiRuntime runtime) {
        clear();
        runtime.getIngredientManager().getAllIngredients(VanillaTypes.ITEM_STACK).forEach(stack -> {
            ItemStack preview = stack.copy();
            if (preview.isDamageableItem()) {
                preview.setDamageValue(0);
            }
            CatalogCategory leaf = CatalogCategory.valueOf(
                    com.siirio.jemcompat.catalog.JemCatalogCategories.classify(preview).name());
            STACKS.computeIfAbsent(leaf, ignored -> new ArrayList<>()).add(preview);
            com.siirio.jemcompat.catalog.JemCatalogCategories.additionalCategories(preview).forEach(view ->
                    STACKS.computeIfAbsent(CatalogCategory.valueOf(view.name()), ignored -> new ArrayList<>())
                            .add(preview));
        });
        STACKS.replaceAll((category, stacks) -> Collections.unmodifiableList(stacks));
    }

    public static void clear() {
        STACKS.clear();
    }

    public static List<CatalogCategory> topLevel(boolean creative) {
        if (creative) {
            return TOP_LEVEL;
        }
        return TOP_LEVEL.stream().filter(category -> category != CatalogCategory.SPAWN_EGGS).toList();
    }

    public static List<CatalogCategory> building() {
        return BUILDING;
    }

    public static List<CatalogCategory> weapons() {
        return WEAPONS;
    }

    public static List<CatalogCategory> wearables() {
        return WEARABLES;
    }

    public static List<CatalogCategory> food() {
        return FOOD;
    }

    public static List<ItemStack> stacks(CatalogCategory category) {
        if (category == CatalogCategory.BUILDING) {
            return aggregate(BUILDING);
        }
        if (category == CatalogCategory.WEAPONS) {
            return aggregate(WEAPONS);
        }
        if (category == CatalogCategory.WEARABLES) {
            return aggregate(WEARABLES);
        }
        if (category == CatalogCategory.FOOD) {
            return aggregate(FOOD);
        }
        return STACKS.getOrDefault(category, List.of());
    }

    private static List<ItemStack> aggregate(List<CatalogCategory> children) {
        return children.stream().flatMap(child -> STACKS.getOrDefault(child, List.of()).stream()).toList();
    }
}
