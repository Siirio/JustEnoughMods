package com.siirio.jemcompat.client.jei;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.BoatItem;
import net.minecraft.world.item.BookItem;
import net.minecraft.world.item.ElytraItem;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MinecartItem;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.BlockItem;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.fml.ModList;
import java.util.function.Predicate;
import com.siirio.jemworldbosstiers.enchantment.ProgressionEnchantment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class CuratedJeiCatalog {
    private static final Predicate<ItemStack> CURIO = ModList.get().isLoaded("curios")
            ? new CuriosEquipment() : stack -> false;


    private static final List<CatalogCategory> TOP_LEVEL = List.of(CatalogCategory.WEAPONS, CatalogCategory.WEARABLE, CatalogCategory.FOOD,
            CatalogCategory.CREATE_REDSTONE, CatalogCategory.BUILDING, CatalogCategory.FISHING, CatalogCategory.BOOKS,
            CatalogCategory.POTIONS, CatalogCategory.TRANSPORT, CatalogCategory.SPAWN_EGGS, CatalogCategory.OTHER);
    private static final List<CatalogCategory> BUILDING = List.of(CatalogCategory.FURNITURE, CatalogCategory.NATURE, CatalogCategory.CONTAINERS,
            CatalogCategory.HOLIDAY, CatalogCategory.BUILDING_BLOCKS);
    private static final EnumMap<CatalogCategory, List<ItemStack>> STACKS = new EnumMap<>(CatalogCategory.class);
    private static final Set<Item> COOKING_CATALYSTS = new HashSet<>();
    private static final TagKey<Item> TRANSPORT=TagKey.create(Registries.ITEM,new ResourceLocation("jemcompat","transport"));
    private static final TagKey<Item> CREATE_REDSTONE=TagKey.create(Registries.ITEM,new ResourceLocation("jemcompat","jei/create_redstone"));
    private static final TagKey<Item> NATURE=TagKey.create(Registries.ITEM,new ResourceLocation("jemcompat","jei/nature"));
    private static final TagKey<Item> FISHING=TagKey.create(Registries.ITEM,new ResourceLocation("jemcompat","jei/fishing"));
    private static List<ItemStack> allStacks = List.of();

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
        Map<ResourceLocation, ItemStack> unique = new LinkedHashMap<>();
        runtime.getIngredientManager().getAllIngredients(VanillaTypes.ITEM_STACK).forEach(stack -> {
            ItemStack preview = stack.copy();
            if (preview.isDamageableItem()) preview.setDamageValue(0);
            CatalogCategory primary = classify(preview);
            STACKS.computeIfAbsent(primary, ignored -> new ArrayList<>()).add(preview);
            ResourceLocation id = ForgeRegistries.ITEMS.getKey(preview.getItem());
            if (id != null) unique.putIfAbsent(id, preview);
        });
        STACKS.replaceAll((category, stacks) -> Collections.unmodifiableList(stacks));
        allStacks = List.copyOf(unique.values());
    }

    public static void clear() {
        STACKS.clear();
        COOKING_CATALYSTS.clear();
        allStacks = List.of();
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

    public static List<ItemStack> stacks(CatalogCategory category) {
        if (category != CatalogCategory.BUILDING) {
            return STACKS.getOrDefault(category, List.of());
        }
        return BUILDING.stream().flatMap(child -> STACKS.getOrDefault(child, List.of()).stream()).toList();
    }

    public static List<ItemStack> allStacks() {
        return allStacks;
    }

    public static CatalogCategory category(ItemStack stack) {
        return classify(stack);
    }

    private static CatalogCategory classify(ItemStack stack) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        String namespace = id == null ? "" : id.getNamespace();
        String path = id == null ? "" : id.getPath();
        if (stack.is(CREATE_REDSTONE)) {
            return CatalogCategory.CREATE_REDSTONE;
        }
        if (stack.is(NATURE)) {
            return CatalogCategory.NATURE;
        }
        if (stack.is(FISHING)) {
            return CatalogCategory.FISHING;
        }
        if (isTransport(stack)) {
            return CatalogCategory.TRANSPORT;
        }
        if (stack.getItem() instanceof SpawnEggItem) {
            return CatalogCategory.SPAWN_EGGS;
        }
        if (isFood(stack, path)) {
            return CatalogCategory.FOOD;
        }
        if (isWeapon(stack)) {
            return CatalogCategory.WEAPONS;
        }
        if (isWearable(stack)) {
            return CatalogCategory.WEARABLE;
        }
        if (isPotion(stack, path)) {
            return CatalogCategory.POTIONS;
        }
        if (isBook(stack, path)) {
            return CatalogCategory.BOOKS;
        }
        if (isForcedNature(stack, path)) {
            return CatalogCategory.NATURE;
        }
        if (isFishing(stack, namespace, path)) {
            return CatalogCategory.FISHING;
        }
        if (isCreateOrRedstone(stack, namespace, path)) {
            return CatalogCategory.CREATE_REDSTONE;
        }
        CatalogCategory building = classifyBuilding(stack, namespace, path);
        return building == null ? CatalogCategory.OTHER : building;
    }

    private static boolean isWearable(ItemStack stack) {
        if (stack.getItem() instanceof ArmorItem || stack.getItem() instanceof ElytraItem || CURIO.test(stack)) {
            return true;
        }
        var player = Minecraft.getInstance().player;
        if (player == null) {
            return false;
        }
        return stack.canEquip(EquipmentSlot.HEAD, player) || stack.canEquip(EquipmentSlot.CHEST, player)
                || stack.canEquip(EquipmentSlot.LEGS, player) || stack.canEquip(EquipmentSlot.FEET, player);
    }

    private static boolean isFishing(ItemStack stack, String namespace, String path) {
        return namespace.equals("tide") || stack.getItem() instanceof FishingRodItem || stack.is(ItemTags.FISHES)
                || containsAny(path, "fishing_rod", "fish_display", "fish_frame", "bait", "bobber", "fishing_hook");
    }

    private static boolean isFood(ItemStack stack, String path) {
        if (stack.isEdible() || COOKING_CATALYSTS.contains(stack.getItem()) || isFarming(stack, path)) {
            return true;
        }
        boolean foodTag = stack.getTags().map(tag -> tag.location().getPath().toLowerCase(Locale.ROOT))
                .anyMatch(tag -> containsAny(tag, "foods", "crops", "fruits", "vegetables", "drinks", "beverages", "dough", "flour"));
        return foodTag || containsAny(path, "drink", "juice", "tea", "coffee", "wine", "beer", "cocktail", "crop",
                "flour", "dough", "cheese", "butter", "cooking_pot", "stove", "oven", "fryer", "kettle", "brewery", "kitchen");
    }

    private static boolean isWeapon(ItemStack stack) {
        return ProgressionEnchantment.isWeaponEligible(stack)||stack.getItem() instanceof ArrowItem;
    }

    private static boolean isFarming(ItemStack stack, String path) {
        return stack.getItem() instanceof HoeItem || containsAny(path, "watering_can", "sickle", "cultivator", "fertilizer", "bone_meal");
    }

    private static boolean isBook(ItemStack stack, String path) {
        return stack.getItem() instanceof BookItem || stack.getItem() instanceof EnchantedBookItem
                || containsAny(path, "book", "tome", "guide", "journal", "manual", "codex", "grimoire");
    }

    private static boolean isPotion(ItemStack stack, String path) {
        return stack.getItem() instanceof PotionItem || containsAny(path, "potion", "elixir", "tonic", "flask");
    }

    private static boolean isTransport(ItemStack stack) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        String namespace = id == null ? "" : id.getNamespace();
        String path = id == null ? "" : id.getPath();
        if (stack.is(TRANSPORT) || stack.getItem() instanceof BoatItem || stack.getItem() instanceof MinecartItem) return true;
        if (Set.of("create", "railways", "bellsandwhistles", "create_train_parts", "createrailwaysnavigator").contains(namespace))
            return containsAny(path, "train", "station", "track", "schedule", "controls", "conductor", "locomotive", "bogey", "carriage");
        return containsAny(path, "wagon", "cart", "sled", "ship", "boat");
    }

    private static boolean isCreateOrRedstone(ItemStack stack, String namespace, String path) {
        if (Set.of("create", "railways", "bellsandwhistles", "create_train_parts", "createrailwaysnavigator").contains(namespace)) {
            return true;
        }
        return stack.is(net.minecraft.world.item.Items.REDSTONE_BLOCK) || containsAny(path, "redstone", "repeater", "comparator", "piston", "observer", "dispenser", "dropper",
                "hopper", "lever", "pressure_plate", "tripwire", "daylight_detector", "sculk_sensor", "target");
    }

    private static CatalogCategory classifyBuilding(ItemStack stack, String namespace, String path) {
        if (Set.of("moa_decor_holidays", "snowyspirit", "giftypresent").contains(namespace)
                || containsAny(path, "christmas", "halloween", "holiday", "snowman", "wreath", "garland", "pumpkin_lantern")) {
            return CatalogCategory.HOLIDAY;
        }
        if (isNature(stack, namespace, path)) {
            return CatalogCategory.NATURE;
        }
        if (containsAny(path, "chest", "barrel", "crate", "drawer", "cupboard", "cabinet", "shelf", "storage", "basket")) {
            return CatalogCategory.CONTAINERS;
        }
        if (Set.of("handcrafted", "dustydecorations", "bbb", "somemoreblocks", "another_furniture").contains(namespace)
                || containsAny(path, "chair", "table", "sofa", "couch", "stool", "bench", "lamp", "curtain", "carpet", "painting", "decoration")) {
            return CatalogCategory.FURNITURE;
        }
        return stack.getItem() instanceof BlockItem ? CatalogCategory.BUILDING_BLOCKS : null;
    }

    private static boolean isNature(ItemStack stack, String namespace, String path) {
        return namespace.equals("immersive_weathering") || stack.is(ItemTags.LEAVES)
                || containsAny(path, "leaf_pile", "leaves", "sapling", "icicle", "moss", "lichen", "ivy", "roots");
    }

    private static boolean isForcedNature(ItemStack stack, String path) {
        return stack.is(ItemTags.LEAVES) || containsAny(path, "leaf_pile", "leaves", "sapling", "icicle");
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
