package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.magiclast.MagicLastSpellProof;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(LivingEntity.class)
public abstract class MagicLastEffectResultQuestMixin {
    @Inject(method = "heal", at = @At("HEAD"))
    private void jem$healBegin(float amount, CallbackInfo callback) { MagicLastSpellProof.healBegin((LivingEntity)(Object)this); }
    @Inject(method = "heal", at = @At("RETURN"))
    private void jem$healEnd(float amount, CallbackInfo callback) { MagicLastSpellProof.healEnd((LivingEntity)(Object)this); }
    @Inject(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z", at = @At("RETURN"))
    private void jem$effect(MobEffectInstance effect, Entity source, CallbackInfoReturnable<Boolean> callback) { if (callback.getReturnValue() && ((LivingEntity)(Object)this).hasEffect(effect.getEffect())) MagicLastSpellProof.outcome((Entity)(Object)this); }
}
