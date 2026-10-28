package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.food.NativeSoilProof;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "net.satisfy.farm_and_charm.platform.PlatformHelper", remap = false)
public abstract class RemainingSprinklerRangeMixin {
    @Inject(method = "getWaterSprinklerRange", at = @At("RETURN"))
    private static void jem$nativePredicateRange(CallbackInfoReturnable<Integer> callback) { NativeSoilProof.nativeRange(); }
}
