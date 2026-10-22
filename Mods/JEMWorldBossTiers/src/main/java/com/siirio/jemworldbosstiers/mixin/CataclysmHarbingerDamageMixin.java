package com.siirio.jemworldbosstiers.mixin;

import com.siirio.jemworldbosstiers.compat.cataclysm.NativeBossDamage;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.github.L_Ender.cataclysm.entity.AnimationMonster.BossMonsters.The_Harbinger_Entity", remap = false)
public abstract class CataclysmHarbingerDamageMixin {
    @Unique private int jem$nativeChargeRandom;

    @Inject(method = "blockbreak", at = @At("HEAD"), require = 1, remap = false)
    private void jem$prepareInputs(CallbackInfo callback) {
        jem$nativeChargeRandom = 0;
        NativeBossDamage.flatFactor((LivingEntity) (Object) this);
    }

    // Bytecode 3.31: ordinal zero selects a block effect; ordinal one is flat damage's 0..4 roll.
    @Redirect(method = "blockbreak", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/util/RandomSource;nextInt(I)I", ordinal = 1, remap = true),
            require = 1, expect = 1, remap = false)
    private int jem$captureFlatRoll(RandomSource random, int bound) {
        jem$nativeChargeRandom = random.nextInt(bound);
        return jem$nativeChargeRandom;
    }

    // Native sum: (ATK + roll) + min(ATK, maxHP * ChargeHpDamage).
    // Compose the flat correction while both min operands and the exact native roll are known.
    @Redirect(method = "blockbreak", at = @At(value = "INVOKE", target = "Ljava/lang/Math;min(DD)D"),
            require = 1, expect = 1, remap = false)
    private double jem$composeOperands(double attack, double percent) {
        int roll = jem$nativeChargeRandom;
        jem$nativeChargeRandom = 0;
        return Math.min(attack, percent) + (attack + roll)
                * (NativeBossDamage.flatFactor((LivingEntity) (Object) this) - 1);
    }

    @Redirect(method = "blockbreak", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z", remap = true),
            require = 1, expect = 1, remap = false)
    private boolean jem$composedHit(LivingEntity target, DamageSource source, float amount) {
        return NativeBossDamage.hurt((LivingEntity) (Object) this, target, source, amount);
    }
}
