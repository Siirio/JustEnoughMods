package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.create.CreateRunProofs;
import com.justenoughmods.achievementguide.integration.create.CreateTrainRun;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.schedule.Schedule;
import com.simibubi.create.content.trains.schedule.ScheduleRuntime;
import com.simibubi.create.content.trains.station.GlobalStation;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.simibubi.create.content.trains.entity.Navigation", remap = false)
public abstract class CreateSignalQuestMixin {
    @Shadow public Train train;
    @Shadow public GlobalStation destination;
    @Shadow public net.createmod.catnip.data.Pair<UUID, Boolean> waitingForSignal;
    @Shadow public int ticksWaitingForSignal;
    @Unique private Schedule jem$waitingSchedule;
    @Unique private GlobalStation jem$waitingDestination;
    @Unique private UUID jem$waitingActor;

    @Inject(method = "tick", at = @At("TAIL"), remap = false)
    private void jem$nativeObstructionCleared(Level world, CallbackInfo callback) {
        if (!(world instanceof ServerLevel level)) return;
        UUID actor = ((CreateTrainRun)train).jem$scheduleOperator();
        boolean scheduled = actor != null && train.runtime.schedule != null && !train.runtime.paused
                && !train.runtime.completed && train.runtime.state == ScheduleRuntime.State.IN_TRANSIT && destination != null;
        if (!scheduled || jem$waitingSchedule != null && (jem$waitingSchedule != train.runtime.schedule
                || jem$waitingDestination != destination || !actor.equals(jem$waitingActor))) {
            jem$waitingSchedule = null;
            jem$waitingDestination = null;
            jem$waitingActor = null;
        }
        if (!scheduled) return;
        if (waitingForSignal != null && ticksWaitingForSignal > 0 && Math.abs(train.speed) < .0001D) {
            jem$waitingSchedule = train.runtime.schedule;
            jem$waitingDestination = destination;
            jem$waitingActor = actor;
        } else if (waitingForSignal == null && jem$waitingSchedule != null && Math.abs(train.speed) > .0001D) {
            CreateRunProofs.emit(level, actor, "create_signal_wait_resume");
            jem$waitingSchedule = null;
            jem$waitingDestination = null;
            jem$waitingActor = null;
        }
    }
}
