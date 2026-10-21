package com.siirio.jemserver.mixin.staging;

import com.siirio.jemworldbosstiers.api.HostedEncounterApi;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public abstract class HostedBossTickMixin {
    // NoAI stops native goals, but custom boss tick overrides also run combat and presentation.
    @Inject(method = "tickNonPassenger", at = @At("HEAD"), cancellable = true)
    private void jem$waitForHostedStart(Entity entity, CallbackInfo callback) {
        Entity owner = HostedEncounterApi.damageOwner(entity);
        if (owner instanceof LivingEntity boss && HostedEncounterApi.held(boss)) {
            if (entity == boss) HostedEncounterApi.hold(boss);
            callback.cancel();
        }
    }
}
