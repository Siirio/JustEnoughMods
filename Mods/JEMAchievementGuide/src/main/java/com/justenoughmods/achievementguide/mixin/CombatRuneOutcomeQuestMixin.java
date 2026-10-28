package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.combattools.CombatRuneProof;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(LivingEntity.class)
public abstract class CombatRuneOutcomeQuestMixin {
    @Inject(method = "heal", at = @At("HEAD"))
    private void jem$beforeHeal(float amount, CallbackInfo callback) { CombatRuneProof.healBegin((LivingEntity)(Object)this); }
    @Inject(method = "heal", at = @At("RETURN"))
    private void jem$healed(float amount, CallbackInfo callback) { CombatRuneProof.healEnd((LivingEntity)(Object)this); }
    @Inject(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z", at = @At("RETURN"))
    private void jem$effect(MobEffectInstance effect, Entity source, CallbackInfoReturnable<Boolean> callback) { if (callback.getReturnValue() && ((LivingEntity)(Object)this).hasEffect(effect.getEffect())) CombatRuneProof.committed((Entity)(Object)this); }
    @Inject(method = "removeEffect", at = @At("RETURN"))
    private void jem$cleanse(MobEffect effect, CallbackInfoReturnable<Boolean> callback) { if (callback.getReturnValue()) CombatRuneProof.committed((Entity)(Object)this); }
}
