package com.siirio.jemworldbosstiers.mixin;

import net.minecraft.advancements.critereon.KilledTrigger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KilledTrigger.class)
public abstract class KilledTriggerMixin {
    @Inject(method = "trigger", at = @At("HEAD"), cancellable = true)
    private void jem$excludeEventKill(ServerPlayer player, Entity entity, DamageSource source, CallbackInfo callback) {
        if (entity.getPersistentData().getBoolean("jem:event_spawned")
                || "RAID_EVENT".equals(entity.getPersistentData().getCompound("jem_world_boss_tiers").getString("Provenance")))
            callback.cancel();
    }
}
