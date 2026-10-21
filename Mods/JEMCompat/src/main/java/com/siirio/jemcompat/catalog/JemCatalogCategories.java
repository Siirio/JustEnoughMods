package com.siirio.jemcompat.catalog;

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


import java.util.Locale;
import java.util.Set;

/** Shared item-category owner for the client recipe browser and server purchase admission. */
public final class JemCatalogCategories {
    public enum Kind { WEAPONS, WEARABLE, FOOD, CREATE_REDSTONE, BUILDING, FURNITURE, NATURE, CONTAINERS,
        HOLIDAY, BUILDING_BLOCKS, FISHING, BOOKS, POTIONS, TRANSPORT, SPAWN_EGGS, OTHER }
    private static final Predicate<ItemStack> CURIO = ModList.get().isLoaded("curios")
            ? new CuriosEquipment() : stack -> false;
    private static final TagKey<Item> COOKING_CATALYSTS = TagKey.create(Registries.ITEM,
            new ResourceLocation("jemcompat", "jei/cooking_catalysts"));
    private static final TagKey<Item> TRANSPORT=TagKey.create(Registries.ITEM,new ResourceLocation("jemcompat","transport"));
    private static final TagKey<Item> CREATE_REDSTONE=TagKey.create(Registries.ITEM,new ResourceLocation("jemcompat","jei/create_redstone"));
    private static final TagKey<Item> NATURE=TagKey.create(Registries.ITEM,new ResourceLocation("jemcompat","jei/nature"));
    private static final TagKey<Item> FISHING=TagKey.create(Registries.ITEM,new ResourceLocation("jemcompat","jei/fishing"));

    private JemCatalogCategories() { }

    public static boolean isMarketBuilding(ItemStack stack) {
        return switch (classify(stack, null, value -> false)) {
            case CREATE_REDSTONE, FURNITURE, NATURE, CONTAINERS, HOLIDAY, BUILDING_BLOCKS -> true;
            default -> false;
        };
    }

    public static Kind classify(ItemStack stack, net.minecraft.world.entity.LivingEntity wearer, Predicate<ItemStack> cookingCatalyst) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        String namespace = id == null ? "" : id.getNamespace();
        String path = id == null ? "" : id.getPath();
        if (stack.is(CREATE_REDSTONE)) {
            return Kind.CREATE_REDSTONE;
        }
        if (stack.is(NATURE)) {
            return Kind.NATURE;
        }
        if (stack.is(FISHING)) {
            return Kind.FISHING;
        }
        if (isTransport(stack)) {
            return Kind.TRANSPORT;
        }
        if (stack.getItem() instanceof SpawnEggItem) {
            return Kind.SPAWN_EGGS;
        }
        if (isFood(stack, path, cookingCatalyst)) {
            return Kind.FOOD;
        }
        if (isWeapon(stack)) {
            return Kind.WEAPONS;
        }
        if (isWearable(stack, wearer)) {
            return Kind.WEARABLE;
        }
        if (isPotion(stack, path)) {
            return Kind.POTIONS;
        }
        if (isBook(stack, path)) {
            return Kind.BOOKS;
        }
        if (isForcedNature(stack, path)) {
            return Kind.NATURE;
        }
        if (isFishing(stack, namespace, path)) {
            return Kind.FISHING;
        }
        if (isCreateOrRedstone(stack, namespace, path)) {
            return Kind.CREATE_REDSTONE;
        }
        Kind building = classifyBuilding(stack, namespace, path);
        return building == null ? Kind.OTHER : building;
    }

    private static boolean isWearable(ItemStack stack, net.minecraft.world.entity.LivingEntity wearer) {
        if (stack.getItem() instanceof ArmorItem || stack.getItem() instanceof ElytraItem || CURIO.test(stack)) {
            return true;
        }
        if (wearer == null) {
            var equipable = net.minecraft.world.item.Equipable.get(stack);
            return equipable != null && equipable.getEquipmentSlot().getType() == EquipmentSlot.Type.ARMOR;
        }
        return stack.canEquip(EquipmentSlot.HEAD, wearer) || stack.canEquip(EquipmentSlot.CHEST, wearer)
                || stack.canEquip(EquipmentSlot.LEGS, wearer) || stack.canEquip(EquipmentSlot.FEET, wearer);
    }

    private static boolean isFishing(ItemStack stack, String namespace, String path) {
        return namespace.equals("tide") || stack.getItem() instanceof FishingRodItem || stack.is(ItemTags.FISHES)
                || containsAny(path, "fishing_rod", "fish_display", "fish_frame", "bait", "bobber", "fishing_hook");
    }

    private static boolean isFood(ItemStack stack, String path, Predicate<ItemStack> cookingCatalyst) {
        if (stack.isEdible() || cookingCatalyst.test(stack) || stack.is(COOKING_CATALYSTS) || isFarming(stack, path)) {
            return true;
        }
        boolean foodTag = stack.getTags().map(tag -> tag.location().getPath().toLowerCase(Locale.ROOT))
                .anyMatch(tag -> containsAny(tag, "foods", "crops", "fruits", "vegetables", "drinks", "beverages", "dough", "flour"));
        return foodTag || containsAny(path, "drink", "juice", "tea", "coffee", "wine", "beer", "cocktail", "crop",
                "flour", "dough", "cheese", "butter", "cooking_pot", "stove", "oven", "fryer", "kettle", "brewery", "kitchen");
    }

    private static boolean isWeapon(ItemStack stack) {
        return com.siirio.jemcompat.compat.equipment.EquipmentEligibility.weapon(stack)||stack.getItem() instanceof ArrowItem;
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

    private static Kind classifyBuilding(ItemStack stack, String namespace, String path) {
        if (Set.of("moa_decor_holidays", "snowyspirit", "giftypresent").contains(namespace)
                || containsAny(path, "christmas", "halloween", "holiday", "snowman", "wreath", "garland", "pumpkin_lantern")) {
            return Kind.HOLIDAY;
        }
        if (isNature(stack, namespace, path)) {
            return Kind.NATURE;
        }
        if (containsAny(path, "chest", "barrel", "crate", "drawer", "cupboard", "cabinet", "shelf", "storage", "basket")) {
            return Kind.CONTAINERS;
        }
        if (Set.of("handcrafted", "dustydecorations", "bbb", "somemoreblocks", "another_furniture").contains(namespace)
                || containsAny(path, "chair", "table", "sofa", "couch", "stool", "bench", "lamp", "curtain", "carpet", "painting", "decoration")) {
            return Kind.FURNITURE;
        }
        return stack.getItem() instanceof BlockItem ? Kind.BUILDING_BLOCKS : null;
    }

    private static boolean isNature(ItemStack stack, String namespace, String path) {
        return namespace.equals("immersive_weathering") || stack.is(ItemTags.LEAVES)
                || containsAny(path, "leaf_pile", "leaves", "sapling", "icicle", "moss", "lichen", "ivy", "roots");
    }

    private static boolean isForcedNature(ItemStack stack, String path) {
        return stack.is(ItemTags.LEAVES) || containsAny(path, "leaf_pile", "leaves", "sapling", "icicle");
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
