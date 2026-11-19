package com.siirio.jemcompat.mixin.transport;

import com.siirio.jemcompat.feature.transport.ShulkerCapacity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ShulkerBoxMenu.class)
public abstract class ShulkerBoxMenuMixin extends AbstractContainerMenu {
    private static final int SHULKER_SLOTS = 27;

    protected ShulkerBoxMenuMixin(MenuType<?> menuType, int containerId) {
        super(menuType, containerId);
    }

    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
    private void jemcompat$moveWideBlockStack(Player player, int index,
            CallbackInfoReturnable<ItemStack> callback) {
        AbstractContainerMenu menu = (AbstractContainerMenu) (Object) this;
        if (index < 0 || index >= menu.slots.size()) {
            callback.setReturnValue(ItemStack.EMPTY);
            return;
        }
        Slot source = menu.slots.get(index);
        if (!source.hasItem()) {
            callback.setReturnValue(ItemStack.EMPTY);
            return;
        }
        ItemStack moving = source.getItem();
        if (index >= SHULKER_SLOTS && !ShulkerCapacity.isEligibleBlockStack(moving)) {
            return;
        }
        ItemStack original = moving.copy();
        boolean moved = index < SHULKER_SLOTS
                ? moveItemStackTo(moving, SHULKER_SLOTS, menu.slots.size(), true)
                : moveIntoShulker(menu, moving);
        if (!moved) {
            callback.setReturnValue(ItemStack.EMPTY);
            return;
        }
        if (moving.isEmpty()) {
            source.setByPlayer(ItemStack.EMPTY);
        } else {
            source.setChanged();
        }
        if (moving.getCount() == original.getCount()) {
            callback.setReturnValue(ItemStack.EMPTY);
            return;
        }
        callback.setReturnValue(original);
    }

    private static boolean moveIntoShulker(AbstractContainerMenu menu, ItemStack moving) {
        boolean changed = false;
        for (int slotIndex = 0; slotIndex < SHULKER_SLOTS && !moving.isEmpty(); slotIndex++) {
            Slot slot = menu.slots.get(slotIndex);
            ItemStack current = slot.getItem();
            if (current.isEmpty() || !ItemStack.isSameItemSameTags(current, moving)) {
                continue;
            }
            int transfer = Math.min(moving.getCount(), ShulkerCapacity.BLOCK_STACK_LIMIT - current.getCount());
            if (transfer <= 0) {
                continue;
            }
            moving.shrink(transfer);
            current.grow(transfer);
            slot.setChanged();
            changed = true;
        }
        for (int slotIndex = 0; slotIndex < SHULKER_SLOTS && !moving.isEmpty(); slotIndex++) {
            Slot slot = menu.slots.get(slotIndex);
            if (slot.hasItem() || !slot.mayPlace(moving)) {
                continue;
            }
            int transfer = Math.min(moving.getCount(), ShulkerCapacity.BLOCK_STACK_LIMIT);
            slot.setByPlayer(moving.split(transfer));
            slot.setChanged();
            changed = true;
        }
        return changed;
    }
}
