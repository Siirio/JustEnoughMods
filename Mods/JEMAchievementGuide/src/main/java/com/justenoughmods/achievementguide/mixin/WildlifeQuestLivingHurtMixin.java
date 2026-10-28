package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.wildlife.WildlifeQuestHurt;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class WildlifeQuestLivingHurtMixin {
    @Inject(method = "hurt", at = @At("HEAD"))
    private void jem$before(DamageSource source, float amount, CallbackInfoReturnable<Boolean> callback) { WildlifeQuestHurt.begin((LivingEntity)(Object)this, source); }
    @Inject(method = "hurt", at = @At("RETURN"))
    private void jem$after(DamageSource source, float amount, CallbackInfoReturnable<Boolean> callback) { WildlifeQuestHurt.end((LivingEntity)(Object)this, source); }
}
