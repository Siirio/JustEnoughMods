package com.siirio.jemcompat.catalog;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.AbstractGlassBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
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
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/** Shared item-category owner for the client recipe browser and server purchase admission. */
public final class JemCatalogCategories {
    public enum Kind { WEAPONS, WEARABLE, FOOD, CREATE_REDSTONE, BUILDING, FURNITURE, NATURE, CONTAINERS,
        HOLIDAY, BUILDING_BLOCKS, GLASS, LIGHT, FISHING, BOOKS, POTIONS, TRANSPORT, SPAWN_EGGS, OTHER }
    private static final Predicate<ItemStack> CURIO = ModList.get().isLoaded("curios")
            ? new CuriosEquipment() : stack -> false;
    private static final TagKey<Item> COOKING_CATALYSTS = TagKey.create(Registries.ITEM,
            new ResourceLocation("jemcompat", "jei/cooking_catalysts"));
    private static final TagKey<Item> TRANSPORT=TagKey.create(Registries.ITEM,new ResourceLocation("jemcompat","transport"));
    private static final TagKey<Item> CREATE_REDSTONE=TagKey.create(Registries.ITEM,new ResourceLocation("jemcompat","jei/create_redstone"));
    private static final TagKey<Item> NATURE=TagKey.create(Registries.ITEM,new ResourceLocation("jemcompat","jei/nature"));
    private static final TagKey<Item> FISHING=TagKey.create(Registries.ITEM,new ResourceLocation("jemcompat","jei/fishing"));
    private static final Map<Block, Boolean> LIGHT_EMISSION = new ConcurrentHashMap<>();
    private static final Set<String> NON_BUILDING_RESOURCES = Set.of(
            "spawn:sunstone", "spawn:moonstone", "spawn:snail_shell", "minecraft:resin_clump",
            "minecraft:powder_snow_bucket", "minecraft:resin_brick");
    private static final Set<String> BEACH_FURNITURE = Set.of("palm_table", "palm_chair", "beach_chair",
            "hooded_beach_chair", "beach_sun_lounger", "palm_bar_stool", "beach_parasol",
            "beach_towel", "radio", "beach_goal");

    private JemCatalogCategories() { }

    public static void clearCaches() {
        LIGHT_EMISSION.clear();
    }

    public static boolean isMarketBuilding(ItemStack stack) {
        return switch (classify(stack, null, value -> false)) {
            case CREATE_REDSTONE, FURNITURE, NATURE, CONTAINERS, HOLIDAY, BUILDING_BLOCKS, GLASS, LIGHT -> true;
            default -> false;
        };
    }

    public static Kind classify(ItemStack stack, net.minecraft.world.entity.LivingEntity wearer, Predicate<ItemStack> cookingCatalyst) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        String namespace = id == null ? "" : id.getNamespace();
        String path = id == null ? "" : id.getPath();
        String identity = id == null ? "" : id.toString();
        if (NON_BUILDING_RESOURCES.contains(identity) || isOre(stack, path)) return Kind.OTHER;
        if (namespace.equals("infinitybuttons")) return Kind.CREATE_REDSTONE;
        if (namespace.equals("xtraarrows")) return Kind.WEAPONS;
        if (isDoll(namespace, path)) return Kind.WEARABLE;
        if (Set.of("toms_storage", "sophisticatedbackpacks", "sophisticatedstorage", "mobhome").contains(namespace)
                || identity.equals("jem_economy:shipping_box") || path.equals("backpack")) return Kind.CONTAINERS;
        if (isFoodMod(namespace) || isFoodWorkstation(stack, path)) return Kind.FOOD;
        if (Set.of("casualswing", "shutterup", "chimes", "secretdoors").contains(namespace))
            return Kind.FURNITURE;
        if (identity.equals("minecraft:tnt")) return Kind.CREATE_REDSTONE;
        if (identity.equals("animal_feeding_trough:feeding_trough") || isPlacedFood(stack, path)) return Kind.FOOD;
        if (isDisplayDecor(stack, namespace, path)) return Kind.FURNITURE;
        if (isStorage(stack, namespace, path)) return Kind.CONTAINERS;
        if (namespace.equals("beachparty") && (path.equals("palm_bar") || path.equals("mini_fridge"))) return Kind.FOOD;
        if (isLight(stack, namespace, path)) return Kind.LIGHT;
        if (isGlass(stack, path)) return Kind.GLASS;
        if (stack.is(CREATE_REDSTONE)) {
            return Kind.CREATE_REDSTONE;
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
        if (isFunctionalWorkstation(path)) return Kind.OTHER;
        if (isFishing(stack, namespace, path)) {
            return Kind.FISHING;
        }
        if (isCreateOrRedstone(stack, namespace, path)) {
            return Kind.CREATE_REDSTONE;
        }
        if (isFurniture(namespace, path) || namespace.equals("beachparty")) return Kind.FURNITURE;
        if (stack.is(NATURE) || isNature(stack, path)) return Kind.NATURE;
        if (isHoliday(namespace, path)) return Kind.HOLIDAY;
        if (isBuildingMaterial(stack, namespace, path)) return Kind.BUILDING_BLOCKS;
        return Kind.OTHER;
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
                || hasWord(path, "fishing_rod", "fish_display", "fish_frame", "bait", "bobber", "fishing_hook");
    }

