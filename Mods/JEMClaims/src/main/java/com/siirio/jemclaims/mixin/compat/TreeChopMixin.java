package com.siirio.jemclaims.mixin.compat;

import com.siirio.jemclaims.ClaimPermission;
import com.siirio.jemclaims.FlanBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "ht.treechop.common.chop.Chop", remap = false)
public abstract class TreeChopMixin {
    @Shadow @Final private BlockPos blockPos;

    @Inject(method = "apply", at = @At("HEAD"), cancellable = true)
    private void jemClaims$chop(Level level, Player player, ItemStack stack, boolean felling, CallbackInfo ci) {
        if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer
                && !FlanBridge.can(serverPlayer, serverLevel, blockPos, ClaimPermission.BREAK)) ci.cancel();
    }
}
