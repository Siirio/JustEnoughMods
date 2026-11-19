package com.siirio.jemcompat.mixin.transport;

import com.siirio.jemcompat.feature.transport.ShulkerCapacity;
import net.minecraft.world.inventory.ShulkerBoxSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Slot.class)
public abstract class ShulkerSlotLimitMixin {
    @Inject(method = "getMaxStackSize()I", at = @At("HEAD"), cancellable = true)
    private void jemcompat$wideShulkerSlot(CallbackInfoReturnable<Integer> callback) {
        if ((Object) this instanceof ShulkerBoxSlot) {
            callback.setReturnValue(ShulkerCapacity.BLOCK_STACK_LIMIT);
        }
    }

    @Inject(method = "getMaxStackSize(Lnet/minecraft/world/item/ItemStack;)I", at = @At("HEAD"), cancellable = true)
    private void jemcompat$wideShulkerBlockSlot(ItemStack stack, CallbackInfoReturnable<Integer> callback) {
        if ((Object) this instanceof ShulkerBoxSlot && ShulkerCapacity.isEligibleBlockStack(stack)) {
            callback.setReturnValue(ShulkerCapacity.BLOCK_STACK_LIMIT);
        }
    }
}
