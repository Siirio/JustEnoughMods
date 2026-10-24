package com.siirio.jemfood.mixin;

import com.siirio.jemfood.FoodState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEffectMixin {
    @Inject(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z", at = @At("HEAD"))
    private void beforeMerge(MobEffectInstance incoming, Entity source, CallbackInfoReturnable<Boolean> callback) {
        if ((Object) this instanceof ServerPlayer player) FoodState.external(player, incoming);
    }
    // forceAddEffect replaces the active entry directly. Relinquish the old native identity first.
    @Inject(method = "forceAddEffect", at = @At("HEAD"))
    private void beforeForce(MobEffectInstance incoming, Entity source, org.spongepowered.asm.mixin.injection.callback.CallbackInfo callback) {
        if ((Object) this instanceof ServerPlayer player) FoodState.external(player, incoming);
    }
}
