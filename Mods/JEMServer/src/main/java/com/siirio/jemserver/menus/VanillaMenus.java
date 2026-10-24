package com.siirio.jemserver.menus;

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
        Component[] lore = java.util.Arrays.stream(lines).map(Component::literal).toArray(Component[]::new);
        return icon(item, Component.literal(title), lore);
    }

    public static ItemStack icon(Item item, Component title, Component... lines) {
        ItemStack stack = new ItemStack(item);
        stack.setHoverName(title);
        ListTag lore = new ListTag();
        for (Component line : lines) lore.add(StringTag.valueOf(Component.Serializer.toJson(line)));
        CompoundTag display = stack.getOrCreateTagElement("display");
        display.put("Lore", lore);
        return stack;
    }

    public static void chest(ServerPlayer player, String owner, String kind, String title, Consumer<Buttons> populate) {
        chest(player, owner, kind, Component.literal(title), populate);
    }

    public static void chest(ServerPlayer player, String owner, String kind, Component title, Consumer<Buttons> populate) {
        if (player.containerMenu instanceof Buttons existing && existing.belongsTo(owner)) {
            existing.populate(kind, title, populate);
            existing.sendAllDataToRemote();
            return;
        }
        player.openMenu(new SimpleMenuProvider((id, inventory, user) -> {
            Buttons buttons = new Buttons(id, inventory, owner);
            buttons.populate(kind, title, populate);
            return buttons;
        }, title));
    }

    public static void input(ServerPlayer player, String title, String initial, Consumer<String> accept) {
        input(player, Component.literal(title), initial, accept);
    }

    public static void input(ServerPlayer player, Component title, String initial, Consumer<String> accept) {
        player.openMenu(new SimpleMenuProvider((id, inventory, user) -> new TextInput(id, inventory, initial, accept), title));
    }

}
