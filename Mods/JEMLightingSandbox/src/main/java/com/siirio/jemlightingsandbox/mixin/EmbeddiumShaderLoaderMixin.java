package com.siirio.jemlightingsandbox.mixin;

import com.siirio.jemlightingsandbox.terrain.EmbeddiumTerrainShaders;
import com.siirio.jemlightingsandbox.terrain.TerrainLighting;
import me.jellysquid.mods.sodium.client.gl.shader.ShaderLoader;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "me.jellysquid.mods.sodium.client.gl.shader.ShaderLoader", remap = false)
public abstract class EmbeddiumShaderLoaderMixin {
    @Inject(method = "getShaderSource(Lnet/minecraft/resources/ResourceLocation;)Ljava/lang/String;",
            at = @At("RETURN"), cancellable = true, remap = false)
    private static void jemLightingSandbox$addTerrainLighting(ResourceLocation id,
                                                              CallbackInfoReturnable<String> callback) {
        if ("sodium".equals(id.getNamespace()) && EmbeddiumTerrainShaders.isTerrainStage(id.getPath())
                && TerrainLighting.canPatchStockShaders()) {
            String source = callback.getReturnValue();
            String patched = EmbeddiumTerrainShaders.patch(id.getPath(), source, ShaderLoader.class.getClassLoader());
            if (patched != source) {
                callback.setReturnValue(patched);
            }
        }
    }
}
