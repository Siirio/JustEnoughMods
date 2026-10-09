package com.siirio.jemserver.mixin.fishing;

import com.li64.tide.data.fishing.FishData;
import com.li64.tide.data.fishing.FishingContext;
import com.siirio.jemserver.smp.fishing.FishingRuntime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = FishData.class, remap = false)
public abstract class TideFishDataMixin {
    @Inject(method = "weight(Lcom/li64/tide/data/fishing/FishingContext;)D", at = @At("RETURN"), cancellable = true)
    private void jemWeight(FishingContext context, CallbackInfoReturnable<Double> result) {
        result.setReturnValue(FishingRuntime.weight((FishData) (Object) this, context, result.getReturnValueD()));
    }
    @Inject(method = "shouldKeep", at = @At("RETURN"), cancellable = true)
    private void jemAccess(FishingContext context, CallbackInfoReturnable<Boolean> result) {
        if (result.getReturnValueZ() && FishingRuntime.active(context) && !FishingRuntime.allowed(context)) result.setReturnValue(false);
    }
}
