package com.siirio.jemserver.mixin.xaero.shared;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "obscura.unified.server.LayerPolicy", remap = false)
public abstract class FullCaveLayerPolicyMixin {
    @Shadow public abstract boolean isKnownDimension(String dimension);

    @Inject(method = "allows", at = @At("HEAD"), cancellable = true)
    private void jem$fullCave(String dimension, int layer, CallbackInfoReturnable<Boolean> callback) {
        if (layer == Integer.MIN_VALUE) callback.setReturnValue(isKnownDimension(dimension));
    }
}
