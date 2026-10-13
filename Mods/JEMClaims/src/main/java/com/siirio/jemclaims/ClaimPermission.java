package com.siirio.jemclaims;

import java.util.EnumSet;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public enum ClaimPermission {
    BREAK("break", Items.DIAMOND_PICKAXE, false, true),
    PLACE("place", Items.GRASS_BLOCK, false, true),
    OPENCONTAINER("open_container", Items.CHEST, false, true),
    INTERACTBLOCK("interact_block", Items.BOOK, false, true),
    BED("bed", Items.RED_BED, false, true),
    ANVIL("anvil", Items.ANVIL, false, true),
    BEACON("beacon", Items.BEACON, false, true),
    ENCHANTMENT("enchantment", Items.ENCHANTING_TABLE, false, true),
    NOTEBLOCK("noteblock", Items.NOTE_BLOCK, false, true),
    JUKEBOX("jukebox", Items.JUKEBOX, false, true),
    BUCKET("bucket", Items.BUCKET, false, true),
    SIGN("interact_sign", Items.OAK_SIGN, false, true),
    DOOR("door", Items.OAK_DOOR, false, true),
    FENCEGATE("fence_gate", Items.OAK_FENCE_GATE, false, true),
    TRAPDOOR("trapdoor", Items.OAK_TRAPDOOR, false, true),
    BUTTONLEVER("button_lever", Items.LEVER, false, true),
    PRESSUREPLATE("pressure_plate", Items.STONE_PRESSURE_PLATE, false, true),
    REDSTONE("redstone", Items.REDSTONE, false, true),
    ANIMALINTERACT("animal_interact", Items.COW_SPAWN_EGG, false, true),
    HURTANIMAL("hurt_animal", Items.IRON_SWORD, false, false),
    CANSTAY("can_stay", Items.LEATHER_BOOTS, false, true),
    HURTPLAYER("hurt_player", Items.DIAMOND_SWORD, false, false),
    EXPLOSIONS("explosions", Items.TNT, true, false),
    FIRESPREAD("fire_spread", Items.FLINT_AND_STEEL, true, false),
    WATERBORDER("water_border", Items.WATER_BUCKET, true, false),
    PISTONBORDER("piston_border", Items.PISTON, true, false),
    CREATE("create_contraption", Items.MINECART, false, false);

    public final String path;
    public final Item icon;
    public final boolean global;
    public final boolean memberDefault;
    private static final EnumSet<ClaimPermission> ALWAYS_ALLOWED = EnumSet.of(
            ANVIL, ENCHANTMENT, NOTEBLOCK, DOOR, FENCEGATE, TRAPDOOR, BUTTONLEVER);

    ClaimPermission(String path, Item icon, boolean global, boolean memberDefault) {
        this.path = path;
        this.icon = icon;
        this.global = global;
        this.memberDefault = memberDefault;
    }

    public boolean alwaysAllowed() {
        return ALWAYS_ALLOWED.contains(this);
    }
}
