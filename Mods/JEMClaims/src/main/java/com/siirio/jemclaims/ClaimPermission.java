package com.siirio.jemclaims;

import java.util.EnumSet;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public enum ClaimPermission {
    BREAK("break", "Ломать блоки", Items.DIAMOND_PICKAXE, false, true),
    PLACE("place", "Ставить блоки", Items.GRASS_BLOCK, false, true),
    OPENCONTAINER("open_container", "Контейнеры", Items.CHEST, false, true),
    INTERACTBLOCK("interact_block", "Взаимодействие с блоками", Items.BOOK, false, true),
    BED("bed", "Кровати", Items.RED_BED, false, true),
    ANVIL("anvil", "Наковальни", Items.ANVIL, false, true),
    BEACON("beacon", "Маяки", Items.BEACON, false, true),
    ENCHANTMENT("enchantment", "Зачарование", Items.ENCHANTING_TABLE, false, true),
    NOTEBLOCK("noteblock", "Нотные блоки", Items.NOTE_BLOCK, false, true),
    JUKEBOX("jukebox", "Проигрыватели", Items.JUKEBOX, false, true),
    BUCKET("bucket", "Вёдра", Items.BUCKET, false, true),
    SIGN("interact_sign", "Таблички", Items.OAK_SIGN, false, true),
    DOOR("door", "Двери", Items.OAK_DOOR, false, true),
    FENCEGATE("fence_gate", "Калитки", Items.OAK_FENCE_GATE, false, true),
    TRAPDOOR("trapdoor", "Люки", Items.OAK_TRAPDOOR, false, true),
    BUTTONLEVER("button_lever", "Кнопки и рычаги", Items.LEVER, false, true),
    PRESSUREPLATE("pressure_plate", "Нажимные плиты", Items.STONE_PRESSURE_PLATE, false, true),
    REDSTONE("redstone", "Редстоун", Items.REDSTONE, false, true),
    ANIMALINTERACT("animal_interact", "Взаимодействие с животными", Items.COW_SPAWN_EGG, false, true),
    HURTANIMAL("hurt_animal", "Урон животным", Items.IRON_SWORD, false, false),
    CANSTAY("can_stay", "Вход на территорию", Items.LEATHER_BOOTS, false, true),
    HURTPLAYER("hurt_player", "PvP", Items.DIAMOND_SWORD, false, false),
    EXPLOSIONS("explosions", "Взрывы", Items.TNT, true, false),
    FIRESPREAD("fire_spread", "Распространение огня", Items.FLINT_AND_STEEL, true, false),
    WATERBORDER("water_border", "Жидкости через границу", Items.WATER_BUCKET, true, false),
    PISTONBORDER("piston_border", "Поршни через границу", Items.PISTON, true, false),
    CREATE("create_contraption", "Конструкции через границу", Items.MINECART, false, false);

    public final String path;
    public final String label;
    public final Item icon;
    public final boolean global;
    public final boolean memberDefault;
    private static final EnumSet<ClaimPermission> ALWAYS_ALLOWED = EnumSet.of(
            ANVIL, ENCHANTMENT, NOTEBLOCK, DOOR, FENCEGATE, TRAPDOOR, BUTTONLEVER);

    ClaimPermission(String path, String label, Item icon, boolean global, boolean memberDefault) {
        this.path = path;
        this.label = label;
        this.icon = icon;
        this.global = global;
        this.memberDefault = memberDefault;
    }

    public boolean alwaysAllowed() {
        return ALWAYS_ALLOWED.contains(this);
    }
}
