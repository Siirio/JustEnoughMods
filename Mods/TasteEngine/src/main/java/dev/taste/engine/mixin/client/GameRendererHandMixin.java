package dev.taste.engine.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.taste.engine.render.RenderHooks;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
abstract class GameRendererHandMixin {
    @Inject(method = "renderItemInHand", at = @At("HEAD"))
    private void taste$beginHand(PoseStack poses, Camera camera, float partialTick, CallbackInfo callback) {
        RenderHooks.beginHand();
    }

    @Inject(method = "renderItemInHand", at = @At("RETURN"))
    private void taste$endHand(PoseStack poses, Camera camera, float partialTick, CallbackInfo callback) {
        RenderHooks.endHand();
    }
}