    private static boolean isFood(ItemStack stack, String path, Predicate<ItemStack> cookingCatalyst) {
        if (stack.isEdible() || cookingCatalyst.test(stack) || stack.is(COOKING_CATALYSTS) || isFarming(stack, path)) {
            return true;
        }
        boolean foodTag = stack.getTags().map(tag -> tag.location().getPath().toLowerCase(Locale.ROOT))
                .anyMatch(tag -> containsAny(tag, "foods", "crops", "fruits", "vegetables", "drinks", "beverages", "dough", "flour"));
        return foodTag || hasWord(path, "drink", "juice", "tea", "coffee", "wine", "beer", "cocktail", "crop",
                "flour", "dough", "cheese", "butter", "cooking_pot", "stove", "oven", "fryer", "kettle", "brewery", "kitchen");
    }

    private static boolean isWeapon(ItemStack stack) {
        return com.siirio.jemcompat.compat.equipment.EquipmentEligibility.weapon(stack)||stack.getItem() instanceof ArrowItem;
    }

    private static boolean isFarming(ItemStack stack, String path) {
        return stack.getItem() instanceof HoeItem || hasWord(path, "watering_can", "sickle", "cultivator", "fertilizer", "bone_meal");
    }

    private static boolean isBook(ItemStack stack, String path) {
        return stack.getItem() instanceof BookItem || stack.getItem() instanceof EnchantedBookItem
                || hasWord(path, "book", "tome", "guide", "journal", "manual", "codex", "grimoire");
    }

    private static boolean isPotion(ItemStack stack, String path) {
        return stack.getItem() instanceof PotionItem || hasWord(path, "potion", "elixir", "tonic", "flask");
    }

