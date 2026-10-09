package com.siirio.jemclaims.mixin.compat;

import com.siirio.jemclaims.ClaimPermission;
import com.siirio.jemclaims.FlanBridge;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "tschipp.carryon.common.carry.PlacementHandler", remap = false)
public abstract class CarryStackMixin {
    @Inject(method = "tryStackEntity", at = @At("HEAD"), cancellable = true)
    private static void jemClaims$stack(ServerPlayer player, Entity target, CallbackInfo ci) {
        if (!FlanBridge.can(player, player.serverLevel(), target.blockPosition(), ClaimPermission.ANIMALINTERACT)
                || !FlanBridge.can(player, player.serverLevel(), target.blockPosition(), ClaimPermission.PLACE)) ci.cancel();
    }
}
