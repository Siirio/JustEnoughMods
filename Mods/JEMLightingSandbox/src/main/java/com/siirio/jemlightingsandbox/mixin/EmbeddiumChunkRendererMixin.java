package com.siirio.jemlightingsandbox.mixin;

import com.siirio.jemlightingsandbox.terrain.TerrainLighting;
import com.siirio.jemlightingsandbox.terrain.WaterSurface;
import net.minecraft.client.Minecraft;
import me.jellysquid.mods.sodium.client.gl.device.CommandList;
import me.jellysquid.mods.sodium.client.gl.shader.GlProgram;
import me.jellysquid.mods.sodium.client.render.chunk.shader.ChunkShaderInterface;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.DefaultTerrainRenderPasses;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "me.jellysquid.mods.sodium.client.render.chunk.ShaderChunkRenderer", remap = false)
public abstract class EmbeddiumChunkRendererMixin {
    @Shadow(remap = false)
    protected GlProgram<ChunkShaderInterface> activeProgram;

    @Inject(method = "begin(Lme/jellysquid/mods/sodium/client/render/chunk/terrain/TerrainRenderPass;)V",
            at = @At("RETURN"), remap = false)
    private void jemLightingSandbox$bindTerrainLighting(TerrainRenderPass pass, CallbackInfo callback) {
        if (activeProgram != null) {
            boolean receivesLighting = pass == DefaultTerrainRenderPasses.SOLID || pass == DefaultTerrainRenderPasses.CUTOUT;
            TerrainLighting.begin(activeProgram.handle(), receivesLighting);
            WaterSurface.prepare(activeProgram.handle(), Minecraft.getInstance(), pass == DefaultTerrainRenderPasses.TRANSLUCENT);
        }
    }

    @Inject(method = "end(Lme/jellysquid/mods/sodium/client/render/chunk/terrain/TerrainRenderPass;)V",
            at = @At("HEAD"), remap = false)
    private void jemLightingSandbox$restoreTextureBinding(CallbackInfo callback) {
        TerrainLighting.end();
        WaterSurface.end();
    }

    @Inject(method = "delete(Lme/jellysquid/mods/sodium/client/gl/device/CommandList;)V",
            at = @At("HEAD"), remap = false)
    private void jemLightingSandbox$forgetProgramBindings(CommandList commands, CallbackInfo callback) {
        TerrainLighting.clearProgramBindings();
        WaterSurface.clearProgramBindings();
    }
}
