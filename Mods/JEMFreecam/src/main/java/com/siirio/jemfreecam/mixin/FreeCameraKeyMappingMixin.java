package com.siirio.jemfreecam.mixin;

import com.siirio.jemfreecam.client.FreeCameraController;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Prevent body-only dodge inputs from firing while the detached camera is active. */
@Mixin(KeyMapping.class)
public abstract class FreeCameraKeyMappingMixin {
    @Inject(method = "isDown", at = @At("HEAD"), cancellable = true)
    private void jemFreecam$blockHeldBodyAction(CallbackInfoReturnable<Boolean> cir) {
        if (blocked()) cir.setReturnValue(false);
    }

    @Inject(method = "consumeClick", at = @At("HEAD"), cancellable = true)
    private void jemFreecam$blockClickedBodyAction(CallbackInfoReturnable<Boolean> cir) {
        if (blocked()) cir.setReturnValue(false);
    }

    private boolean blocked() {
        if (!FreeCameraController.isActive()) return false;
        String name = ((KeyMapping) (Object) this).getName();
        return name.equals("key.epicfight.dodge")
                || name.equals("key.block_factorys_bosses.dodge_roll");
    }
}
