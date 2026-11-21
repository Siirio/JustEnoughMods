package com.siirio.jemcompat.catalog;

import net.minecraft.world.item.ItemStack;

import java.util.function.Predicate;

/** Shared exact item-category owner for the client recipe browser and server purchase admission. */
public final class JemCatalogCategories {
    public enum Kind {
        WEAPONS,
        WEAPON_TOTEMS,
        WEAPON_RANGED,
        WEAPON_MELEE,
        WEARABLE,
        FOOD,
        ORES,
        CREATE_REDSTONE,
        BUILDING,
        FURNITURE,
        NATURE,
        CONTAINERS,
        HOLIDAY,
        BUILDING_BLOCKS,
        GLASS,
        LIGHT,
        FISHING,
        BOOKS,
        POTIONS,
        TRANSPORT,
        SPAWN_EGGS,
        OTHER
    }

    private JemCatalogCategories() {
    }

    public static Kind classify(ItemStack stack) {
        return JemCatalogManifest.category(stack);
    }

    /** Retained for binary compatibility with the earlier shared classifier contract. */
    @Deprecated(forRemoval = false)
    public static Kind classify(ItemStack stack, net.minecraft.world.entity.LivingEntity wearer,
                                Predicate<ItemStack> cookingCatalyst) {
        Kind category = classify(stack);
        return switch (category) {
            case WEAPON_TOTEMS, WEAPON_RANGED, WEAPON_MELEE -> Kind.WEAPONS;
            default -> category;
        };
    }

    /** Retained for binary compatibility. The manifest has no mutable classifier caches. */
    @Deprecated(forRemoval = false)
    public static void clearCaches() {
    }

    public static boolean isMarketBuilding(ItemStack stack) {
        return switch (classify(stack)) {
            case CREATE_REDSTONE, FURNITURE, NATURE, CONTAINERS, HOLIDAY, BUILDING_BLOCKS, GLASS, LIGHT -> true;
            default -> false;
        };
    }
}
