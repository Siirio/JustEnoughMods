package com.siirio.mobhome.mixin.shelter.pack;

import com.siirio.mobhome.feature.shelter.CompanionAdapters;
import dev.xylonity.knightlib.api.event.impl.server.LivingDeathEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Native Totem handles all captured tameables, not just the Companions namespace. */
@Pseudo
@Mixin(targets = "dev.xylonity.companions.common.event.CompanionsServerEvents", remap = false)
public abstract class CompanionsRespawnOwnerMixin {
    @Inject(method = "onDeath", at = @At("HEAD"), cancellable = true, remap = false)
    private static void jemShelterOwnsThisDeath(LivingDeathEvent event, CallbackInfo callback) {
        // Skip only the competing native listener, before its chunk lookup, UUID removal or queue insertion.
        // The actual death event and native Totem configuration/charges remain untouched.
        if (CompanionAdapters.bound(event.getEntity())) callback.cancel();
    }
}
