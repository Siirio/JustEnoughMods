package com.siirio.jemcompat.mixin.transport;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "immersive_aircraft.entity.EngineVehicle", remap = false)
public abstract class EngineVehicleFuelMixin {
    private static final float GROUNDED_FUEL_MULTIPLIER = 0.25F;

    // Native refuel feeds the engine from its onboard inventory, including during flight.
    @Inject(method = "getFuelConsumption()F", at = @At("RETURN"), cancellable = true)
    private void jemcompat$groundedFuelConsumption(CallbackInfoReturnable<Float> callback) {
        if (((Entity) (Object) this).onGround()) {
            callback.setReturnValue(callback.getReturnValueF() * GROUNDED_FUEL_MULTIPLIER);
        }
    }
}
