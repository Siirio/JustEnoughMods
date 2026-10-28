package com.justenoughmods.achievementguide.integration.create;

import com.railwayteam.railways.content.conductor.ConductorEntity;
import com.railwayteam.railways.mixin.AccessorCarriage;
import com.railwayteam.railways.mixin_interfaces.ICarriageConductors;
import com.simibubi.create.Create;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.schedule.Schedule;
import com.simibubi.create.content.trains.schedule.ScheduleRuntime;
import com.simibubi.create.content.trains.schedule.destination.DestinationInstruction;
import com.simibubi.create.content.trains.station.GlobalStation;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.FakePlayer;

/** Native conductor identity from actual creation/assignment through actual scheduled train arrival. */
public final class CreateWhistleProofs {
    public static final String CREATOR = "jemQuestConductorCreatorV1";
    public static final String ASSIGNER = "jemQuestConductorAssignerV1";
    private CreateWhistleProofs() {}
    public static boolean eligible(ServerPlayer actor) { return actor != null && !(actor instanceof FakePlayer) && !actor.isSpectator() && !actor.getAbilities().instabuild; }
    private static boolean owner(CompoundTag data, UUID actor) {
        return data.hasUUID(CREATOR) && data.hasUUID(ASSIGNER) && actor.equals(data.getUUID(CREATOR)) && actor.equals(data.getUUID(ASSIGNER));
    }
    public static boolean active(Train train, UUID conductor) {
        if (train.carriages.isEmpty() || train.carriages.size() > 1024) return false;
        for (var carriage : train.carriages) {
            var controllers = ((ICarriageConductors)carriage).railways$getControllingConductors();
            if (controllers.size() <= 128 && controllers.contains(conductor)) return true;
        }
        return false;
    }
    public static boolean owner(Train train, UUID conductor, UUID actor) {
        if (!active(train, conductor)) return false;
        for (var carriage : train.carriages) {
            if (!((ICarriageConductors)carriage).railways$getControllingConductors().contains(conductor)) continue;
            var present = carriage.anyAvailableEntity();
            if (present != null && present.getPassengers().size() <= 64) for (Entity passenger : present.getPassengers()) {
                if (passenger instanceof ConductorEntity && passenger.getUUID().equals(conductor) && owner(passenger.getPersistentData(), actor)) return true;
            }
            var saved = ((AccessorCarriage)carriage).getSerialisedPassengers();
            if (saved.size() > 64) return false;
            for (CompoundTag passenger : saved.values()) {
                if (passenger.hasUUID("UUID") && passenger.getUUID("UUID").equals(conductor)
                        && passenger.getString("id").equals("railways:conductor") && owner(passenger.getCompound("ForgeData"), actor)) return true;
            }
        }
        return false;
    }
    public static void requested(ServerPlayer actor, ItemStack whistle, Schedule before) {
        if (!eligible(actor) || !whistle.hasTag() || !whistle.getTag().hasUUID("SelectedTrain") || !whistle.getTag().hasUUID("SelectedConductor")) return;
        Train train = Create.RAILWAYS.trains.get(whistle.getTag().getUUID("SelectedTrain"));
        UUID conductor = whistle.getTag().getUUID("SelectedConductor");
        if (train == null || train.graph == null || train.runtime.schedule == null || train.runtime.schedule == before
                || !train.runtime.isAutoSchedule || train.runtime.schedule.cyclic || train.runtime.schedule.entries.size() != 1
                || !(train.runtime.schedule.entries.get(0).instruction instanceof DestinationInstruction)
                || !owner(train, conductor, actor.getUUID())) return;
        String destination = train.runtime.schedule.entries.get(0).instruction.getData().getString("Text");
        if (destination.isBlank() || destination.length() > 256) return;
        var point = train.carriages.get(0).getLeadingPoint();
        if (point == null || point.edge == null || point.node1 == null || point.node2 == null) return;
        var position = point.getPosition(train.graph);
        CompoundTag proof = new CompoundTag(); proof.putUUID("actor", actor.getUUID()); proof.putUUID("conductor", conductor);
        proof.putString("station", destination); proof.put("schedule", train.runtime.schedule.write());
        proof.putDouble("x", position.x); proof.putDouble("y", position.y); proof.putDouble("z", position.z);
        ((CreateWhistleRun)train).jem$whistleProof(proof);
    }
    private static boolean valid(Train train, CompoundTag proof) {
        return proof.hasUUID("actor") && proof.hasUUID("conductor") && Create.RAILWAYS.trains.get(train.id) == train
                && !train.invalid && !train.derailed && train.runtime.schedule != null && train.runtime.isAutoSchedule && !train.runtime.paused
                && proof.getCompound("schedule").equals(train.runtime.schedule.write()) && active(train, proof.getUUID("conductor"));
    }
    public static void ticked(Train train, CompoundTag proof) {
        if (proof.isEmpty()) return;
        if (!valid(train, proof) || train.runtime.completed) { ((CreateWhistleRun)train).jem$whistleProof(new CompoundTag()); return; }
        if (train.currentStation != null || train.navigation.destination == null || train.runtime.state != ScheduleRuntime.State.IN_TRANSIT
                || Math.abs(train.speed) < .0001 || !train.navigation.destination.name.equals(proof.getString("station")) || train.graph == null) return;
        var point = train.carriages.get(0).getLeadingPoint();
        if (point == null || point.edge == null || point.node1 == null || point.node2 == null) return;
        var position = point.getPosition(train.graph);
        if (position.distanceToSqr(proof.getDouble("x"), proof.getDouble("y"), proof.getDouble("z")) <= .000001) return;
        if (proof.hasUUID("destination") && !train.navigation.destination.id.equals(proof.getUUID("destination"))) {
            ((CreateWhistleRun)train).jem$whistleProof(new CompoundTag()); return;
        }
        proof.putUUID("destination", train.navigation.destination.id); proof.putBoolean("moved", true);
        ((CreateWhistleRun)train).jem$whistleProof(proof);
    }
    public static UUID arriving(Train train, GlobalStation station, CompoundTag proof) {
        return valid(train, proof) && proof.getBoolean("moved") && proof.hasUUID("destination")
                && station.id.equals(proof.getUUID("destination")) && station.name.equals(proof.getString("station"))
                && train.runtime.state == ScheduleRuntime.State.IN_TRANSIT && owner(train, proof.getUUID("conductor"), proof.getUUID("actor"))
                ? proof.getUUID("actor") : null;
    }
}
