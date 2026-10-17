package com.siirio.jemeconomy.merchant;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MerchantContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Display-only result: the native menu routes all monetary takes to settlement. */
public final class NativeCoinResultSlot extends Slot {
    public NativeCoinResultSlot(MerchantContainer container) { super(container, 2, 220, 37); }
    @Override public boolean mayPlace(ItemStack stack) { return false; }
    @Override public boolean mayPickup(Player player) { return false; }
    @Override public ItemStack remove(int count) { return ItemStack.EMPTY; }
}
