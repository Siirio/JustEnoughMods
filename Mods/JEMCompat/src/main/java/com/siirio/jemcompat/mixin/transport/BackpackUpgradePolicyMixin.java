package com.siirio.jemcompat.mixin.transport;

import com.siirio.jemcompat.feature.transport.BackpackPolicy;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = UpgradeHandler.class, remap = false)
public abstract class BackpackUpgradePolicyMixin {
    @Shadow @Final private IStorageWrapper storageWrapper;

    @Inject(method = "isItemValid", at = @At("HEAD"), cancellable = true)
    private void jemcompat$rejectRetiredBackpackUpgrade(int slot, ItemStack stack,
            CallbackInfoReturnable<Boolean> callback) {
        if ("backpack".equals(storageWrapper.getStorageType()) && BackpackPolicy.isRetiredUpgrade(stack)) {
            callback.setReturnValue(false);
        }
    }
}
