package com.siirio.jemworldbosstiers.mixin;

import com.siirio.jemworldbosstiers.compat.cataclysm.NativeBossDamage;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.github.L_Ender.cataclysm.entity.InternalAnimationMonster.IABossMonsters.Maledictus.Maledictus_Entity", remap = false)
public abstract class CataclysmMaledictusDamageMixin {
    @Inject(method = {"m_8107_", "ComboAreaAttack", "Grab", "ShieldSmashDamage", "Rushattack", "uppercut", "AreaAttack"},
            at = @At("HEAD"), require = 7, remap = false)
    private void jem$prepareInputs(CallbackInfo callback) {
        NativeBossDamage.flatFactor((LivingEntity) (Object) this);
    }

    // All 11 native additions are flat + min(flat, maxHP * coefficient).
    // At min's call site both original operands are still separate. Add only the flat
    // correction here, before the native fadd; never infer either operand from event damage.
    @Redirect(method = {"m_8107_", "ComboAreaAttack", "Grab", "ShieldSmashDamage", "Rushattack", "uppercut", "AreaAttack"},
            at = @At(value = "INVOKE", target = "Ljava/lang/Math;min(FF)F"),
            require = 11, expect = 11, remap = false)
    private float jem$composeOperands(float flat, float percent) {
        return (float) (Math.min(flat, percent) + flat * (NativeBossDamage.flatFactor((LivingEntity) (Object) this) - 1));
    }

    @Redirect(method = {"m_8107_", "ComboAreaAttack", "Grab", "ShieldSmashDamage", "Rushattack", "uppercut", "AreaAttack"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z", remap = true),
            require = 11, expect = 11, remap = false)
    private boolean jem$composedHit(LivingEntity target, DamageSource source, float amount) {
        return NativeBossDamage.hurt((LivingEntity) (Object) this, target, source, amount);
    }
}
