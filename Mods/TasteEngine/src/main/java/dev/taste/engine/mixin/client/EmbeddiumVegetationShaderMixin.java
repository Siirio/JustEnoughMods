package dev.taste.engine.mixin.client;

import dev.taste.engine.effects.VegetationMotion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Pseudo
@Mixin(targets = "me.jellysquid.mods.sodium.client.gl.shader.GlShader", remap = false)
abstract class EmbeddiumVegetationShaderMixin {
    @ModifyArg(method = "<init>", at = @At(value = "INVOKE",
            target = "Lme/jellysquid/mods/sodium/client/gl/shader/ShaderWorkarounds;safeShaderSource(ILjava/lang/CharSequence;)V"),
            index = 1, require = 0)
    private CharSequence taste$windSource(CharSequence source) {
        return VegetationMotion.embeddiumSource(source.toString());
    }
}
