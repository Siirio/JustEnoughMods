package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.battle.WorldBattleExtraMovement;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "com.github.L_Ender.cataclysm.capabilities.ChargeCapability$ChargeCapabilityImp", remap = false)
public abstract class WorldBattleChargeImpactQuestMixin {
    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;m_6469_(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private boolean jem$nativeImpact(LivingEntity target, DamageSource source, float amount, LivingEntity actor) {
        float before = target.getHealth(); boolean accepted = target.hurt(source, amount);
        if (accepted) WorldBattleExtraMovement.impact(actor, target, source, before, "bulwark");
        return accepted;
    }
}
