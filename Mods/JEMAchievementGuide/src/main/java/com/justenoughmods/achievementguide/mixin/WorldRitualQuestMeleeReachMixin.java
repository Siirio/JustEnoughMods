package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.worldritual.WorldRitualQuestStun;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(MeleeAttackGoal.class)
public abstract class WorldRitualQuestMeleeReachMixin implements WorldRitualQuestStun.ReachAccess {
    @Invoker("getAttackReachSqr") public abstract double jem$ritualReach(LivingEntity actor);
}
