package com.siirio.jemcompat.feature.backpack;

import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import com.mrcrayfish.backpacked.inventory.BackpackInventory;

/** Ingredients are owned by their backpack item, never by a detached server cache. */
public final class BackpackCraftingGrid extends TransientCraftingContainer {
    public static final String KEY = "JemWorkbenchIngredients";
    private final ItemStack backpack;
    private final AbstractContainerMenu owner;
    private final Player player;

    public BackpackCraftingGrid(AbstractContainerMenu menu, ItemStack backpack, Player player) {
        super(menu, 3, 3, readIngredients(backpack));
        this.backpack = backpack;
        this.owner = menu;
        this.player = player;
        if (!player.level().isClientSide) persist();
    }

    @Override public void setChanged() { if (owner != null) owner.slotsChanged(this); }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return BackpackInventory.isAllowedItem(stack);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        // ResultSlot also inserts crafting remainders through this container seam.
        if (!stack.isEmpty() && !canPlaceItem(slot, stack)) {
            returnToPlayer(stack);
            return;
        }
        super.setItem(slot, stack);
    }

    private void returnToPlayer(ItemStack stack) {
        if (player.level().isClientSide || stack.isEmpty()) return;
        ItemStack returned = stack.copy();
        if (!player.getInventory().add(returned)) player.drop(returned, false);
    }

    private static NonNullList<ItemStack> readIngredients(ItemStack backpack) {
        var items = NonNullList.withSize(9, ItemStack.EMPTY);
        if (!backpack.isEmpty()) ContainerHelper.loadAllItems(backpack.getOrCreateTag().getCompound(KEY), items);
        return items;
    }

    public void persist() {
        if (backpack.isEmpty()) return;
        // Existing NBT and direct in-place stack mutations must obey the same native admission.
        // Removal precedes return, so a prohibited ingredient has exactly one live owner.
        for (int i = 0; i < 9; i++) {
            ItemStack stack = getItem(i);
            if (!stack.isEmpty() && !canPlaceItem(i, stack)) returnToPlayer(super.removeItemNoUpdate(i));
        }
        var items = NonNullList.withSize(9, ItemStack.EMPTY);
        for (int i = 0; i < 9; i++) items.set(i, getItem(i));
        CompoundTag tag = new CompoundTag();
        ContainerHelper.saveAllItems(tag, items);
        backpack.getOrCreateTag().put(KEY, tag);
    }
}
