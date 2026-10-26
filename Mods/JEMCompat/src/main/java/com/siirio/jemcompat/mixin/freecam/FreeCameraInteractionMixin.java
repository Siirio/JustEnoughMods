package com.siirio.jemcompat.mixin.freecam;

import com.siirio.jemcompat.client.FreeCameraController;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Guard native action entry points, including held block breaking between clicks. */
@Mixin(Minecraft.class)
public abstract class FreeCameraInteractionMixin {
    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void jemcompat$noRemoteAttack(CallbackInfoReturnable<Boolean> cir) {
        if (FreeCameraController.isActive()) cir.setReturnValue(false);
    }

    @Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
    private void jemcompat$noRemoteMining(boolean held, CallbackInfo ci) {
        if (FreeCameraController.isActive()) ci.cancel();
    }

    @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
    private void jemcompat$noRemoteUse(CallbackInfo ci) {
        if (FreeCameraController.isActive()) ci.cancel();
    }
}
