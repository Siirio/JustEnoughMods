package com.siirio.jemcinematicinteractions;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public final class CinematicCraftingTransferMenu extends AbstractContainerMenu {
    public static final CinematicCraftingTransferMenu INSTANCE = new CinematicCraftingTransferMenu();

    private CinematicCraftingTransferMenu() {
        super(null, 0);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
