package com.siirio.jemworldbosstiers.mixin;

import com.siirio.jemworldbosstiers.compat.cataclysm.NativeBossDamage;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "com.github.L_Ender.cataclysm.entity.AnimationMonster.BossMonsters.Ignis_Entity", remap = false)
public abstract class CataclysmIgnisDamageMixin {
    // Exact 3.31 SRG call sites: every listed formula reads ATK separately from target max HP.
    // Scaling the input retains native coefficients, randomness, geometry and percent arithmetic.
    @Redirect(method = {"AreaAttack", "BodyCheckAttack", "Poke", "ShieldSmashDamage", "UltimateAttack", "Phase_Transition"},
            at = @At(value = "INVOKE", target = "Lcom/github/L_Ender/cataclysm/entity/AnimationMonster/BossMonsters/Ignis_Entity;m_21133_(Lnet/minecraft/world/entity/ai/attributes/Attribute;)D", remap = false),
            require = 6, expect = 6, remap = false)
    private double jem$flatInput(@Coerce LivingEntity boss, Attribute attribute) {
        return NativeBossDamage.attackInput(boss);
    }

    @Redirect(method = {"AreaAttack", "BodyCheckAttack", "Poke", "ShieldSmashDamage", "UltimateAttack", "Phase_Transition"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z", remap = true),
            require = 6, expect = 6, remap = false)
    private boolean jem$composedHit(LivingEntity target, DamageSource source, float amount) {
        return NativeBossDamage.hurt((LivingEntity) (Object) this, target, source, amount);
    }
}
