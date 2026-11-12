package dev.taste.engine.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.taste.engine.render.RenderHooks;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(GameRenderer.class)
abstract class GameRendererHandMixin {
    @Redirect(method = "renderItemInHand", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderHandsWithItems(FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/player/LocalPlayer;I)V"))
    private void taste$renderHands(ItemInHandRenderer renderer, float partialTick, PoseStack poses,
            MultiBufferSource.BufferSource buffers, LocalPlayer player, int packedLight) {
        try {
            RenderHooks.beginHand();
            renderer.renderHandsWithItems(partialTick, poses, buffers, player, packedLight);
        } finally {
            RenderHooks.endHand();
        }
    }
}
