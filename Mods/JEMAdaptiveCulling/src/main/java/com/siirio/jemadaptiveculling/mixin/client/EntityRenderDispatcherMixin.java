package com.siirio.jemadaptiveculling.mixin.client;

import com.siirio.jemadaptiveculling.client.VisibilityCache;
import com.siirio.jemadaptiveculling.config.JEMConfig;
import com.siirio.jemadaptiveculling.client.CompatibilityPolicy;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {
    @Inject(method = "shouldRender", at = @At("RETURN"), cancellable = true)
    private <E extends Entity> void jemadaptiveculling$cullOccludedEntity(
            E entity,
            Frustum frustum,
            double cameraX,
            double cameraY,
            double cameraZ,
            CallbackInfoReturnable<Boolean> callback
    ) {
        if (!callback.getReturnValue() || !JEMConfig.ENTITY_OCCLUSION.get() || !CompatibilityPolicy.canCull(entity)
                || entity instanceof Player
                || entity == Minecraft.getInstance().cameraEntity
                || entity.isCurrentlyGlowing()
                || entity.noCulling
                || Minecraft.getInstance().hitResult instanceof EntityHitResult hit && hit.getEntity() == entity
                || entity instanceof LivingEntity living && living.getMaxHealth() >= 150.0F
                || Minecraft.getInstance().player != null && (Minecraft.getInstance().player.getLastHurtMob() == entity
                || Minecraft.getInstance().player.getLastHurtByMob() == entity)) {
            return;
        }
        if (!VisibilityCache.shouldRenderEntity(entity.getId(), entity.getBoundingBoxForCulling())) {
            callback.setReturnValue(false);
        }
    }
}
