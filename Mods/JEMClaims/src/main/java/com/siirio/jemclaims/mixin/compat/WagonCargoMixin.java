package com.siirio.jemclaims.mixin.compat;

import com.siirio.jemclaims.ClaimPermission;
import com.siirio.jemclaims.FlanBridge;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "net.favouriteless.trotting_wagons.common.entities.base.AbstractInventoryWagon", remap = false)
public abstract class WagonCargoMixin {
    @Inject(method = "m_6096_", at = @At("HEAD"), cancellable = true)
    private void jemClaims$open(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        Entity wagon = (Entity) (Object) this;
        if (player instanceof ServerPlayer serverPlayer && player.isSecondaryUseActive()
                && !FlanBridge.can(serverPlayer, serverPlayer.serverLevel(), wagon.blockPosition(), ClaimPermission.OPENCONTAINER)) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }
}
