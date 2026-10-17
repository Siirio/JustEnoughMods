package com.siirio.jemsettlements.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;

/** Keep native translatable names intact until the receiving client's language is known. */
final class SettlementNames {
    private SettlementNames() { }
    static void project(CompoundTag row, Entity entity, String saved, String translation) {
        // Villager Names and name tags populate the native CustomName; preserve their actual component.
        Component name = entity == null ? null : entity.getCustomName() != null ? entity.getCustomName() : entity.getName();
        if (name == null && saved != null) name = Component.Serializer.fromJson(saved);
        if (name == null) name = Component.translatable(translation);
        row.putString("NameComponent", Component.Serializer.toJson(name));
        row.putString("NameKey", translation);
        row.putString("Name", name.getString());
    }
    static String entityKey(net.minecraft.resources.ResourceLocation type) {
        return "entity." + type.getNamespace() + "." + type.getPath();
    }
}
