package com.siirio.jemfood.mixin;

import com.siirio.jemfood.FoodEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Only the actual vanilla poison operation is a poison hit, not other magic during poisoning. */
@Mixin(MobEffect.class)
public abstract class PoisonMixin {
    @Redirect(method = "applyEffectTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private boolean nativePoison(LivingEntity entity, DamageSource source, float amount) {
        return FoodEvents.poisonDamage(entity, source, amount, (Object) this == MobEffects.POISON);
    }
}
