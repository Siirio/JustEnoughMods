package com.siirio.unofficialoffgrid.mixin;

import com.siirio.unofficialoffgrid.RelativePlacementClassifier;
import dev.tazer.offgrid.world.Overlays;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Overlays.class, remap = false)
public abstract class OverlaysMixin {
    @Inject(method = "canOffset", at = @At("RETURN"), cancellable = true)
    private static void unofficialOffgrid$extendOffsetEligibility(BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() && RelativePlacementClassifier.extendsOffgrid(state)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "canOverlay", at = @At("RETURN"), cancellable = true)
    private static void unofficialOffgrid$extendOverlayEligibility(BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() && RelativePlacementClassifier.extendsOffgrid(state)) {
            cir.setReturnValue(true);
        }
    }
}
