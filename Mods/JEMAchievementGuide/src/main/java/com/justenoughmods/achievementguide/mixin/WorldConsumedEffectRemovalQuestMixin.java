package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.worldmisc.WorldHungerGuardProof;
import com.justenoughmods.achievementguide.integration.worldmisc.WorldMaelstromProof;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Native committed effect removal revokes only its consumed-food observational origin. */
@Mixin(LivingEntity.class)
public abstract class WorldConsumedEffectRemovalQuestMixin {
    @Inject(method = "onEffectRemoved", at = @At("TAIL"))
    private void jem$nativeEffectRemoved(MobEffectInstance removed, CallbackInfo callback) {
        if ((Object)this instanceof ServerPlayer actor) {
            WorldMaelstromProof.removed(actor, removed);
            WorldHungerGuardProof.removed(actor, removed);
        }
    }
}
