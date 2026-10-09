package com.siirio.jemclaims;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

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

    public static void chest(ServerPlayer player, String title, Consumer<Buttons> populate) {
        chest(player, "menu", title, populate);
    }

    public static void chest(ServerPlayer player, String kind, String title, Consumer<Buttons> populate) {
        if (player.containerMenu instanceof Buttons existing) {
            existing.populate(kind, title, populate);
            existing.sendAllDataToRemote();
            return;
        }
        player.openMenu(new SimpleMenuProvider((id, inventory, user) -> {
            Buttons buttons = new Buttons(id, inventory);
            buttons.populate(kind, title, populate);
            return buttons;
        }, Component.translatableWithFallback("jem.ui." + kind, title)));
    }

    public static void input(ServerPlayer player, String title, String initial, Consumer<String> accept) {
        player.openMenu(new SimpleMenuProvider((id, inventory, user) -> new TextInput(id, inventory, initial, accept), Component.translatableWithFallback("jem.ui.input", title)));
    }

    public static final class Buttons extends ChestMenu {
        private final Map<Integer, Runnable> actions = new HashMap<>();
        private final SimpleContainer contents;

        private void populate(String kind, String title, Consumer<Buttons> populate) {
            actions.clear();
            contents.clearContent();
            populate.accept(this);
            for (int slot = 0; slot < contents.getContainerSize(); slot++) {
                ItemStack item = contents.getItem(slot);
                if (item.isEmpty()) continue;
                item.getOrCreateTag().putString("jem_ui_kind", kind);
                item.getOrCreateTag().putString("jem_ui_title", title);
            }
        }

        private Buttons(int id, Inventory inventory) {
            this(id, inventory, new SimpleContainer(54));
        }

        private Buttons(int id, Inventory inventory, SimpleContainer contents) {
            super(MenuType.GENERIC_9x6, id, inventory, contents, 6);
            this.contents = contents;
        }

        public void button(int slot, ItemStack icon, Runnable action) {
            icon.getOrCreateTag().putBoolean("jem_ui_action", action != null);
            contents.setItem(slot, icon);
            if (action != null) actions.put(slot, action);
        }

        @Override
        public void clicked(int slot, int button, ClickType type, Player player) {
            if (type == ClickType.PICKUP && button == 0 && actions.containsKey(slot)) actions.get(slot).run();
            if (player instanceof ServerPlayer serverPlayer) serverPlayer.containerMenu.sendAllDataToRemote();
        }

        @Override
        public ItemStack quickMoveStack(Player player, int slot) { return ItemStack.EMPTY; }

        @Override
        public boolean stillValid(Player player) { return true; }
    }

    private static final class TextInput extends AnvilMenu {
        private final Consumer<String> accept;
        private String value;

        private TextInput(int id, Inventory inventory, String initial, Consumer<String> accept) {
            super(id, inventory, ContainerLevelAccess.NULL);
            this.accept = accept;
            this.value = initial;
            inputSlots.setItem(0, icon(Items.PAPER, initial));
            refresh();
        }

        private void refresh() {
            resultSlots.setItem(0, icon(Items.LIME_DYE, "Подтвердить", value == null ? "" : value));
            setMaximumCost(0);
            broadcastChanges();
        }

        @Override
        public boolean setItemName(String name) {
            value = name;
            refresh();
            return true;
        }

        @Override
        public void createResult() {
            if (accept != null) refresh();
        }

        @Override
        public void clicked(int slot, int button, ClickType type, Player player) {
            if (slot == 2 && button == 0 && type == ClickType.PICKUP) accept.accept(value == null ? "" : value);
            if (player instanceof ServerPlayer serverPlayer) serverPlayer.containerMenu.sendAllDataToRemote();
        }

        @Override
        public ItemStack quickMoveStack(Player player, int slot) { return ItemStack.EMPTY; }

        @Override
        public void removed(Player player) {
            inputSlots.clearContent();
            resultSlots.clearContent();
            super.removed(player);
        }

        @Override
        public boolean stillValid(Player player) { return true; }
    }
}
