package com.siirio.jemfreecam.mixin;

import com.siirio.jemfreecam.client.FreeCameraController;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Intercept the actual local player's turn without competing for MouseHandler redirects. */
@Mixin(Entity.class)
public abstract class FreeCameraPlayerTurnMixin {
    @Inject(method = "turn", at = @At("HEAD"), cancellable = true)
    private void jemcompat$turnCamera(double horizontal, double vertical, CallbackInfo ci) {
        if (FreeCameraController.turnCameraForPlayer((Entity) (Object) this, horizontal, vertical)) ci.cancel();
    }
}
