package com.siirio.jemcompat.mixin.transport;

import com.siirio.jemcompat.feature.transport.AircraftFuelInventory;
import com.siirio.jemcompat.feature.transport.AircraftFuelState;
import com.siirio.jemcompat.feature.transport.AircraftPolicy;
import com.siirio.jemcompat.feature.transport.TransportContent;
import immersive_aircraft.cobalt.registration.CobaltFuelRegistry;
import immersive_aircraft.config.Config;
import immersive_aircraft.entity.EngineVehicle;
import immersive_aircraft.entity.inventory.VehicleInventoryDescription;
import immersive_aircraft.util.Utils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** IA 1.5.0 owns controls/presentation; this adapter owns the dedicated server fuel clock. */
@Mixin(value = EngineVehicle.class, remap = false)
public abstract class EngineVehicleFuelMixin implements AircraftFuelState {
    @Shadow @Final private int[] fuel;
    @Shadow @Final private static EntityDataAccessor<Float> UTILIZATION;
    @Shadow @Final private static EntityDataAccessor<Boolean> LOW_ON_FUEL;
    @Unique private long jemcompat$remainingNanos;
    @Unique private long jemcompat$lastTickNanos;
    @Unique private long jemcompat$lastHudNanos;
    @Unique private boolean jemcompat$wasWorking;

    @Unique private EngineVehicle jemcompat$vehicle() { return (EngineVehicle) (Object) this; }
    @Unique private boolean jemcompat$enabled() { return AircraftPolicy.enabled(jemcompat$vehicle()) && fuel.length > 0; }

    @Override
    public int jemcompat$burningUnits() {
        return (int) Math.min(Integer.MAX_VALUE, (jemcompat$remainingNanos + AircraftPolicy.UNIT_NANOS - 1) / AircraftPolicy.UNIT_NANOS);
    }

    @Unique
    private boolean jemcompat$takeUnit() {
        var vehicle = jemcompat$vehicle();
        for (var slot : vehicle.getInventoryDescription().getSlots(VehicleInventoryDescription.BOILER)) {
            var stack = vehicle.getInventory().getItem(slot.index());
            if (Utils.getFuelTime(stack) <= 0) continue;
            var remainder = CobaltFuelRegistry.INSTANCE.getCraftingRemainingItem(stack);
            vehicle.getInventory().removeItem(slot.index(), 1);
            if (!remainder.isEmpty()) vehicle.spawnAtLocation(remainder);
            jemcompat$remainingNanos += AircraftPolicy.UNIT_NANOS;
            return true;
        }
        return false;
    }

    @Inject(method = "m_8119_()V", at = @At("HEAD"), remap = false)
    private void jemcompat$activeClock(CallbackInfo callback) {
        var vehicle = jemcompat$vehicle();
        if (vehicle.level().isClientSide || !jemcompat$enabled()) return;
        var inventory = (AircraftFuelInventory) vehicle.getInventory();
        inventory.reconcileCapacity();
        long now = System.nanoTime();
        long elapsed = jemcompat$lastTickNanos == 0 ? 0 : Math.max(0, now - jemcompat$lastTickNanos);
        jemcompat$lastTickNanos = now;
        boolean exempt = Config.getInstance().fuelConsumption == 0 || (!Config.getInstance().burnFuelInCreative && vehicle.isPilotCreative());
        if (jemcompat$wasWorking && !exempt) {
            // A delayed server tick still represents real active engine time.
            while (elapsed > 0) {
                if (jemcompat$remainingNanos == 0 && !jemcompat$takeUnit()) break;
                long used = Math.min(elapsed, jemcompat$remainingNanos);
                jemcompat$remainingNanos -= used;
                elapsed -= used;
            }
        }
        jemcompat$wasWorking = vehicle.isVehicle() && (vehicle.getEngineTarget() > 0 || vehicle.enginePower.getSmooth() > 0.0001F)
                && (jemcompat$remainingNanos > 0 || inventory.storedUnits() > 0);
        if ((jemcompat$lastHudNanos == 0 || now - jemcompat$lastHudNanos >= 1_000_000_000L) && vehicle.getControllingPassenger() instanceof ServerPlayer pilot) {
            jemcompat$lastHudNanos = now;
            long seconds = (jemcompat$remainingNanos + 999_999_999L) / 1_000_000_000L + inventory.storedUnits() * 90L;
            pilot.displayClientMessage(Component.translatable("message.jemcompat.aircraft.fuel", inventory.storedUnits() + jemcompat$burningUnits(),
                    AircraftPolicy.CAPACITY, seconds / 60, String.format(java.util.Locale.ROOT, "%02d", seconds % 60)), true);
        }
    }

