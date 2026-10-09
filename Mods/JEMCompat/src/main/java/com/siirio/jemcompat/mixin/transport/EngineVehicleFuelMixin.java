package com.siirio.jemcompat.mixin.transport;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "immersive_aircraft.entity.EngineVehicle", remap = false)
public abstract class EngineVehicleFuelMixin {
    private static final double MAXIMUM_REFUEL_SPEED = 0.5 / 20.0;
    private static final float GROUNDED_FUEL_MULTIPLIER = 0.25F;

    @Inject(method = "refuel()V", at = @At("HEAD"), cancellable = true)
    private void jemcompat$restrictRefueling(CallbackInfo callback) {
        Entity aircraft = (Entity) (Object) this;
        if (!aircraft.onGround() || aircraft.getDeltaMovement().horizontalDistanceSqr() >= MAXIMUM_REFUEL_SPEED * MAXIMUM_REFUEL_SPEED) {
            callback.cancel();
        }
    }

    @Inject(method = "getFuelConsumption()F", at = @At("RETURN"), cancellable = true)
    private void jemcompat$groundedFuelConsumption(CallbackInfoReturnable<Float> callback) {
        if (((Entity) (Object) this).onGround()) {
            callback.setReturnValue(callback.getReturnValueF() * GROUNDED_FUEL_MULTIPLIER);
        }
    }
}
