package com.siirio.jemserver.mixin.xaero.shared;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "obscura.unified.util.MapLayer", remap = false)
public abstract class FullCaveMapLayerMixin {
    @Inject(method = "isValidCaveLayer", at = @At("HEAD"), cancellable = true)
    private static void jem$fullCave(int layer, CallbackInfoReturnable<Boolean> callback) {
        if (layer == Integer.MIN_VALUE) callback.setReturnValue(true);
    }
}
