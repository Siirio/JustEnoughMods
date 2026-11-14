package com.siirio.jemlightingsandbox.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.siirio.jemlightingsandbox.terrain.EntityShadowMap;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelReader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = EntityRenderDispatcher.class, priority = 1100)
abstract class EntityShadowBlobMixin {
    @Inject(method = "renderShadow(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;"
            + "Lnet/minecraft/world/entity/Entity;FFLnet/minecraft/world/level/LevelReader;F)V",
            at = @At("HEAD"), cancellable = true)
    private static void jem$hideReplacedBlob(PoseStack poses, MultiBufferSource buffers, Entity entity,
            float strength, float partialTick, LevelReader level, float radius, CallbackInfo callback) {
        if (EntityShadowMap.replacesBlob(entity)) {
            callback.cancel();
        }
    }
}
