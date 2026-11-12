package dev.taste.engine.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.taste.engine.render.RenderHooks;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
abstract class LevelRendererFrameMixin {
    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void taste$beginWorld(PoseStack poses, float partialTick, long finishTimeNano, boolean drawBlockOutline,
            Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f projection, CallbackInfo callback) {
        RenderHooks.beginWorld(camera, poses.last().pose(), projection, partialTick);
    }

    @Inject(method = "renderLevel", at = @At("RETURN"))
    private void taste$endWorld(PoseStack poses, float partialTick, long finishTimeNano, boolean drawBlockOutline,
            Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f projection, CallbackInfo callback) {
        RenderHooks.endWorld();
    }
}
