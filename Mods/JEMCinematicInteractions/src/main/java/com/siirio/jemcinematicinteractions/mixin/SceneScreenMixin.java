package com.siirio.jemcinematicinteractions.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "dev.dyslexic.cinematic.client.SceneScreen", remap = false)
public abstract class SceneScreenMixin {
    @Inject(method = "guideShown", at = @At("HEAD"), cancellable = true)
    private void jemCinematicInteractions$hideFieldGuide(CallbackInfoReturnable<Boolean> callback) {
        callback.setReturnValue(false);
    }
}
