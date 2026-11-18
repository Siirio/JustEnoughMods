package com.siirio.jemcompat.client.jei;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.UseAnim;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class CuratedJeiCatalog {
    private static final List<CatalogCategory> TOP_LEVEL = List.of(CatalogCategory.WEAPONS, CatalogCategory.WEARABLE, CatalogCategory.FOOD,
            CatalogCategory.ORES,
            CatalogCategory.CREATE_REDSTONE, CatalogCategory.BUILDING, CatalogCategory.FISHING, CatalogCategory.BOOKS,
            CatalogCategory.POTIONS, CatalogCategory.TRANSPORT, CatalogCategory.SPAWN_EGGS, CatalogCategory.OTHER);
    private static final List<CatalogCategory> BUILDING = List.of(CatalogCategory.BUILDING_BLOCKS, CatalogCategory.GLASS,
            CatalogCategory.LIGHT, CatalogCategory.FURNITURE, CatalogCategory.CONTAINERS, CatalogCategory.NATURE,
            CatalogCategory.HOLIDAY);
    private static final List<CatalogCategory> WEAPONS = List.of(CatalogCategory.WEAPON_TOTEMS,
            CatalogCategory.WEAPON_RANGED, CatalogCategory.WEAPON_MELEE);
    private static final EnumMap<CatalogCategory, List<ItemStack>> STACKS = new EnumMap<>(CatalogCategory.class);
    private static final Set<Item> COOKING_CATALYSTS = new HashSet<>();

    private CuratedJeiCatalog() {
    }

    public static void rebuild(IJeiRuntime runtime) {
        clear();
        runtime.getRecipeManager().createRecipeCategoryLookup().get()
                .filter(category -> isCookingRecipe(category.getRecipeType().getUid()))
                .forEach(category -> runtime.getRecipeManager().createRecipeCatalystLookup(category.getRecipeType())
                        .get(VanillaTypes.ITEM_STACK)
                        .map(ItemStack::getItem)
                        .forEach(COOKING_CATALYSTS::add));
        runtime.getIngredientManager().getAllIngredients(VanillaTypes.ITEM_STACK).forEach(stack -> {
            ItemStack preview = stack.copy();
            if (preview.isDamageableItem()) preview.setDamageValue(0);
            CatalogCategory primary = classify(preview);
            STACKS.computeIfAbsent(primary, ignored -> new ArrayList<>()).add(preview);
            if (primary == CatalogCategory.WEAPONS) {
                STACKS.computeIfAbsent(classifyWeapon(preview), ignored -> new ArrayList<>()).add(preview);
            }
        });
        STACKS.replaceAll((category, stacks) -> Collections.unmodifiableList(stacks));
    }

    public static void clear() {
        STACKS.clear();
        COOKING_CATALYSTS.clear();
        com.siirio.jemcompat.catalog.JemCatalogCategories.clearCaches();
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

    public static List<ItemStack> stacks(CatalogCategory category) {
        if (category != CatalogCategory.BUILDING) {
            return STACKS.getOrDefault(category, List.of());
        }
        return BUILDING.stream().flatMap(child -> STACKS.getOrDefault(child, List.of()).stream()).toList();
    }

    private static CatalogCategory classify(ItemStack stack) {
        return CatalogCategory.valueOf(com.siirio.jemcompat.catalog.JemCatalogCategories.classify(stack,
                Minecraft.getInstance().player, value -> COOKING_CATALYSTS.contains(value.getItem())).name());
    }

    private static CatalogCategory classifyWeapon(ItemStack stack) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        String path = id == null ? "" : id.getPath().toLowerCase(Locale.ROOT);
        if (hasWord(path, "totem") || stack.getTags().anyMatch(tag -> hasWord(tag.location().getPath(), "totem"))) {
            return CatalogCategory.WEAPON_TOTEMS;
        }
        UseAnim use = stack.getUseAnimation();
        if (stack.getItem() instanceof ProjectileWeaponItem || stack.getItem() instanceof ArrowItem
                || stack.getItem() instanceof TridentItem || use == UseAnim.BOW || use == UseAnim.CROSSBOW
                || use == UseAnim.SPEAR || hasWord(path, "bow", "crossbow", "arrow", "bolt", "spear", "javelin",
                "trident", "throwable", "throwing", "boomerang", "chakram", "dart", "shuriken", "gun", "rifle",
                "pistol", "musket", "cannon", "launcher", "blaster", "slingshot")
                || stack.getTags().anyMatch(tag -> hasWord(tag.location().getPath(), "bows", "crossbows", "arrows",
                "spears", "throwable", "ranged"))) {
            return CatalogCategory.WEAPON_RANGED;
        }
        return CatalogCategory.WEAPON_MELEE;
    }

    private static boolean hasWord(String value, String... words) {
        String normalized = value.toLowerCase(Locale.ROOT);
        for (String word : words) {
            if (normalized.equals(word) || normalized.startsWith(word + "_") || normalized.endsWith("_" + word)
                    || normalized.contains("_" + word + "_")) return true;
        }
        return false;
    }

    private static boolean isCookingRecipe(ResourceLocation id) {
        String value = id.toString().toLowerCase(Locale.ROOT);
        return containsAny(value, "smelting", "smoking", "campfire", "cooking", "baking", "caking", "brewing", "fermenting",
                "frying", "oven", "stove", "kettle", "mini_fridge", "palm_bar", "cutting_board");
    }

    private static boolean containsAny(String value, String... needles) {
        for (String needle : needles) {
            if (value.contains(needle)) {
                return true;
            }
        }
        return false;
    }
}
