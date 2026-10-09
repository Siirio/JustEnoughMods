package com.siirio.jemclaims.mixin.compat;

import com.siirio.jemclaims.compat.MachineContext;
import com.simibubi.create.content.kinetics.deployer.DeployerFakePlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.simibubi.create.content.kinetics.deployer.DeployerHandler", remap = false)
public abstract class CreateDeployerMixin {
    @Inject(method = "activate", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/kinetics/deployer/DeployerHandler;activateInner(Lcom/simibubi/create/content/kinetics/deployer/DeployerFakePlayer;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/Vec3;Lcom/simibubi/create/content/kinetics/deployer/DeployerBlockEntity$Mode;)V"))
    private static void jemClaims$beforeActivate(DeployerFakePlayer player, Vec3 source, BlockPos target, Vec3 direction, @Coerce Object mode, CallbackInfo callback) {
        BlockPos origin = MachineContext.source() == null ? BlockPos.containing(source) : MachineContext.source();
        com.siirio.jemclaims.compat.CreatePlacement.placing(player.serverLevel(), origin, target);
        MachineContext.push(origin);
    }

    @Inject(method = "activate", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/kinetics/deployer/DeployerHandler;activateInner(Lcom/simibubi/create/content/kinetics/deployer/DeployerFakePlayer;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/Vec3;Lcom/simibubi/create/content/kinetics/deployer/DeployerBlockEntity$Mode;)V", shift = At.Shift.AFTER))
    private static void jemClaims$afterActivate(DeployerFakePlayer player, Vec3 source, BlockPos target, Vec3 direction, @Coerce Object mode, CallbackInfo callback) {
        MachineContext.pop();
    }
}
