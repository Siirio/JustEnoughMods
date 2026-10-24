package com.siirio.jemserver.menus;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;

/** Menu titles cross the server boundary as components, before language resolution. */
public final class MenuTitles {
    private static final String TITLE = "jem_ui_title_component";
    private static final String LEGACY_TITLE = "jem_ui_title";
    private static final int MAX_LENGTH = 32767;

    private MenuTitles() {}

    public static void write(CompoundTag tag, Component title) {
        String encoded = Component.Serializer.toJson(title);
        bounded(encoded);
        tag.putString(TITLE, encoded);
        tag.remove(LEGACY_TITLE);
    }

    public static Component read(CompoundTag tag) {
        if (!tag.contains(TITLE)) {
            if (!tag.contains(LEGACY_TITLE)) return invalid();
            if (tag.contains(LEGACY_TITLE) && !tag.contains(LEGACY_TITLE, Tag.TAG_STRING)) return invalid();
            String literal = tag.getString(LEGACY_TITLE);
            return literal.length() > MAX_LENGTH ? invalid() : Component.literal(literal);
        }
        if (!tag.contains(TITLE, Tag.TAG_STRING)) return invalid();
        String encoded = tag.getString(TITLE);
        if (encoded.length() > MAX_LENGTH) return invalid();
        try {
            Component title = Component.Serializer.fromJson(encoded);
            return title == null ? invalid() : title;
        } catch (RuntimeException malformed) {
            return invalid();
        }
    }

    private static Component invalid() { return Component.translatable("jem.ui.invalid_title"); }

    private static void bounded(String value) {
        if (value.length() > MAX_LENGTH) throw new IllegalArgumentException("Menu title exceeds metadata limit");
    }
}
