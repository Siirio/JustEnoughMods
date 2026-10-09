package com.siirio.jemclaims.mixin.compat;

import com.siirio.jemclaims.ClaimPermission;
import com.siirio.jemclaims.FlanBridge;
import com.siirio.jemclaims.compat.GraveCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "de.maxhenkel.gravestone.GraveUtils", remap = false)
public abstract class GraveRecoveryMixin {
    @Inject(method = "canBreakGrave", at = @At("HEAD"), cancellable = true)
    private static void jemClaims$recover(Level level, Player player, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (player instanceof ServerPlayer serverPlayer && level instanceof ServerLevel serverLevel
                && !GraveCompat.isOwner(serverPlayer, level, pos)
                && !FlanBridge.can(serverPlayer, serverLevel, pos, ClaimPermission.BREAK)) {
            cir.setReturnValue(false);
        }
    }
}
