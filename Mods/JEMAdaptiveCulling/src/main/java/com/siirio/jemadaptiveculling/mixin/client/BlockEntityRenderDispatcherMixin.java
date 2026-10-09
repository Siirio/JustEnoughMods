package com.siirio.jemadaptiveculling.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.siirio.jemadaptiveculling.client.VisibilityCache;
import com.siirio.jemadaptiveculling.config.JEMConfig;
import com.siirio.jemadaptiveculling.client.CompatibilityPolicy;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.world.level.block.entity.BlockEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockEntityRenderDispatcher.class)
public abstract class BlockEntityRenderDispatcherMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private <E extends BlockEntity> void jemadaptiveculling$cullOccludedBlockEntity(
            E blockEntity,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffers,
            CallbackInfo callback
    ) {
        if (!JEMConfig.BLOCK_ENTITY_OCCLUSION.get() || !CompatibilityPolicy.canCull(blockEntity)) {
            return;
        }
        BlockEntityRenderer<E> renderer = ((BlockEntityRenderDispatcher) (Object) this).getRenderer(blockEntity);
        if (renderer == null || renderer.shouldRenderOffScreen(blockEntity)) {
            return;
        }
        if (!VisibilityCache.shouldRenderBlockEntity(blockEntity, renderer, partialTick)) {
            callback.cancel();
        }
    }
}


