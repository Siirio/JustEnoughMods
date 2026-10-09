package com.siirio.jemclaims.mixin.bridge;

import com.siirio.jemclaims.FlanBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "io.github.flemmli97.flan.claim.Claim", remap = false)
public abstract class MachinePermissionMixin {
    @Inject(method = "canInteract", at = @At("HEAD"), cancellable = true)
    private void jemClaims$machine(ServerPlayer player, ResourceLocation permission, BlockPos target, boolean message, CallbackInfoReturnable<Boolean> callback) {
        InteractionResult result = FlanBridge.machinePermission(player, permission, target);
        if (result != InteractionResult.PASS) callback.setReturnValue(result == InteractionResult.SUCCESS);
    }

    @Inject(method = "canInteract", at = @At("RETURN"), cancellable = true)
    private void jemClaims$overlap(ServerPlayer player, ResourceLocation permission, BlockPos target, boolean message, CallbackInfoReturnable<Boolean> callback) {
        if (callback.getReturnValue() && !FlanBridge.overlapPermission((io.github.flemmli97.flan.claim.Claim) (Object) this, player, permission, target)) callback.setReturnValue(false);
    }
}
