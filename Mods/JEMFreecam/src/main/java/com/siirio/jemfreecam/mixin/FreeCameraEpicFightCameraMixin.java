package com.siirio.jemfreecam.mixin;

import com.siirio.jemfreecam.client.FreeCameraController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Let Epic Fight's native MouseHandler redirect reach Entity.turn while freecam owns the view. */
@Pseudo
@Mixin(targets = "yesman.epicfight.api.client.camera.EpicFightCameraAPI", remap = false)
public abstract class FreeCameraEpicFightCameraMixin {
    @Inject(method = "turnCamera", at = @At("HEAD"), cancellable = true, remap = false)
    private void jemcompat$leaveMouseTurnToFreecam(double horizontal, double vertical,
            CallbackInfoReturnable<Boolean> cir) {
        if (FreeCameraController.isActive()) cir.setReturnValue(false);
    }
}