    private static boolean isTransport(ItemStack stack) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        String namespace = id == null ? "" : id.getNamespace();
        String path = id == null ? "" : id.getPath();
        if (stack.is(TRANSPORT) || stack.getItem() instanceof BoatItem || stack.getItem() instanceof MinecartItem) return true;
        if (Set.of("create", "railways", "bellsandwhistles", "create_train_parts", "createrailwaysnavigator").contains(namespace))
            return hasWord(path, "train", "station", "track", "schedule", "controls", "conductor", "locomotive", "bogey", "carriage");
        return hasWord(path, "wagon", "cart", "sled", "ship", "boat");
    }

    private static boolean isCreateOrRedstone(ItemStack stack, String namespace, String path) {
        if (stack.is(net.minecraft.world.item.Items.REDSTONE_BLOCK)) return true;
        if (hasWord(path, "redstone", "repeater", "comparator", "piston", "observer", "dispenser", "dropper",
                "hopper", "lever", "pressure_plate", "tripwire", "daylight_detector", "sculk_sensor", "target",
                "command_block", "tnt", "button")) return true;
        return Set.of("create", "railways", "bellsandwhistles", "create_train_parts", "createrailwaysnavigator").contains(namespace)
                && hasWord(path, "cogwheel", "shaft", "gearbox", "press", "deployer", "mixer", "basin",
                        "motor", "fan", "pump", "pipe", "boiler", "flywheel", "drill", "saw", "belt",
                        "conveyor", "millstone", "crushing_wheel", "steam_engine", "steam_whistle", "mechanical");
    }

    private static boolean isOre(ItemStack stack, String path) {
        return path.endsWith("_ore") || path.contains("_ore_") || path.equals("ancient_debris")
                || stack.getTags().anyMatch(tag -> (tag.location().getNamespace().equals("forge")
                || tag.location().getNamespace().equals("c"))
                && (tag.location().getPath().equals("ores") || tag.location().getPath().startsWith("ores/")));
    }

    private static boolean isPlacedFood(ItemStack stack, String path) {
        return stack.getItem() instanceof BlockItem && (path.equals("cake") || path.endsWith("_cake"));
    }

    private static boolean isFoodMod(String namespace) {
        return namespace.startsWith("kaleidoscope_") || namespace.equals("bakery") || namespace.equals("herbalbrews");
    }

    private static boolean isFoodWorkstation(ItemStack stack, String path) {
        return stack.getItem() instanceof BlockItem && (Set.of("furnace", "blast_furnace", "smoker", "cauldron",
                "water_cauldron", "lava_cauldron", "powder_snow_cauldron", "cooking_pot", "stove", "oven",
                "grill", "spit", "smokehouse", "kitchen_furnace").contains(path)
                || hasWord(path, "cookstove", "cooking_stove", "kitchen_stove", "kitchen_oven", "roasting_spit"));
    }

    private static boolean isDoll(String namespace, String path) {
        return path.endsWith("_doll") || namespace.equals("kaleidoscope_doll") && (path.equals("custom_doll")
                || path.equals("doll_entity_item") || path.matches("doll_[0-9]+"));
    }

    private static boolean isDisplayDecor(ItemStack stack, String namespace, String path) {
        if (path.endsWith("_trophy") || path.endsWith("_skull") || path.endsWith("_skull_block")
                || namespace.equals("infinitybuttons") && path.equals("hoglin_mount_button")) return true;
        if (stack.getItem() instanceof BlockItem && namespace.equals("minecraft") && path.endsWith("_head")) return true;
        if (namespace.equals("blastfromthepast") && Set.of("antler_display", "broad_antler_display",
                "curly_antler_display", "spikey_antler_display").contains(path)) return true;
        if (namespace.equals("dustydecorations") && path.equals("displayed_oars")) return true;
        return namespace.equals("beachparty") && BEACH_FURNITURE.contains(path);
    }

    private static boolean isStorage(ItemStack stack, String namespace, String path) {
        if (namespace.equals("spawn") && Set.of("clam_case_purple", "clam_case_red", "clam_case_white",
                "clam_case_yellow").contains(path)) return true;
        if (namespace.equals("beachparty") && path.equals("palm_cabinet")) return true;
        if (!(stack.getItem() instanceof BlockItem blockItem)) return false;
        Block block = blockItem.getBlock();
        if (path.endsWith("bookshelf") && block instanceof EntityBlock) return true;
        return block instanceof ChestBlock || block instanceof BarrelBlock || block instanceof ShulkerBoxBlock
                || block instanceof EntityBlock && hasWord(path, "chest", "barrel", "crate", "drawer",
                        "cupboard", "cabinet", "storage", "basket");
    }

    private static boolean isLight(ItemStack stack, String namespace, String path) {
        if (namespace.equals("beachparty") && (path.equals("palm_torch_item") || path.equals("tall_palm_torch"))) return true;
        if (!(stack.getItem() instanceof BlockItem blockItem)) return false;
        return LIGHT_EMISSION.computeIfAbsent(blockItem.getBlock(), block -> block.getStateDefinition()
                .getPossibleStates().stream().anyMatch(state -> state.getLightEmission() > 0));
    }

    private static boolean isGlass(ItemStack stack, String path) {
        if (!(stack.getItem() instanceof BlockItem blockItem)) return false;
        if (blockItem.getBlock() instanceof AbstractGlassBlock) return true;
        if (stack.getTags().anyMatch(tag -> (tag.location().getNamespace().equals("forge")
                || tag.location().getNamespace().equals("c"))
                && hasWord(tag.location().getPath().replace('/', '_'), "glass", "glass_panes"))) return true;
        return path.equals("glass") || path.equals("glass_pane") || path.endsWith("_glass") || path.contains("_glass_")
                || path.endsWith("_seaglass") || path.endsWith("_seaglass_pane")
                || path.endsWith("_window") || path.endsWith("_window_pane");
    }

    private static boolean isFurniture(String namespace, String path) {
        return (namespace.equals("beachparty") && BEACH_FURNITURE.contains(path))
                || hasWord(path, "chair", "table", "sofa", "couch", "stool", "bench", "curtain",
                "carpet", "painting", "decoration", "display", "parasol", "towel", "radio", "urn",
                "shelf", "banner", "sign", "bed", "lamp");
    }

    private static boolean isFunctionalWorkstation(String path) {
        return Set.of("crafting_table", "smithing_table", "enchanting_table", "cartography_table",
                "fletching_table", "grindstone", "stonecutter", "loom", "anvil", "chipped_anvil",
                "damaged_anvil").contains(path);
    }

    private static boolean isNature(ItemStack stack, String path) {
        if (stack.is(ItemTags.LEAVES)) return true;
        if (stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof LeavesBlock) return true;
        return hasWord(path, "leaf_pile", "leaves", "sapling", "icicle", "moss", "lichen", "ivy",
                "roots", "sprout", "flower", "vine", "seastar", "seastars");
    }

    private static boolean isHoliday(String namespace, String path) {
        return Set.of("moa_decor_holidays", "snowyspirit", "giftypresent").contains(namespace)
                || hasWord(path, "christmas", "halloween", "holiday", "snowman", "wreath", "garland", "pumpkin_lantern");
    }

    private static boolean isBuildingMaterial(ItemStack stack, String namespace, String path) {
        if (!(stack.getItem() instanceof BlockItem blockItem)) return false;
        Block block = blockItem.getBlock();
        if (block instanceof SlabBlock || block instanceof StairBlock || block instanceof WallBlock
                || block instanceof FenceBlock || block instanceof FenceGateBlock || block instanceof RotatedPillarBlock) return true;
        if (namespace.equals("dustydecorations") && (path.endsWith("_cushion")
                || path.endsWith("_cushion_slab") || path.endsWith("_cushion_stairs"))) return true;
        if (block instanceof EntityBlock) return false;
        // Every remaining ordinary placeable block is a construction material. Functional block entities
        // retain their earlier dedicated or miscellaneous routes.
        return true;
    }

    private static boolean hasWord(String value, String... words) {
        for (String word : words) {
            if (value.equals(word) || value.startsWith(word + "_") || value.endsWith("_" + word)
                    || value.contains("_" + word + "_")) return true;
        }
        return false;
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
