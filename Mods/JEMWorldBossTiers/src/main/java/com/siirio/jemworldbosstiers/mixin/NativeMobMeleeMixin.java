package com.siirio.jemworldbosstiers.mixin;

import com.siirio.jemworldbosstiers.compat.cataclysm.NativeBossDamage;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Mob.class)
public abstract class NativeMobMeleeMixin {
    @Inject(method = "doHurtTarget(Lnet/minecraft/world/entity/Entity;)Z", at = @At("HEAD"))
    private void jem$prepareMelee(Entity target, CallbackInfoReturnable<Boolean> callback) {
        NativeBossDamage.flatFactor((LivingEntity) (Object) this);
    }

    // Vanilla's native method forms ATK + its own weapon enchantment bonus before this call.
    @Redirect(method = "doHurtTarget(Lnet/minecraft/world/entity/Entity;)Z",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"),
            require = 1, expect = 1)
    private boolean jem$nativeFlatAttack(Entity target, DamageSource source, float amount) {
        if (!(target instanceof LivingEntity living)) return target.hurt(source, amount);
        LivingEntity boss = (LivingEntity) (Object) this;
        return NativeBossDamage.hurt(boss, living, source, (float) (amount * NativeBossDamage.flatFactor(boss)));
    }
}
