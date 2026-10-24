package com.justenoughmods.achievementguide.client.handbook;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

enum HandbookSection {
    START("start", 0x6EBE4B, Items.OAK_SAPLING),
    BOSSES("bosses", 0xD95855, Items.NETHER_STAR),
    ENCHANTMENTS("enchantments", 0xB775D6, Items.ENCHANTED_BOOK),
    ITEMS("items", 0xE9AE45, Items.CHEST),
    MECHANICS("mechanics", 0x4EBEB4, Items.COMPARATOR),
    MANUALS("manuals", 0x6298DA, Items.WRITABLE_BOOK);

    final String path;
    final int color;
    final Item icon;
    HandbookSection(String path, int color, Item icon) {
        this.path = path;
        this.color = color;
        this.icon = icon;
    }
    Component title() { return Component.translatable("jem_advancements.handbook.section." + path); }
}
