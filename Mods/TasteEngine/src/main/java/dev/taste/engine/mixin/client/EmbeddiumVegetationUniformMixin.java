package dev.taste.engine.mixin.client;

import dev.taste.engine.effects.VegetationMotion;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "me.jellysquid.mods.sodium.client.render.chunk.shader.ChunkShaderInterface", remap = false)
abstract class EmbeddiumVegetationUniformMixin {
    @Inject(method = "setupState", at = @At("RETURN"), require = 0)
    private void taste$windUniforms(CallbackInfo ci) {
        int program = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        if (program != 0) VegetationMotion.bind(program);
    }
}
