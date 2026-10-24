package com.siirio.jemfood.mixin;

import com.siirio.jemfood.client.FoodTooltips;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraftforge.client.ForgeHooksClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Filter only the synchronized exact client instance, never all hidden native effects of an ID. */
@Mixin(value = ForgeHooksClient.class, remap = false)
public abstract class EffectScreenMixin {
    @Inject(method = "shouldRenderEffect", at = @At("HEAD"), cancellable = true, remap = false)
    private static void ownedBackend(MobEffectInstance instance, CallbackInfoReturnable<Boolean> callback) {
        if (FoodTooltips.hidden(instance)) callback.setReturnValue(false);
    }
}
