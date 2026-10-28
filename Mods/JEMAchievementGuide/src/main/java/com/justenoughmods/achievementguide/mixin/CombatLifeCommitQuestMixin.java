package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.combattools.CombatLifeProof;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value = LivingEntity.class, priority = 500)
public abstract class CombatLifeCommitQuestMixin {
    @Shadow protected boolean dead;
    @Inject(method = "hurt", at = @At("HEAD"))
    private void jem$hurtBegin(DamageSource source, float amount, CallbackInfoReturnable<Boolean> callback) { CombatLifeProof.hurtBegin((LivingEntity)(Object)this); }
    @Inject(method = "hurt", at = @At("RETURN"))
    private void jem$hurtEnd(DamageSource source, float amount, CallbackInfoReturnable<Boolean> callback) { CombatLifeProof.hurtEnd((LivingEntity)(Object)this, callback.getReturnValue()); }
    @Inject(method = "die", at = @At("HEAD"))
    private void jem$deathBegin(DamageSource source, CallbackInfo callback) { CombatLifeProof.deathBegin((LivingEntity)(Object)this, dead, source); }
    @Inject(method = "die", at = @At("RETURN"))
    private void jem$deathEnd(DamageSource source, CallbackInfo callback) { CombatLifeProof.deathEnd((LivingEntity)(Object)this, dead); }
}
