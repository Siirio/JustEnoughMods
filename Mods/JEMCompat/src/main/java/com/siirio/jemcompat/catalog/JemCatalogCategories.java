package com.siirio.jemcompat.catalog;

import net.minecraft.world.item.ItemStack;

import java.util.Set;
import java.util.function.Predicate;

/** Shared exact item-category owner for the client recipe browser and server purchase admission. */
public final class JemCatalogCategories {
    public enum Kind {
        WEAPONS,
        WEAPON_TOTEMS,
        WEAPON_RANGED,
        WEAPON_MELEE,
        WEAPON_SHIELDS,
        WEAPON_TRAINING,
        WEARABLES,
        WEARABLE_ARMOR,
        WEARABLE_MOB_ARMOR,
        WEARABLE_DOLLS,
        FOOD,
        FOOD_EDIBLES,
        FOOD_COOKING_EQUIPMENT,
        ORES,
        CREATE_REDSTONE,
        BUILDING,
        BUILDING_FURNITURE,
        BUILDING_NATURE,
        BUILDING_CONTAINERS,
        BUILDING_HOLIDAY,
        BUILDING_BLOCKS,
        BUILDING_GLASS,
        BUILDING_LIGHT,
        BUILDING_TOOLS,
        FISHING,
        BOOKS,
        POTIONS,
        TRANSPORT,
        COMPANIONS,
        SPAWN_EGGS,
        OTHER
    }

    private JemCatalogCategories() {
    }

    public static Kind classify(ItemStack stack) {
        return JemCatalogManifest.category(stack);
    }

    /** Exact additional catalog views. These do not change the primary category used by Economy. */
    public static Set<Kind> additionalCategories(ItemStack stack) {
        return JemCatalogManifest.additionalCategories(stack);
    }

    /** Retained for binary compatibility with the earlier shared classifier contract. */
    @Deprecated(forRemoval = false)
    public static Kind classify(ItemStack stack, net.minecraft.world.entity.LivingEntity wearer,
                                Predicate<ItemStack> cookingCatalyst) {
        Kind category = classify(stack);
        return switch (category) {
            case WEAPON_TOTEMS, WEAPON_RANGED, WEAPON_MELEE, WEAPON_SHIELDS, WEAPON_TRAINING -> Kind.WEAPONS;
            case WEARABLE_ARMOR, WEARABLE_MOB_ARMOR, WEARABLE_DOLLS -> Kind.WEARABLES;
            case FOOD_EDIBLES, FOOD_COOKING_EQUIPMENT -> Kind.FOOD;
            case BUILDING_FURNITURE, BUILDING_NATURE, BUILDING_CONTAINERS, BUILDING_HOLIDAY,
                    BUILDING_BLOCKS, BUILDING_GLASS, BUILDING_LIGHT, BUILDING_TOOLS -> Kind.BUILDING;
            default -> category;
        };
    }

    /** Retained for binary compatibility. The manifest has no mutable classifier caches. */
    @Deprecated(forRemoval = false)
    public static void clearCaches() {
    }

    public static boolean isMarketBuilding(ItemStack stack) {
        return switch (classify(stack)) {
            case CREATE_REDSTONE, BUILDING_FURNITURE, BUILDING_NATURE, BUILDING_CONTAINERS,
                    BUILDING_HOLIDAY, BUILDING_BLOCKS, BUILDING_GLASS, BUILDING_LIGHT,
                    BUILDING_TOOLS -> true;
            default -> false;
        };
    }
}
