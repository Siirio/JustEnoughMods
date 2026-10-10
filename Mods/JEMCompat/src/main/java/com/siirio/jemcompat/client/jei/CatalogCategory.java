package com.siirio.jemcompat.client.jei;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

enum CatalogCategory {
    WEAPONS("gui.jemcompat.jei.weapons", "minecraft:diamond_sword"),
    WEARABLE("gui.jemcompat.jei.wearable", "minecraft:netherite_chestplate"),
    FOOD("gui.jemcompat.jei.food", "farm_and_charm:cooking_pot"),
    CREATE_REDSTONE("gui.jemcompat.jei.create_redstone", "create:cogwheel"),
    BUILDING("gui.jemcompat.jei.building", "bbb:hammer"),
    FURNITURE("gui.jemcompat.jei.furniture", "minecraft:painting"),
    NATURE("gui.jemcompat.jei.nature", "immersive_weathering:mossy_bricks"),
    CONTAINERS("gui.jemcompat.jei.containers", "minecraft:chest"),
    HOLIDAY("gui.jemcompat.jei.holiday", "minecraft:snowball"),
    BUILDING_BLOCKS("gui.jemcompat.jei.building_blocks", "minecraft:bricks"),
    FISHING("gui.jemcompat.jei.fishing", "tide:fish_display"),
    BOOKS("gui.jemcompat.jei.books", "minecraft:written_book"),
    POTIONS("gui.jemcompat.jei.potions", "minecraft:potion"),
    TRANSPORT("gui.jemcompat.jei.transport", "create:controls"),
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
        return new ItemStack(item == null ? fallbackIcon() : item);
    }

    private Item fallbackIcon() {
        return switch (this) {
            case FOOD -> net.minecraft.world.item.Items.BAKED_POTATO;
            case CREATE_REDSTONE -> net.minecraft.world.item.Items.REDSTONE;
            case BUILDING, FURNITURE -> net.minecraft.world.item.Items.PAINTING;
            case NATURE -> net.minecraft.world.item.Items.MOSS_BLOCK;
            case CONTAINERS -> net.minecraft.world.item.Items.CHEST;
            case HOLIDAY -> net.minecraft.world.item.Items.SNOWBALL;
            case BUILDING_BLOCKS -> net.minecraft.world.item.Items.BRICKS;
            case FISHING -> net.minecraft.world.item.Items.FISHING_ROD;
            case BOOKS -> net.minecraft.world.item.Items.WRITTEN_BOOK;
            case POTIONS -> net.minecraft.world.item.Items.POTION;
            case TRANSPORT -> net.minecraft.world.item.Items.MINECART;
            case SPAWN_EGGS -> net.minecraft.world.item.Items.PIG_SPAWN_EGG;
            case OTHER -> net.minecraft.world.item.Items.BUNDLE;
            case WEAPONS -> net.minecraft.world.item.Items.DIAMOND_SWORD;
            case WEARABLE -> net.minecraft.world.item.Items.NETHERITE_CHESTPLATE;
        };
    }
}
