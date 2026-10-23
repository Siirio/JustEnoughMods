package com.siirio.jemcompat.mixin.food;

import com.siirio.jemcompat.feature.food.FoodEvents;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Added fires after vanilla caches the old instance; detach before that lookup. */
@Mixin(LivingEntity.class)
public abstract class FoodEffectOwnershipMixin {
    @Inject(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z", at = @At("HEAD"))
    private void jemFoodBeforeNativeMerge(MobEffectInstance incoming, Entity source, CallbackInfoReturnable<Boolean> callback) {
        if ((Object) this instanceof ServerPlayer player) FoodEvents.beforeExternalAdd(player, incoming);
    }
}

