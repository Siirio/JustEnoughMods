package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.travel.GougeTravelProof;
import net.gouge.physics.ClimbMoves;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ClimbMoves.class, remap = false)
public abstract class GougeTravelKickQuestMixin {
    @Inject(method = "wallKick(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/phys/BlockHitResult;[I)Z", at = @At("RETURN"))
    private static void jem$nativeKick(ServerPlayer actor, ServerLevel level, BlockHitResult hit, int[] nativeState,
                                       CallbackInfoReturnable<Boolean> callback) {
        GougeTravelProof.kicked(actor, callback.getReturnValue());
    }
}
