package com.siirio.jemworldbosstiers.mixin;

import com.siirio.jemworldbosstiers.compat.cataclysm.NativeBossDamage;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "com.github.L_Ender.cataclysm.entity.projectile.Death_Laser_Beam_Entity", remap = false)
public abstract class CataclysmDeathLaserDamageMixin {
    @Shadow(remap = false) public LivingEntity caster;
    @Shadow(remap = false) public abstract float getDamage();

    // Native damage = configFlat + min(configFlat, target.maxHP * configHP * .01).
    // Harbinger DeathLaserGoal supplies CMCommonConfig.Harbinger.DeathLaserdamage, not ATK.
    @Redirect(method = "m_8119_", at = @At(value = "INVOKE",
            target = "Lcom/github/L_Ender/cataclysm/entity/projectile/Death_Laser_Beam_Entity;getDamage()F", ordinal = 0),
            require = 1, expect = 1, remap = false)
    private float jem$fixedFlat(@Coerce Object beam) {
        return caster == null ? getDamage() : (float) (getDamage() * NativeBossDamage.fixedFactor(caster));
    }

    @Redirect(method = "m_8119_", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z", remap = true),
            require = 1, expect = 1, remap = false)
    private boolean jem$composedHit(LivingEntity target, DamageSource source, float amount) {
        return caster == null ? target.hurt(source, amount) : NativeBossDamage.hurt(caster, target, source, amount);
    }
}
