package com.siirio.jemmenus;

import java.util.function.Consumer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class VanillaMenus {
    private VanillaMenus() {}

    public static ItemStack icon(Item item, String title, String... lines) {
        ItemStack stack = new ItemStack(item);
        stack.setHoverName(Component.literal(title));
        ListTag lore = new ListTag();
        for (String line : lines) lore.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal(line))));
        CompoundTag display = stack.getOrCreateTagElement("display");
        display.put("Lore", lore);
        return stack;
    }

    public static void chest(ServerPlayer player, String owner, String kind, String title, Consumer<Buttons> populate) {
        if (player.containerMenu instanceof Buttons existing && existing.belongsTo(owner)) {
            existing.populate(kind, title, populate);
            existing.sendAllDataToRemote();
            return;
        }
        player.openMenu(new SimpleMenuProvider((id, inventory, user) -> {
            Buttons buttons = new Buttons(id, inventory, owner);
            buttons.populate(kind, title, populate);
            return buttons;
        }, Component.translatableWithFallback("jem.ui." + kind, title)));
    }

    public static void input(ServerPlayer player, String title, String initial, Consumer<String> accept) {
        player.openMenu(new SimpleMenuProvider((id, inventory, user) -> new TextInput(id, inventory, initial, accept), Component.translatableWithFallback("jem.ui.input", title)));
    }

}