    @Inject(method = "consumeFuel(F)F", at = @At("HEAD"), cancellable = true)
    private void jemcompat$disableTickFuel(float amount, CallbackInfoReturnable<Float> callback) {
        if (jemcompat$enabled()) callback.setReturnValue(0F);
    }

    @Inject(method = "refuel()V", at = @At("HEAD"), cancellable = true)
    private void jemcompat$disablePrefill(CallbackInfo callback) {
        if (jemcompat$enabled()) callback.cancel();
    }

    @Inject(method = "getFuelUtilization()F", at = @At("HEAD"), cancellable = true)
    private void jemcompat$projectUtilization(CallbackInfoReturnable<Float> callback) {
        var vehicle = jemcompat$vehicle();
        if (vehicle.level().isClientSide || !jemcompat$enabled()) return;
        boolean available = jemcompat$remainingNanos > 0 || ((AircraftFuelInventory) vehicle.getInventory()).storedUnits() > 0;
        boolean exempt = Config.getInstance().fuelConsumption == 0 || (!Config.getInstance().burnFuelInCreative && vehicle.isPilotCreative());
        float value = available || exempt ? 1F : 0F;
        vehicle.getEntityData().set(UTILIZATION, value);
        callback.setReturnValue(value);
    }

    @Inject(method = "isFuelLow()Z", at = @At("HEAD"), cancellable = true)
    private void jemcompat$projectLowFuel(CallbackInfoReturnable<Boolean> callback) {
        var vehicle = jemcompat$vehicle();
        if (vehicle.level().isClientSide || !jemcompat$enabled()) return;
        boolean low = jemcompat$remainingNanos + ((AircraftFuelInventory) vehicle.getInventory()).storedUnits() * AircraftPolicy.UNIT_NANOS <= AircraftPolicy.UNIT_NANOS;
        vehicle.getEntityData().set(LOW_ON_FUEL, low);
        callback.setReturnValue(low);
    }

    @Inject(method = "m_7380_(Lnet/minecraft/nbt/CompoundTag;)V", at = @At("TAIL"), remap = false)
    private void jemcompat$saveClock(CompoundTag tag, CallbackInfo callback) {
        jemcompat$writeFuel(tag);
    }

    @Inject(method = "m_7378_(Lnet/minecraft/nbt/CompoundTag;)V", at = @At("TAIL"), remap = false)
    private void jemcompat$readClock(CompoundTag tag, CallbackInfo callback) {
        jemcompat$readFuel(tag);
    }

    @Override
    public void jemcompat$writeFuel(CompoundTag tag) {
        if (jemcompat$enabled()) tag.putLong("JemActiveFuelNanos", jemcompat$remainingNanos);
    }

    @Override
    public void jemcompat$readFuel(CompoundTag tag) {
        if (!jemcompat$enabled()) return;
        if (tag.contains("JemActiveFuelNanos")) {
            jemcompat$remainingNanos = Math.min(AircraftPolicy.CAPACITY * AircraftPolicy.UNIT_NANOS, Math.max(0, tag.getLong("JemActiveFuelNanos")));
        } else {
            // Retain the unspent fraction of old native configured fuel units, including multiple boilers.
            int unit = Math.max(1, Utils.getFuelTime(TransportContent.AIRCRAFT_FUEL.get().getDefaultInstance()));
            for (int value : fuel) jemcompat$remainingNanos += Math.max(0, value) * (AircraftPolicy.UNIT_NANOS / unit);
        }
        java.util.Arrays.fill(fuel, 0);
        jemcompat$pauseFuelClock();
    }

    @Override
    public void jemcompat$pauseFuelClock() {
        jemcompat$lastTickNanos = 0;
        jemcompat$wasWorking = false;
    }
}
