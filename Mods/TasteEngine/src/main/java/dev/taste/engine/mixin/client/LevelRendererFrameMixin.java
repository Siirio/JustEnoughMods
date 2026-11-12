package dev.taste.engine.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.taste.engine.render.RenderHooks;
import dev.taste.engine.render.TasteRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.PostChain;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
abstract class LevelRendererFrameMixin {
    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void taste$beginWorld(PoseStack poses, float partialTick, long finishTimeNano, boolean drawBlockOutline,
            Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f projection, CallbackInfo callback) {
        RenderHooks.beginWorld(camera, poses.last().pose(), projection, partialTick);
    }

    @Inject(method = "renderLevel", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/RenderType;translucent()Lnet/minecraft/client/renderer/RenderType;"))
    private void taste$captureOpaqueWorld(PoseStack poses, float partialTick, long finishTimeNano, boolean drawBlockOutline,
            Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f projection, CallbackInfo callback) {
        RenderHooks.beforeTranslucentTerrain();
    }

    @Inject(method = "renderLevel", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/multiplayer/ClientLevel;entitiesForRendering()Ljava/lang/Iterable;"))
    private void taste$prepareActorLighting(CallbackInfo callback) {
        RenderHooks.beforeActors();
    }

    @Inject(method = "renderClouds", at = @At("HEAD"), cancellable = true)
    private void taste$replaceNativeClouds(CallbackInfo callback) {
        if (TasteRenderer.instance().suppressNativeClouds()) callback.cancel();
    }

    @Inject(method = "renderSnowAndRain", at = @At("HEAD"), cancellable = true)
    private void taste$replaceNativePrecipitation(CallbackInfo callback) {
        if (TasteRenderer.instance().suppressNativePrecipitation()) callback.cancel();
    }

    @Redirect(method = "renderLevel", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/PostChain;process(F)V"))
    private void taste$routeFabulousResolve(PostChain chain, float partialTick) {
        if (!TasteRenderer.instance().composeFabulous(chain)) chain.process(partialTick);
    }

    @Inject(method = "renderLevel", at = @At("RETURN"))
    private void taste$endWorld(PoseStack poses, float partialTick, long finishTimeNano, boolean drawBlockOutline,
            Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f projection, CallbackInfo callback) {
        RenderHooks.endWorld();
    }
}
