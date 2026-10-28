package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.create.CreateTrainRun;
import com.simibubi.create.content.trains.schedule.Schedule;
import com.simibubi.create.content.trains.schedule.ScheduleRuntime;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.FakePlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "com.simibubi.create.content.trains.schedule.ScheduleItem", remap = false)
public abstract class CreateScheduleAssignmentQuestMixin {
    @Redirect(method = "handScheduleTo", at = @At(value = "INVOKE",
            target = "Lcom/simibubi/create/content/trains/schedule/ScheduleRuntime;setSchedule(Lcom/simibubi/create/content/trains/schedule/Schedule;Z)V"), remap = false)
    private void jem$assigned(ScheduleRuntime runtime, Schedule schedule, boolean automatic, ItemStack stack,
                              Player player, LivingEntity conductor, InteractionHand hand) {
        runtime.setSchedule(schedule, automatic);
        if (!automatic && runtime.schedule == schedule && !schedule.entries.isEmpty()
                && player instanceof ServerPlayer actor && !(actor instanceof FakePlayer)
                && !actor.isSpectator() && !actor.getAbilities().instabuild) {
            ((CreateTrainRun)runtime.train).jem$scheduleActor(actor.getUUID());
        }
    }
}
