package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.create.CreateDeployerProofs;
import com.simibubi.create.content.kinetics.deployer.DeployerFakePlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.simibubi.create.content.kinetics.deployer.DeployerHandler", remap = false)
public abstract class CreateDeployerPlacementQuestMixin {
    @Inject(method = "activateInner", at = @At("HEAD"), remap = false)
    private static void jem$beginNativePlacement(DeployerFakePlayer player, Vec3 vec, BlockPos pos, Vec3 extension,
                                                 @Coerce Object mode, CallbackInfo callback) {
        CreateDeployerProofs.begin(player);
    }
    @Inject(method = "activateInner", at = @At("RETURN"), remap = false)
    private static void jem$finishNativePlacement(DeployerFakePlayer player, Vec3 vec, BlockPos pos, Vec3 extension,
                                                  @Coerce Object mode, CallbackInfo callback) {
        CreateDeployerProofs.finished(player);
    }
}
