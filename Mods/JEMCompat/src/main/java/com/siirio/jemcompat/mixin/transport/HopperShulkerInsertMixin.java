package com.siirio.jemcompat.mixin.transport;

import com.siirio.jemcompat.feature.transport.ShulkerCapacity;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HopperBlockEntity.class)
public abstract class HopperShulkerInsertMixin {
    @Inject(method = "tryMoveInItem", at = @At("HEAD"), cancellable = true)
    private static void jemcompat$mergeWideShulkerStack(Container source, Container destination,
            ItemStack incoming, int slot, Direction direction, CallbackInfoReturnable<ItemStack> callback) {
        if (!(destination instanceof ShulkerBoxBlockEntity shulker)
                || !ShulkerCapacity.isEligibleBlockStack(incoming)) {
            return;
        }
        ItemStack current = shulker.getItem(slot);
        if (current.isEmpty() || !ItemStack.isSameItemSameTags(current, incoming)
                || current.getCount() >= ShulkerCapacity.BLOCK_STACK_LIMIT
                || !shulker.canPlaceItem(slot, incoming)
                || direction != null && !shulker.canPlaceItemThroughFace(slot, incoming, direction)) {
            return;
        }
        int transfer = Math.min(incoming.getCount(), ShulkerCapacity.BLOCK_STACK_LIMIT - current.getCount());
        if (transfer <= 0) {
            return;
        }
        incoming.shrink(transfer);
        current.grow(transfer);
        shulker.setChanged();
        callback.setReturnValue(incoming);
    }
}
