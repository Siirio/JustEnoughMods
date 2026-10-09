package com.siirio.jemcompat.mixin.client;

import com.siirio.jemcompat.client.render.FrozenTintBufferSource;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherFrozenTintMixin {
    @ModifyVariable(
            method = "render(Lnet/minecraft/world/entity/Entity;DDDFFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0
    )
    private MultiBufferSource jemcompat$applyFrozenTint(MultiBufferSource buffers, Entity entity) {
        return entity instanceof LivingEntity living && living.getTicksFrozen() > 0
                ? new FrozenTintBufferSource(buffers)
                : buffers;
    }
}
