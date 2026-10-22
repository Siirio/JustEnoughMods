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
@Mixin(targets = "com.github.L_Ender.cataclysm.entity.AnimationMonster.BossMonsters.The_Leviathan.The_Leviathan_Entity", remap = false)
public abstract class CataclysmLeviathanDamageMixin {
    // 3.31: first ATK read forms flat damage. A second read caps native percent damage.
    // Only ordinal zero changes; the native Math.min(ATK, maxHP * coefficient) remains intact.
    @Redirect(method = {"TailWhips", "chargeDamage", "biteattack", "Tentacleattack", "TentacleHoldattack"},
            at = @At(value = "INVOKE", target = "Lcom/github/L_Ender/cataclysm/entity/AnimationMonster/BossMonsters/The_Leviathan/The_Leviathan_Entity;m_21133_(Lnet/minecraft/world/entity/ai/attributes/Attribute;)D", ordinal = 0, remap = false),
            require = 5, expect = 5, remap = false)
    private double jem$flatInput(@Coerce LivingEntity boss, Attribute attribute) {
        return NativeBossDamage.attackInput(boss);
    }

    @Redirect(method = {"TailWhips", "chargeDamage", "biteattack", "Tentacleattack", "TentacleHoldattack"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z", remap = true),
            require = 5, expect = 5, remap = false)
    private boolean jem$composedHit(LivingEntity target, DamageSource source, float amount) {
        return NativeBossDamage.hurt((LivingEntity) (Object) this, target, source, amount);
    }
}

