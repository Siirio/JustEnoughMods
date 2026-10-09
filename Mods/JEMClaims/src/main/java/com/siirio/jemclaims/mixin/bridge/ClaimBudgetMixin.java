package com.siirio.jemclaims.mixin.bridge;

import com.siirio.jemclaims.ClaimUnion;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "io.github.flemmli97.flan.player.PlayerClaimData", remap = false)
public abstract class ClaimBudgetMixin {
    @Shadow @Final private ServerPlayer player;
    @Shadow private int usedBlocks;

    @Inject(method = "calculateUsedClaimBlocks", at = @At("HEAD"), cancellable = true)
    private void jemClaims$used(CallbackInfoReturnable<Integer> callback) {
        callback.setReturnValue(ClaimUnion.used(player.server, player.getUUID()));
    }
    @Inject(method = "updateClaimScores", at = @At("RETURN"))
    private void jemClaims$score(CallbackInfoReturnable<Integer> callback) {
        usedBlocks = ClaimUnion.used(player.server, player.getUUID());
    }
}
