package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.worldritual.WorldRitualQuestStun;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import org.spongepowered.asm.mixin.*;
@Mixin(Mob.class)
public abstract class WorldRitualQuestMobGoalsMixin implements WorldRitualQuestStun.GoalAccess {
    @Shadow @Final protected GoalSelector goalSelector;
    @Override public GoalSelector jem$ritualGoals(){return goalSelector;}
}
