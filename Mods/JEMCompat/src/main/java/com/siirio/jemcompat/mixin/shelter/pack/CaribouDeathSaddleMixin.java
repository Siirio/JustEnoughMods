package com.siirio.jemcompat.mixin.shelter.pack;

import com.siirio.jemcompat.feature.shelter.ShelterEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** This native procedure emits a saddle through addFreshEntity after normal death loot capture. */
@Pseudo
@Mixin(targets = "net.imasillylittleguy.cnc.procedures.CaribouEntityDiesProcedure", remap = false)
public abstract class CaribouDeathSaddleMixin {
    @Inject(method = "execute", at = @At("HEAD"), cancellable = true, remap = false)
    private static void jemNoDuplicateSaddle(LevelAccessor level, double x, double y, double z, Entity animal, CallbackInfo callback) {
        if (animal != null && ShelterEvents.capturedDeath(animal)) callback.cancel();
    }
}
