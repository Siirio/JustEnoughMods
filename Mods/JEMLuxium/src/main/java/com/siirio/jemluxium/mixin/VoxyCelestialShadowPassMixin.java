package com.siirio.jemluxium.mixin;

import com.vinlanx.luxium.client.shadows.neoskycelestia.NeoSkyCelestia;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "me.cortex.voxy.client.core.VoxyRenderSystem", remap = false)
public abstract class VoxyCelestialShadowPassMixin {
    @Inject(
            method = "setupViewport(Lorg/joml/Matrix4fc;Lorg/joml/Matrix4fc;DDD)Lme/cortex/voxy/client/core/rendering/Viewport;",
            at = @At("HEAD"),
            cancellable = true,
            require = 0,
            remap = false
    )
    private void jemLuxium$skipViewportInCelestialShadowPass(CallbackInfoReturnable<Object> callback) {
        // Voxy skips rendering when no viewport is supplied.
        if (NeoSkyCelestia.get().isRenderingTerrainShadowPass()) {
            callback.setReturnValue(null);
        }
    }
}
