package com.siirio.jemcompat.mixin.transport;

import com.siirio.jemcompat.feature.transport.ShulkerCapacity;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraftforge.items.wrapper.SidedInvWrapper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = SidedInvWrapper.class, remap = false)
public abstract class SidedShulkerHandlerMixin {
    @Shadow @Final protected WorldlyContainer inv;
    @Shadow @Final protected net.minecraft.core.Direction side;

    @Inject(method = "insertItem", at = @At("HEAD"), cancellable = true, remap = false)
    private void jemcompat$insertWideShulkerStack(int slot, ItemStack incoming, boolean simulate,
            CallbackInfoReturnable<ItemStack> callback) {
        if (!(inv instanceof ShulkerBoxBlockEntity) || !ShulkerCapacity.isEligibleBlockStack(incoming)) {
            return;
        }
        int actualSlot = SidedInvWrapper.getSlot(inv, slot, side);
        if (actualSlot < 0 || !inv.canPlaceItemThroughFace(actualSlot, incoming, side)
                || !inv.canPlaceItem(actualSlot, incoming)) {
            callback.setReturnValue(incoming);
            return;
        }
        ItemStack current = inv.getItem(actualSlot);
        if (!current.isEmpty() && !ItemStack.isSameItemSameTags(current, incoming)) {
            callback.setReturnValue(incoming);
            return;
        }
        int room = ShulkerCapacity.BLOCK_STACK_LIMIT - current.getCount();
        int transfer = Math.min(room, incoming.getCount());
        if (transfer <= 0) {
            callback.setReturnValue(incoming);
            return;
        }
        ItemStack remainder = incoming.copy();
        remainder.shrink(transfer);
        if (!simulate) {
            ItemStack inserted = current.isEmpty() ? incoming.copyWithCount(transfer) : current.copy();
            if (!current.isEmpty()) {
                inserted.grow(transfer);
            }
            inv.setItem(actualSlot, inserted);
            inv.setChanged();
        }
        callback.setReturnValue(remainder);
    }
}
