package dev.taste.engine.mixin.client;

import com.mojang.blaze3d.preprocessor.GlslPreprocessor;
import com.mojang.blaze3d.shaders.Program;
import dev.taste.engine.effects.VegetationMotion;
import java.io.IOException;
import java.io.InputStream;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Program.class)
abstract class VanillaVegetationProgramMixin {
    @ModifyVariable(method = "compileShader", at = @At("HEAD"), argsOnly = true)
    private static InputStream taste$windSource(InputStream stream, Program.Type type, String name, InputStream input,
            String pack, GlslPreprocessor preprocessor) throws IOException {
        return type == Program.Type.VERTEX ? VegetationMotion.vanillaSource(name, stream) : stream;
    }
}
