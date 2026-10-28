package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.travel.AircraftFuelObservation;
import com.justenoughmods.achievementguide.integration.travel.TravelProof;
import com.justenoughmods.achievementguide.integration.travel.TravelVehicleObservation;
import immersive_aircraft.entity.EngineVehicle;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = EngineVehicle.class, remap = false)
public abstract class AircraftTravelQuestMixin implements TravelVehicleObservation {
    @Shadow @Final private int[] fuel;
    @Unique private TravelProof.Journey jem$flight;
    @Unique private TravelProof.Journey jem$grounded;
    @Unique private ServerPlayer jem$fuelPilot;
    @Unique private long jem$beforeFuel;
    @Unique private long jem$beforeActiveFuel;
    @Unique private int jem$previousEngineTick = -1;
    @Unique private int jem$landedAt = -1;

    @Unique private EngineVehicle jem$aircraft() { return (EngineVehicle) (Object) this; }
    @Unique private boolean jem$biplane() {
        return BuiltInRegistries.ENTITY_TYPE.getKey(jem$aircraft().getType()).toString().equals("immersive_aircraft:biplane");
    }
    @Unique private long jem$fuel() {
        long total = 0;
        for (int value : fuel) total += Math.max(0, value);
        return total;
    }

    @Inject(method = "m_8119_()V", at = @At("TAIL"), remap = false)
    private void jem$afterEngine(CallbackInfo callback) {
        var vehicle = jem$aircraft();
        if (vehicle.level().isClientSide || !jem$biplane()) return;
        ServerPlayer currentPilot = TravelProof.controller(vehicle);
        if (jem$flight != null && (currentPilot != jem$flight.actor || vehicle.tickCount - jem$flight.began > 24000)) jem$flight = null;
        if (jem$grounded != null && (currentPilot != jem$grounded.actor || vehicle.tickCount - jem$grounded.began > 24000)) jem$grounded = null;
        long nativeFuel = jem$fuel();
        long remaining = AircraftFuelObservation.remaining(vehicle);
        // Adjacent completed native engine ticks avoid any dependency on other HEAD-injector ordering.
        if (jem$flight != null && jem$landedAt < 0 && !vehicle.onGround()
                && jem$fuelPilot == currentPilot && currentPilot == jem$flight.actor
                && jem$previousEngineTick >= jem$flight.began && vehicle.tickCount == jem$previousEngineTick + 1) {
            if (nativeFuel < jem$beforeFuel || jem$beforeActiveFuel > 0 && remaining >= 0 && remaining < jem$beforeActiveFuel) {
                jem$flight.fuelConsumed = true;
            }
        }
        jem$fuelPilot = currentPilot;
        jem$beforeFuel = nativeFuel;
        jem$beforeActiveFuel = remaining;
        jem$previousEngineTick = vehicle.tickCount;
        if (jem$flight != null && jem$landedAt >= 0) {
            if (!vehicle.onGround() || vehicle.isInWater() || vehicle.isUnderWater()
                    || jem$flight.last.distanceToSqr(vehicle.position()) > 1.0E-8
                    || !Float.isFinite(vehicle.getHealth()) || vehicle.getHealth() <= 0) {
                jem$flight = null;
            } else if (vehicle.tickCount - jem$landedAt >= 2) {
                if (jem$flight.fuelConsumed && jem$flight.reached(vehicle)) TravelProof.emit(currentPilot, "v3_world_air_route");
                jem$flight = null;
            }
        }
        if (jem$flight == null) jem$landedAt = -1;
    }

    @Override public void jem$travelAccepted(ServerPlayer actor, Vec3 from, boolean groundedBefore) {
        var vehicle = jem$aircraft();
        if (!jem$biplane() || vehicle.isRemoved() || !Float.isFinite(vehicle.getHealth()) || vehicle.getHealth() <= 0) {
            jem$flight = null;
            jem$grounded = null;
            return;
        }
        if (jem$flight != null && !jem$flight.valid(vehicle, actor, from)) jem$flight = null;
        if (jem$flight == null && groundedBefore && !vehicle.onGround() && jem$grounded != null
                && jem$grounded.valid(vehicle, actor, from)) {
            jem$flight = new TravelProof.Journey(actor, from, vehicle);
            jem$landedAt = -1;
        }
        jem$grounded = vehicle.onGround() ? new TravelProof.Journey(actor, vehicle.position(), vehicle) : null;
        if (jem$flight == null) return;
        jem$flight.last = vehicle.position();
        if (vehicle.onGround() && jem$landedAt < 0) {
            if (jem$flight.reached(vehicle)) jem$landedAt = vehicle.tickCount;
            else jem$flight = null;
        }
    }

    @Override public void jem$travelReset() { jem$flight = null; jem$grounded = null; jem$landedAt = -1; }
}
