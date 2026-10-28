package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.create.CreateRunProofs;
import com.justenoughmods.achievementguide.integration.create.CreateTrainRun;
import com.justenoughmods.achievementguide.integration.create.CreateCargoProof;
import com.justenoughmods.achievementguide.integration.create.CreateCargoTransfers;
import net.minecraft.world.item.ItemStack;
import java.util.HashMap;
import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.graph.DimensionPalette;
import com.simibubi.create.content.trains.graph.TrackGraph;
import com.simibubi.create.content.trains.schedule.ScheduleRuntime;
import com.simibubi.create.content.trains.station.GlobalStation;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.simibubi.create.content.trains.entity.Train", remap = false)
public abstract class CreateTrainQuestMixin implements CreateTrainRun {
    @Unique private static final String JEM_TRIP = "jemQuestNativeTripV1";
    @Shadow public UUID currentStation;
    @Shadow public List<Carriage> carriages;
    @Shadow public ScheduleRuntime runtime;
    @Shadow public double speed;
    @Shadow public boolean invalid;
    @Shadow public boolean derailed;
    @Unique private UUID jem$assignedScheduleActor;
    @Unique private UUID jem$departure;
    @Unique private UUID jem$driver;
    @Unique private UUID jem$scheduledActor;
    @Unique private UUID jem$scheduledDestination;
    @Unique private boolean jem$moved;
    @Unique private final Map<Carriage, CreateCargoProof> jem$carriedItems = new HashMap<>();
    @Unique private UUID jem$cargoActor;
    @Unique private UUID jem$cargoStation;
    @Unique private UUID jem$arrivalActor;
    @Unique private String jem$arrivalAction;

    @Override public void jem$scheduleActor(UUID actor) {
        jem$assignedScheduleActor = actor;
        jem$scheduledActor = null;
        jem$scheduledDestination = null;
    }

    @Override public UUID jem$scheduleOperator() { return jem$assignedScheduleActor; }

    @Unique private void jem$cargoDeparture() {
        jem$carriedItems.clear();
        jem$cargoActor = null;
        jem$cargoStation = null;
        if (jem$driver == null && jem$scheduledActor == null) return;
        for (Carriage carriage : carriages) {
            if (carriage.storage == null || carriage.storage.getAllItems() == null) continue;
            var items = carriage.storage.getAllItems();
            for (int slot = 0; slot < items.getSlots(); slot++) {
                ItemStack item = items.getStackInSlot(slot);
                if (item.isEmpty()) continue;
                jem$carriedItems.put(carriage, new CreateCargoProof(carriage.storage.getVersion(), CreateCargoTransfers.fingerprint(item)));
                break;
            }
        }
    }

    @Override public boolean jem$cargoReady(Carriage carriage) {
        var carried = jem$carriedItems.get(carriage);
        return carried != null && jem$cargoActor != null && Objects.equals(jem$cargoStation, currentStation)
                && carriage.storage != null && carried.version() == carriage.storage.getVersion();
    }

    @Override public void jem$cargoDelivered(Carriage carriage, ItemStack item, int versionBefore) {
        var carried = jem$carriedItems.get(carriage);
        ServerLevel level = jem$level();
        if (carried == null || level == null || jem$cargoActor == null || !Objects.equals(jem$cargoStation, currentStation)
                || carried.version() != versionBefore || carriage.storage.getVersion() != versionBefore + 1
                || !carried.itemFingerprint().equals(CreateCargoTransfers.fingerprint(item))) return;
        CreateRunProofs.emit(level, jem$cargoActor, "create_train_cargo_delivery");
        jem$carriedItems.clear();
        jem$cargoActor = null;
        jem$cargoStation = null;
    }

    @Unique private UUID jem$nativeDriver() {
        UUID result = null;
        for (Carriage carriage : carriages) {
            var entity = carriage.anyAvailableEntity();
            if (entity == null) continue;
            UUID driver = entity.getControllingPlayer().orElse(null);
            if (driver == null) continue;
            if (result != null && !result.equals(driver)) return null;
            result = driver;
        }
        return result;
    }

    @Unique private ServerLevel jem$level() {
        for (Carriage carriage : carriages) {
            var entity = carriage.anyAvailableEntity();
            if (entity != null && ((Entity)(Object)entity).level() instanceof ServerLevel level) return level;
        }
        return null;
    }

    @Unique private void jem$clearTrip() {
        jem$departure = null;
        jem$driver = null;
        jem$scheduledActor = null;
        jem$scheduledDestination = null;
        jem$moved = false;
    }

    @Inject(method = "leaveStation", at = @At("HEAD"), remap = false)
    private void jem$departed(CallbackInfo callback) {
        if (currentStation == null || jem$level() == null) return;
        jem$clearTrip();
        jem$departure = currentStation;
        jem$driver = jem$nativeDriver();
        if (jem$driver == null && jem$assignedScheduleActor != null && runtime.schedule != null
                && !runtime.paused && !runtime.completed && runtime.state == ScheduleRuntime.State.IN_TRANSIT
                && runtime.train.navigation.destination != null) {
            jem$scheduledActor = jem$assignedScheduleActor;
            jem$scheduledDestination = runtime.train.navigation.destination.id;
        }
        jem$cargoDeparture();
    }

