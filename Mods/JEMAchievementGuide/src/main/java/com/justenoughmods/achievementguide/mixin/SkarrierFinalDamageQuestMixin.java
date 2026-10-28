package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.SkarrierWorkshopProof;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(LivingEntity.class)
public abstract class SkarrierFinalDamageQuestMixin {
    @Shadow protected boolean dead;
    @Inject(method = "hurt", at = @At("HEAD"))
    private void jem$begin(DamageSource source, float amount, CallbackInfoReturnable<Boolean> callback) { SkarrierWorkshopProof.hurtBegin((LivingEntity)(Object)this); }
    @Inject(method = "hurt", at = @At("RETURN"))
    private void jem$end(DamageSource source, float amount, CallbackInfoReturnable<Boolean> callback) { SkarrierWorkshopProof.hurtEnd((LivingEntity)(Object)this, callback.getReturnValue()); }
    @Inject(method = "causeFallDamage", at = @At("HEAD"))
    private void jem$fallBegin(float distance, float multiplier, DamageSource source, CallbackInfoReturnable<Boolean> callback) {
        SkarrierWorkshopProof.fallBegin((LivingEntity)(Object)this);
    }
    @Inject(method = "causeFallDamage", at = @At("RETURN"))
    private void jem$fallEnd(float distance, float multiplier, DamageSource source, CallbackInfoReturnable<Boolean> callback) {
        SkarrierWorkshopProof.fallEnd((LivingEntity)(Object)this);
    }
    @Inject(method = "die", at = @At("HEAD"))
    private void jem$deathBegin(DamageSource source, CallbackInfo callback) { SkarrierWorkshopProof.deathBegin((LivingEntity)(Object)this, dead); }
    @Inject(method = "die", at = @At("RETURN"))
    private void jem$deathEnd(DamageSource source, CallbackInfo callback) { SkarrierWorkshopProof.deathEnd((LivingEntity)(Object)this, dead); }
}
