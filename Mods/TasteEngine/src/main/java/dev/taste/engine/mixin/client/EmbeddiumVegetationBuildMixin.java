package dev.taste.engine.mixin.client;

import dev.taste.engine.effects.VegetationMotion;
import dev.taste.engine.render.embeddium.EmbeddiumVegetation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer", remap = false)
abstract class EmbeddiumVegetationBuildMixin {
    @Inject(method = "renderModel", at = @At("HEAD"), require = 0)
    private void taste$open(@Coerce Object context, @Coerce Object buffers, CallbackInfo ci) {
        EmbeddiumVegetation.begin(context);
    }

    @Inject(method = "renderModel", at = @At("RETURN"), require = 0)
    private void taste$close(CallbackInfo ci) {
        VegetationMotion.endMesh();
    }
}