    @Inject(method = "tick", at = @At("TAIL"), remap = false)
    private void jem$nativeTravel(Level world, CallbackInfo callback) {
        if (world.isClientSide || jem$departure == null || currentStation != null) return;
        UUID driver = jem$nativeDriver();
        if (invalid || derailed || jem$driver != null && !Objects.equals(jem$driver, driver)
                || jem$scheduledActor != null && (driver != null || runtime.schedule == null || runtime.paused || runtime.completed)) {
            jem$clearTrip();
            jem$carriedItems.clear();
            jem$cargoActor = null;
            jem$cargoStation = null;
            return;
        }
        if (Math.abs(speed) > .0001D) jem$moved = true;
    }

    @Inject(method = "arriveAt", at = @At("HEAD"), remap = false)
    private void jem$arriving(GlobalStation station, CallbackInfo callback) {
        jem$arrivalActor = null;
        jem$arrivalAction = null;
        if (jem$departure == null || !jem$moved || jem$departure.equals(station.id) || invalid || derailed) return;
        if (jem$driver != null && jem$driver.equals(jem$nativeDriver())) {
            jem$arrivalActor = jem$driver;
            jem$arrivalAction = "create_train_trip";
        } else if (jem$scheduledActor != null && station.id.equals(jem$scheduledDestination)
                && runtime.schedule != null && !runtime.paused && !runtime.completed
                && runtime.state == ScheduleRuntime.State.IN_TRANSIT && jem$nativeDriver() == null) {
            jem$arrivalActor = jem$scheduledActor;
            jem$arrivalAction = "create_scheduled_arrival";
        }
    }

    @Inject(method = "arriveAt", at = @At("TAIL"), remap = false)
    private void jem$arrived(GlobalStation station, CallbackInfo callback) {
        ServerLevel level = jem$level();
        if (level != null && jem$arrivalActor != null && station.id.equals(currentStation)) {
            CreateRunProofs.emit(level, jem$arrivalActor, jem$arrivalAction);
            jem$cargoActor = jem$arrivalActor;
            jem$cargoStation = station.id;
            jem$carriedItems.entrySet().removeIf(entry -> entry.getKey().storage.getVersion() != entry.getValue().version());
        } else {
            jem$carriedItems.clear();
            jem$cargoActor = null;
            jem$cargoStation = null;
        }
        jem$arrivalActor = null;
        jem$arrivalAction = null;
        jem$clearTrip();
    }

    @Override public void jem$writeTrip(CompoundTag tag) {
        CompoundTag proof = new CompoundTag();
        if (jem$assignedScheduleActor != null) proof.putUUID("schedule_actor", jem$assignedScheduleActor);
        if (jem$departure != null) proof.putUUID("departure", jem$departure);
        if (jem$driver != null) proof.putUUID("driver", jem$driver);
        if (jem$scheduledActor != null) proof.putUUID("trip_schedule_actor", jem$scheduledActor);
        if (jem$scheduledDestination != null) proof.putUUID("scheduled_destination", jem$scheduledDestination);
        proof.putBoolean("moved", jem$moved);
        tag.put(JEM_TRIP, proof);
    }

    @Override public void jem$readTrip(CompoundTag tag) {
        jem$clearTrip();
        // Native cargo version counters are not saved. Reloads cannot prove continuity.
        jem$carriedItems.clear();
        jem$cargoActor = null;
        jem$cargoStation = null;
        CompoundTag proof = tag.getCompound(JEM_TRIP);
        jem$assignedScheduleActor = proof.hasUUID("schedule_actor") ? proof.getUUID("schedule_actor") : null;
        jem$departure = proof.hasUUID("departure") ? proof.getUUID("departure") : null;
        jem$driver = proof.hasUUID("driver") ? proof.getUUID("driver") : null;
        jem$scheduledActor = proof.hasUUID("trip_schedule_actor") ? proof.getUUID("trip_schedule_actor") : null;
        jem$scheduledDestination = proof.hasUUID("scheduled_destination") ? proof.getUUID("scheduled_destination") : null;
        jem$moved = proof.getBoolean("moved");
    }

    @Inject(method = "write", at = @At("RETURN"), remap = false)
    private void jem$save(DimensionPalette palette, CallbackInfoReturnable<CompoundTag> callback) {
        jem$writeTrip(callback.getReturnValue());
    }

    @Inject(method = "read", at = @At("RETURN"), remap = false)
    private static void jem$load(CompoundTag tag, Map<UUID, TrackGraph> graphs, DimensionPalette palette,
                                 CallbackInfoReturnable<Train> callback) {
        ((CreateTrainRun)callback.getReturnValue()).jem$readTrip(tag);
    }
}
