package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.home.HomeSignalProof;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "net.larsmans.infinitybuttons.InfinityButtonsUtil", remap = false)
public abstract class HomeChainEndpointQuestMixin {
    @Inject(method = "checkChains", at = @At("RETURN"))
    private static void jem$nativeEndpoint(Level level, BlockPos pos, CallbackInfoReturnable<Integer> callback) {
        HomeSignalProof.chains(level, pos, callback.getReturnValue());
    }
}
