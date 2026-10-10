package com.siirio.jemmenus;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

public final class Buttons extends ChestMenu {
    private final Map<Integer, Runnable> actions = new HashMap<>();
    private final SimpleContainer contents;
    private final String owner;

    void populate(String kind, String title, Consumer<Buttons> populate) {
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

    Buttons(int id, Inventory inventory, String owner) {
        this(id, inventory, new SimpleContainer(54), owner);
    }

    private Buttons(int id, Inventory inventory, SimpleContainer contents, String owner) {
        super(MenuType.GENERIC_9x6, id, inventory, contents, 6);
        this.contents = contents;
        this.owner = owner;
    }

    boolean belongsTo(String owner) {
        return this.owner.equals(owner);
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
