package com.siirio.jemcompat.mixin.transport;

import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerElytraMixin {
    private static final float REQUIRED_FALL_DISTANCE = 6.0F;

    @Inject(method = "tryToStartFallFlying", at = @At("HEAD"), cancellable = true)
    private void jemcompat$requireSixBlockFall(CallbackInfoReturnable<Boolean> callback) {
        if (((Player) (Object) this).fallDistance < REQUIRED_FALL_DISTANCE) {
            callback.setReturnValue(false);
        }
    }
}
