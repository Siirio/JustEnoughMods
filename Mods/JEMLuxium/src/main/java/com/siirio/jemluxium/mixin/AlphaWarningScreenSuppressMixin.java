package com.siirio.jemluxium.mixin;

import com.vinlanx.luxium.Config;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "com.vinlanx.luxium.client.guiscreen.AlphaWarningScreen", remap = false)
public abstract class AlphaWarningScreenSuppressMixin {
    @Inject(method = "shouldShow", at = @At("HEAD"), cancellable = true, remap = false)
    private static void jemLuxium$skipStartupWarning(CallbackInfoReturnable<Boolean> callback) {
        if (!Config.CLIENT.alphaWarningShown.get()) {
            Config.CLIENT.alphaWarningShown.set(true);
            Config.CLIENT.alphaWarningShown.save();
        }
        callback.setReturnValue(false);
    }
}
