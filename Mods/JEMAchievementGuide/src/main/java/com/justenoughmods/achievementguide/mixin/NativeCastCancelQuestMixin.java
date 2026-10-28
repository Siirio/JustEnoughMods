package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.CombatCastCancelProof;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "io.redspace.ironsspellbooks.network.casting.CancelCastPacket", remap = false)
public abstract class NativeCastCancelQuestMixin {
    @Inject(method = "cancelCast", at = @At("HEAD"))
    private static void jem$beforeNativeCancel(ServerPlayer actor, boolean cooldown, CallbackInfo callback) {
        CombatCastCancelProof.beforeCancellation(actor);
    }
    @Inject(method = "cancelCast", at = @At("RETURN"))
    private static void jem$afterNativeCancel(ServerPlayer actor, boolean cooldown, CallbackInfo callback) {
        CombatCastCancelProof.afterCancellation(actor);
    }
}
