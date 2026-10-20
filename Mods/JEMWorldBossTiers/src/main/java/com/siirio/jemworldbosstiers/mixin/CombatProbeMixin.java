package com.siirio.jemworldbosstiers.mixin;

import com.siirio.jemworldbosstiers.diagnostic.CombatProbe;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class CombatProbeMixin {
    @Inject(method = "hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z", at = @At("HEAD"))
    private void jem$beginHit(DamageSource source, float amount, CallbackInfoReturnable<Boolean> callback) {
        LivingEntity target = (LivingEntity) (Object) this;
        if (!(target.level() instanceof ServerLevel level) || !level.getServer().isDedicatedServer()) return;
        CombatProbe.beginHit(target, source, amount);
    }

    @Inject(method = "hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z", at = @At("RETURN"))
    private void jem$finishHit(DamageSource source, float amount, CallbackInfoReturnable<Boolean> callback) {
        LivingEntity target = (LivingEntity) (Object) this;
        if (!(target.level() instanceof ServerLevel level) || !level.getServer().isDedicatedServer()) return;
        CombatProbe.finishHit(target, callback.getReturnValue());
    }
}
