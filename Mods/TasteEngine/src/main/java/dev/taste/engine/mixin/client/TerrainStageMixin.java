package dev.taste.engine.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.taste.engine.render.RenderHooks;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
abstract class TerrainStageMixin {
    @Inject(method = "renderChunkLayer", at = @At("HEAD"))
    private void taste$beginTerrainLayer(RenderType type, PoseStack poses, double cameraX, double cameraY,
            double cameraZ, Matrix4f projection, CallbackInfo callback) {
        RenderHooks.beginLayer(type);
    }

    @Inject(method = "renderChunkLayer", at = @At("RETURN"))
    private void taste$endTerrainLayer(RenderType type, PoseStack poses, double cameraX, double cameraY,
            double cameraZ, Matrix4f projection, CallbackInfo callback) {
        RenderHooks.endLayer(type);
    }
}
