package com.siirio.jemcompat.mixin.transport;

import com.siirio.jemcompat.feature.transport.BackpackPolicy;
import net.minecraft.world.inventory.ShulkerBoxSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ShulkerBoxSlot.class)
public abstract class ShulkerBoxSlotMixin {
    @Inject(method = "mayPlace", at = @At("HEAD"), cancellable = true)
    private void jemcompat$rejectBackpacks(ItemStack stack, CallbackInfoReturnable<Boolean> callback) {
        if (BackpackPolicy.isBackpack(stack)) {
            callback.setReturnValue(false);
        }
    }
}
