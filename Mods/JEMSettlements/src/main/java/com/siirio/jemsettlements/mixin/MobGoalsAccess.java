package com.siirio.jemsettlements.mixin;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Resolve the inherited vanilla field against its mapped declaring class. */
@Mixin(Mob.class)
public interface MobGoalsAccess {
    @Accessor("goalSelector")
    GoalSelector jem$getGoalSelector();
}
