package com.siirio.jemclaims.mixin.bridge;

import com.siirio.jemclaims.ClaimUnion;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "io.github.flemmli97.flan.player.OfflinePlayerData", remap = false)
public abstract class OfflineClaimBudgetMixin {
    @Shadow @Final public MinecraftServer server;
    @Shadow @Final public UUID owner;

    @Inject(method = "usedClaimBlocks", at = @At("HEAD"), cancellable = true)
    private void jemClaims$used(CallbackInfoReturnable<Integer> callback) {
        callback.setReturnValue(ClaimUnion.used(server, owner));
    }
    @Inject(method = "isExpired", at = @At("HEAD"), cancellable = true)
    private void jemClaims$forever(java.time.LocalDateTime date, CallbackInfoReturnable<Boolean> callback) {
        callback.setReturnValue(false);
    }
}
