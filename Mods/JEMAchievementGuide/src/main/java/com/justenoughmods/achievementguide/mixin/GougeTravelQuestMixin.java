package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.travel.GougeTravelProof;
import net.gouge.physics.GougePhysics;
import net.gouge.physics.GripState;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = GougePhysics.class, remap = false)
public abstract class GougeTravelQuestMixin {
    @Inject(method = "tick0(Lnet/minecraft/server/level/ServerPlayer;[I)Z", at = @At("HEAD"))
    private static void jem$beforeSupport(ServerPlayer actor, int[] nativeState,
                                          CallbackInfoReturnable<Boolean> callback) {
        GougeTravelProof.beforeSupport(actor);
    }

    @Inject(method = "tick0(Lnet/minecraft/server/level/ServerPlayer;[I)Z", at = @At("RETURN"))
    private static void jem$nativeSupport(ServerPlayer actor, int[] nativeState,
                                          CallbackInfoReturnable<Boolean> callback) {
        GougeTravelProof.supported(actor, callback.getReturnValue(), GripState.isActive(actor.getUUID()));
    }

    @Inject(method = "stopSliding(Lnet/minecraft/server/level/ServerPlayer;)V", at = @At("HEAD"))
    private static void jem$nativeStop(ServerPlayer actor, CallbackInfo callback) {
        GougeTravelProof.reset(actor.getUUID());
    }
}
