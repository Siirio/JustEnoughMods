package com.siirio.jemclaims.mixin.compat;

import com.siirio.jemclaims.ClaimPermission;
import com.siirio.jemclaims.FlanBridge;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.simibubi.create.content.contraptions.actors.harvester.HarvesterMovementBehaviour", remap = false)
public abstract class CreateHarvesterMixin {
    @Inject(method = "visitNewPosition", at = @At("HEAD"), cancellable = true)
    private void jemClaims$harvest(MovementContext context, BlockPos target, CallbackInfo ci) {
        if (context.world instanceof ServerLevel level
                && (!FlanBridge.canAutomate(level, context.contraption.anchor, target, ClaimPermission.BREAK)
                || !FlanBridge.canAutomate(level, context.contraption.anchor, target, ClaimPermission.PLACE))) { ci.cancel(); return; }
        if (context.world instanceof ServerLevel level) com.siirio.jemclaims.compat.CreatePlacement.placing(level, context.contraption.anchor, target);
    }
}
