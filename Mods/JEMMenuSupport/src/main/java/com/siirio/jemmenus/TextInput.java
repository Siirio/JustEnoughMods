package com.siirio.jemmenus;

import java.util.function.Consumer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

final class TextInput extends AnvilMenu {
    private final Consumer<String> accept;
    private String value;

    TextInput(int id, Inventory inventory, String initial, Consumer<String> accept) {
        super(id, inventory, ContainerLevelAccess.NULL);
        this.accept = accept;
        this.value = initial;
        inputSlots.setItem(0, VanillaMenus.icon(Items.PAPER, initial));
        refresh();
    }

    private void refresh() {
        resultSlots.setItem(0, VanillaMenus.icon(Items.LIME_DYE, "Подтвердить", value == null ? "" : value));
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
