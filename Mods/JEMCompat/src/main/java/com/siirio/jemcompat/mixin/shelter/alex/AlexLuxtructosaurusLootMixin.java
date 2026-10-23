package com.siirio.jemcompat.mixin.shelter.alex;

import com.github.alexmodguy.alexscaves.server.entity.living.LuxtructosaurusEntity;
import com.siirio.jemcompat.feature.shelter.ShelterEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Native delayed death animation populates a second loot buffer outside LivingDropsEvent. */
@Pseudo
@Mixin(value = LuxtructosaurusEntity.class, remap = false)
public abstract class AlexLuxtructosaurusLootMixin {
    @Inject(method = "populateDeathLootForLuxtructosaurus", at = @At("HEAD"), cancellable = true)
    private void jemcompat$suppressDeferredDeathLoot(CallbackInfo ci) {
        if (ShelterEvents.capturedDeath((LuxtructosaurusEntity) (Object) this)) ci.cancel();
    }
}
