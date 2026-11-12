package dev.taste.engine.mixin.client;

import dev.taste.engine.effects.VegetationMotion;
import net.minecraft.client.renderer.ShaderInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ShaderInstance.class)
abstract class VegetationUniformMixin {
    @Inject(method = "apply", at = @At("RETURN"))
    private void taste$windUniforms(CallbackInfo ci) {
        ShaderInstance shader = (ShaderInstance) (Object) this;
        if (VegetationMotion.isVanillaProgram(shader.getName())) VegetationMotion.bind(shader.getId());
    }
}
