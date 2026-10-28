package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.create.CreateTrainRun;
import com.justenoughmods.achievementguide.integration.create.CreateWhistleRun;
import net.minecraft.nbt.CompoundTag;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.schedule.Schedule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.simibubi.create.content.trains.schedule.ScheduleRuntime", remap = false)
public abstract class CreateScheduleLifecycleQuestMixin {
    @Shadow public Train train;

    @Inject(method = "setSchedule", at = @At("HEAD"), remap = false)
    private void jem$replaced(Schedule schedule, boolean automatic, CallbackInfo callback) {
        ((CreateTrainRun)train).jem$scheduleActor(null);
        if (train instanceof CreateWhistleRun run) run.jem$whistleProof(new CompoundTag());
    }

    @Inject(method = "discardSchedule", at = @At("HEAD"), remap = false)
    private void jem$canceled(CallbackInfo callback) {
        ((CreateTrainRun)train).jem$scheduleActor(null);
        if (train instanceof CreateWhistleRun run) run.jem$whistleProof(new CompoundTag());
    }
}
