package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.creaturecare.CreatureCareProof;
import com.justenoughmods.achievementguide.integration.creaturecare.CreatureCareRide;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "org.primal.entity.animal.WalrusEntity", remap = false)
public abstract class CreatureCareWalrusAttackMixin {
    @Redirect(method = "doSlamAttackDamage", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;m_147240_(DDD)V"))
    private void jem$slamKnockback(LivingEntity target, double strength, double x, double z) {
        var before = target.getDeltaMovement(); target.knockback(strength, x, z);
        var self = (AbstractHorse)(Object)this;
        if (!CreatureCareProof.data(self).getBoolean("walrus_command_water") && !before.equals(target.getDeltaMovement())) CreatureCareRide.affected(self);
    }
    @Redirect(method = "doSlamAttackDamage", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;m_6469_(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private boolean jem$slamDamage(Entity target, DamageSource source, float amount) {
        float health = target instanceof LivingEntity living ? living.getHealth() : 0;
        boolean accepted = target.hurt(source, amount); var self = (AbstractHorse)(Object)this;
        if (accepted && !CreatureCareProof.data(self).getBoolean("walrus_command_water")
                && (!(target instanceof LivingEntity living) || living.getHealth() < health)) CreatureCareRide.affected(self);
        return accepted;
    }
    @Redirect(method = "m_6727_", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;m_6469_(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private boolean jem$whirlDamage(LivingEntity target, DamageSource source, float amount) {
        float health = target.getHealth(); boolean accepted = target.hurt(source, amount); var self = (AbstractHorse)(Object)this;
        if (accepted && health > target.getHealth() && CreatureCareProof.data(self).getBoolean("walrus_command_water")) CreatureCareRide.affected(self);
        return accepted;
    }
}
