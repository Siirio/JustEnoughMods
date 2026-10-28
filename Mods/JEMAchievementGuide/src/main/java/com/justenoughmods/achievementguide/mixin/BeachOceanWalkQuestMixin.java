package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.beach.BeachTraversalProof;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.satisfy.beachparty.core.effect.OceanWalkEffect", remap = false)
public abstract class BeachOceanWalkQuestMixin {
    @Inject(method = "m_6742_(Lnet/minecraft/world/entity/LivingEntity;I)V", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;m_6853_(Z)V", shift = At.Shift.AFTER), remap = false)
    private void jem$actualWaterSupport(LivingEntity entity, int amplifier, CallbackInfo callback) {
        if (entity instanceof ServerPlayer player && player.connection instanceof BeachTraversalProof.Access access) {
            access.jem$nativeBeachTraversal().supported(player);
        }
    }
}
