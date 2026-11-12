package dev.taste.engine.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.taste.engine.render.RenderHooks;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
abstract class GameRendererWorldBoundaryMixin {
    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void taste$beginGameWorld(float partialTick, long finishTimeNano, PoseStack pose, CallbackInfo callback) {
        RenderHooks.beginGameWorld();
    }

    @Inject(method = "renderLevel", at = @At(value = "INVOKE", shift = At.Shift.AFTER, remap = false,
            target = "Lnet/minecraftforge/client/ForgeHooksClient;dispatchRenderStage(Lnet/minecraftforge/client/event/RenderLevelStageEvent$Stage;Lnet/minecraft/client/renderer/LevelRenderer;Lcom/mojang/blaze3d/vertex/PoseStack;Lorg/joml/Matrix4f;ILnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/culling/Frustum;)V"))
    private void taste$afterForgeWorld(float partialTick, long finishTimeNano, PoseStack pose, CallbackInfo callback) {
        RenderHooks.afterForgeWorld();
    }

    @Inject(method = "renderLevel", at = @At("RETURN"))
    private void taste$endGameWorld(float partialTick, long finishTimeNano, PoseStack pose, CallbackInfo callback) {
        RenderHooks.endGameWorld();
    }
}
