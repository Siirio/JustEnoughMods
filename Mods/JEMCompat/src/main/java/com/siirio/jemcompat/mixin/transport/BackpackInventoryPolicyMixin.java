package com.siirio.jemcompat.mixin.transport;

import com.siirio.jemcompat.feature.transport.BackpackPolicy;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackInventoryHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = BackpackInventoryHandler.class, remap = false)
public abstract class BackpackInventoryPolicyMixin {
    @Inject(method = "isAllowed", at = @At("HEAD"), cancellable = true)
    private void jemcompat$rejectPortableContainers(ItemStack stack, CallbackInfoReturnable<Boolean> callback) {
        if (BackpackPolicy.isPortableContainer(stack)) {
            callback.setReturnValue(false);
        }
    }
}
