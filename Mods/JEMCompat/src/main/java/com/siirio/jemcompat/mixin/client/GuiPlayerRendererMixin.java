package com.siirio.jemcompat.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.siirio.jemcompat.client.render.GuiPlayerRenderScope;
import com.siirio.jemmenus.client.VanillaGuiPlayers;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PlayerRenderer.class, priority = 2000)
public abstract class GuiPlayerRendererMixin {
    @Inject(method = "render(Lnet/minecraft/client/player/AbstractClientPlayer;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At("HEAD"), cancellable = true)
    private void jem$vanillaGuiPlayer(AbstractClientPlayer player, float yaw, float tick, PoseStack poses, MultiBufferSource buffers, int light, CallbackInfo callback) {
        if (!GuiPlayerRenderScope.active()) return;
        var vanillaBuffers = net.minecraft.client.Minecraft.getInstance().renderBuffers().bufferSource();
        VanillaGuiPlayers.render(player, yaw, poses, vanillaBuffers, 15728880);
        vanillaBuffers.endBatch();
        callback.cancel();
    }
}
