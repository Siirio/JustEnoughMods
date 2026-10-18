package com.siirio.jemcompat.mixin.transport;

import com.siirio.jemcompat.feature.transport.AircraftPolicy;
import immersive_aircraft.entity.AirplaneEntity;
import immersive_aircraft.entity.AircraftEntity;
import immersive_aircraft.entity.VehicleEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = VehicleEntity.class, remap = false)
public abstract class AircraftLiftMixin {
    @Shadow protected float movementY;

    @Inject(method = "m_8119_()V", at = @At(value = "INVOKE", target = "Limmersive_aircraft/entity/VehicleEntity;updateController()V", shift = At.Shift.AFTER, remap = false), remap = false)
    private void jemcompat$responsiveClimb(CallbackInfo callback) {
        if (!((Object) this instanceof AircraftEntity vehicle) || !AircraftPolicy.enabled(vehicle)
                || !vehicle.isVehicle() || vehicle.getEnginePower() <= 0 || vehicle.isUnderWater()) return;
        // Plane Y input changes throttle/brakes; climb is its nose-up direction. Rotor/airship Y is native ascent.
        double climb = vehicle instanceof AirplaneEntity ? Math.max(0, vehicle.getForwardDirection().y) : Math.max(0, movementY);
        if (climb <= 0 || (vehicle instanceof AirplaneEntity && movementY < 0)) return;
        Vec3 velocity = vehicle.getDeltaMovement();
        double maximum = AircraftPolicy.MAX_ASCENT_SPEED.get() / 20.0;
        double y = Math.min(maximum, velocity.y + AircraftPolicy.ASCENT_ACCELERATION.get() * climb * vehicle.getEnginePower());
        vehicle.setDeltaMovement(velocity.x, y, velocity.z);
    }
}
