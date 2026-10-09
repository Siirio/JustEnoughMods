package com.justenoughmods.achievementguide.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.jack.gravestones.JacksGravestones$ForgeEvents", remap = false)
public abstract class LegacyGraveCreationMixin {
    @Inject(method = "onLivingDeath", at = @At("HEAD"), cancellable = true)
    private static void jem$preserveInventoryForGraveStone(CallbackInfo callback) {
        callback.cancel();
    }
}
