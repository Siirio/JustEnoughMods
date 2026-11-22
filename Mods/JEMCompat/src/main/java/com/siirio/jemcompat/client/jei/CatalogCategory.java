package com.siirio.jemcompat.client.jei;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

enum CatalogCategory {
    WEAPONS("gui.jemcompat.jei.weapons", "minecraft:diamond_sword"),
    WEAPON_TOTEMS("gui.jemcompat.jei.weapon_totems", "minecraft:totem_of_undying"),
    WEAPON_RANGED("gui.jemcompat.jei.weapon_ranged", "minecraft:bow"),
    WEAPON_MELEE("gui.jemcompat.jei.weapon_melee", "minecraft:iron_sword"),
    WEAPON_SHIELDS("gui.jemcompat.jei.weapon_shields", "minecraft:shield"),
    WEAPON_TRAINING("gui.jemcompat.jei.weapon_training", "recruits:recruit_block"),
    WEARABLES("gui.jemcompat.jei.wearables", "minecraft:netherite_chestplate"),
    WEARABLE_ARMOR("gui.jemcompat.jei.wearable_armor", "minecraft:diamond_chestplate"),
    WEARABLE_MOB_ARMOR("gui.jemcompat.jei.wearable_mob_armor", "minecraft:diamond_horse_armor"),
    WEARABLE_DOLLS("gui.jemcompat.jei.wearable_dolls", "kaleidoscope_doll:doll_entity_item"),
    FOOD("gui.jemcompat.jei.food", "farm_and_charm:cooking_pot"),
    FOOD_EDIBLES("gui.jemcompat.jei.food_edibles", "minecraft:bread"),
    FOOD_COOKING_EQUIPMENT("gui.jemcompat.jei.food_cooking_equipment", "farm_and_charm:cooking_pot"),
    ORES("gui.jemcompat.jei.ores", "minecraft:raw_iron"),
    CREATE_REDSTONE("gui.jemcompat.jei.create_redstone", "create:cogwheel"),
    BUILDING("gui.jemcompat.jei.building", "jem_building:builder_hammer"),
    BUILDING_FURNITURE("gui.jemcompat.jei.furniture", "minecraft:painting"),
    BUILDING_NATURE("gui.jemcompat.jei.nature", "minecraft:moss_block"),
    BUILDING_CONTAINERS("gui.jemcompat.jei.containers", "minecraft:chest"),
    BUILDING_HOLIDAY("gui.jemcompat.jei.holiday", "minecraft:snowball"),
    BUILDING_BLOCKS("gui.jemcompat.jei.building_blocks", "minecraft:bricks"),
    BUILDING_GLASS("gui.jemcompat.jei.glass", "minecraft:glass"),
    BUILDING_LIGHT("gui.jemcompat.jei.light", "minecraft:lantern"),
    BUILDING_TOOLS("gui.jemcompat.jei.building_tools", "jem_building:builder_hammer"),
    FISHING("gui.jemcompat.jei.fishing", "tide:fish_display"),
    BOOKS("gui.jemcompat.jei.books", "minecraft:written_book"),
    POTIONS("gui.jemcompat.jei.potions", "minecraft:potion"),
    TRANSPORT("gui.jemcompat.jei.transport", "create:controls"),
    COMPANIONS("gui.jemcompat.jei.companions", "minecraft:wolf_spawn_egg"),
    SPAWN_EGGS("gui.jemcompat.jei.spawn_eggs", "minecraft:pig_spawn_egg"),
    OTHER("gui.jemcompat.jei.other", "minecraft:bundle");

    private final String translationKey;
    private final String iconId;

    CatalogCategory(String translationKey, String iconId) {
        this.translationKey = translationKey;
        this.iconId = iconId;
    }

    public String translationKey() {
        return translationKey;
    }

    public ItemStack icon() {
        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(iconId));
        if ((item == null || item == net.minecraft.world.item.Items.AIR) && this == BUILDING) {
            item = ForgeRegistries.ITEMS.getValue(new ResourceLocation("bbb", "hammer"));
        }
        return new ItemStack(item == null ? fallbackIcon() : item);
    }

    private Item fallbackIcon() {
        return switch (this) {
            case FOOD, FOOD_EDIBLES -> net.minecraft.world.item.Items.BAKED_POTATO;
            case FOOD_COOKING_EQUIPMENT -> net.minecraft.world.item.Items.FURNACE;
            case ORES -> net.minecraft.world.item.Items.RAW_IRON;
            case CREATE_REDSTONE -> net.minecraft.world.item.Items.REDSTONE;
            case BUILDING, BUILDING_FURNITURE -> net.minecraft.world.item.Items.PAINTING;
            case BUILDING_NATURE -> net.minecraft.world.item.Items.MOSS_BLOCK;
            case BUILDING_CONTAINERS -> net.minecraft.world.item.Items.CHEST;
            case BUILDING_HOLIDAY -> net.minecraft.world.item.Items.SNOWBALL;
            case BUILDING_BLOCKS -> net.minecraft.world.item.Items.BRICKS;
            case BUILDING_GLASS -> net.minecraft.world.item.Items.GLASS;
            case BUILDING_LIGHT -> net.minecraft.world.item.Items.LANTERN;
            case BUILDING_TOOLS -> net.minecraft.world.item.Items.IRON_PICKAXE;
            case FISHING -> net.minecraft.world.item.Items.FISHING_ROD;
            case BOOKS -> net.minecraft.world.item.Items.WRITTEN_BOOK;
            case POTIONS -> net.minecraft.world.item.Items.POTION;
            case TRANSPORT -> net.minecraft.world.item.Items.MINECART;
            case COMPANIONS -> net.minecraft.world.item.Items.WOLF_SPAWN_EGG;
            case SPAWN_EGGS -> net.minecraft.world.item.Items.PIG_SPAWN_EGG;
            case OTHER -> net.minecraft.world.item.Items.BUNDLE;
            case WEAPONS -> net.minecraft.world.item.Items.DIAMOND_SWORD;
            case WEAPON_TOTEMS -> net.minecraft.world.item.Items.TOTEM_OF_UNDYING;
            case WEAPON_RANGED -> net.minecraft.world.item.Items.BOW;
            case WEAPON_MELEE -> net.minecraft.world.item.Items.IRON_SWORD;
            case WEAPON_SHIELDS -> net.minecraft.world.item.Items.SHIELD;
            case WEAPON_TRAINING -> net.minecraft.world.item.Items.ARMOR_STAND;
            case WEARABLES -> net.minecraft.world.item.Items.NETHERITE_CHESTPLATE;
            case WEARABLE_ARMOR -> net.minecraft.world.item.Items.DIAMOND_CHESTPLATE;
            case WEARABLE_MOB_ARMOR -> net.minecraft.world.item.Items.DIAMOND_HORSE_ARMOR;
            case WEARABLE_DOLLS -> net.minecraft.world.item.Items.ARMOR_STAND;
        };
    }
}
