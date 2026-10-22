package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.criterion.JemCriteria;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    private static final double SPYGLASS_RANGE = 256.0D;
    private static final double LARGE_TARGET_MARGIN = 4.0D;
    private static final ResourceLocation ENHANCE = new ResourceLocation("jem_guide", "qol/1");

    @Inject(method = "updateUsingItem", at = @At("TAIL"))
    private void detectSpyglassTarget(ItemStack stack, CallbackInfo ci) {
        LivingEntity living = (LivingEntity) (Object) this;
        if (!(living instanceof ServerPlayer player) || !stack.is(Items.SPYGLASS)) {
            return;
        }
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getViewVector(1.0F).scale(SPYGLASS_RANGE));
        HitResult obstruction = player.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, player));
        if (obstruction.getType() != HitResult.Type.MISS) {
            end = obstruction.getLocation();
        }
        AABB search = player.getBoundingBox().expandTowards(end.subtract(start)).inflate(LARGE_TARGET_MARGIN);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(player, start, end, search,
                entity -> entity instanceof LivingEntity && entity != player && !entity.isSpectator(), SPYGLASS_RANGE * SPYGLASS_RANGE);
        if (hit != null) {
            JemCriteria.fire(player, ENHANCE);
        }
    }
}
