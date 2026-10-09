package com.siirio.jemclaims.mixin.compat;

import com.siirio.jemclaims.compat.MachineContext;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.kinetics.deployer.DeployerFakePlayer;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.simibubi.create.content.kinetics.deployer.DeployerMovementBehaviour", remap = false)
public abstract class CreateMovingDeployerMixin {
    private static final String ACTIVATE = "Lcom/simibubi/create/content/kinetics/deployer/DeployerMovementBehaviour;activate(Lcom/simibubi/create/content/contraptions/behaviour/MovementContext;Lnet/minecraft/core/BlockPos;Lcom/simibubi/create/content/kinetics/deployer/DeployerFakePlayer;Lcom/simibubi/create/content/kinetics/deployer/DeployerBlockEntity$Mode;)V";

    @Inject(method = "visitNewPosition", at = @At(value = "INVOKE", target = ACTIVATE))
    private void jemClaims$beforeVisit(MovementContext context, BlockPos target, CallbackInfo callback) {
        if (context.world instanceof net.minecraft.server.level.ServerLevel level) com.siirio.jemclaims.compat.CreatePlacement.placing(level, context.contraption.anchor, target);
        MachineContext.push(context.contraption.anchor);
    }

    @Inject(method = "visitNewPosition", at = @At(value = "INVOKE", target = ACTIVATE, shift = At.Shift.AFTER))
    private void jemClaims$afterVisit(MovementContext context, BlockPos target, CallbackInfo callback) {
        MachineContext.pop();
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = ACTIVATE))
    private void jemClaims$beforeTick(MovementContext context, CallbackInfo callback) {
        DeployerFakePlayer player = (DeployerFakePlayer) context.temporaryData;
        BlockPos target = ((CreateDeployerPlayerAccessor) player).jemClaims$blockBreakingProgress().getKey();
        if (context.world instanceof net.minecraft.server.level.ServerLevel level) com.siirio.jemclaims.compat.CreatePlacement.placing(level, context.contraption.anchor, target);
        MachineContext.push(context.contraption.anchor);
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = ACTIVATE, shift = At.Shift.AFTER))
    private void jemClaims$afterTick(MovementContext context, CallbackInfo callback) {
        MachineContext.pop();
    }
}
