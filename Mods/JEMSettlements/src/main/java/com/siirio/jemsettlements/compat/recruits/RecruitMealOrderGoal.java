package com.siirio.jemsettlements.compat.recruits;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import java.util.EnumSet;
import net.minecraft.world.entity.ai.goal.Goal;

/** Idle recruits temporarily follow the selected-chest order through native goal arbitration. */
public final class RecruitMealOrderGoal extends Goal {
    private final AbstractRecruitEntity recruit;
    public RecruitMealOrderGoal(AbstractRecruitEntity recruit) {
        this.recruit = recruit; setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }
    @Override public boolean canUse() { return RecruitNeedsService.hasMealOrder(recruit); }
    @Override public boolean canContinueToUse() { return RecruitNeedsService.hasMealOrder(recruit); }
    @Override public boolean requiresUpdateEveryTick() { return true; }
    @Override public void start() { recruit.getNavigation().stop(); RecruitNeedsService.orderedMeal(recruit); }
    @Override public void tick() { if (recruit.tickCount % 20 == 0) RecruitNeedsService.orderedMeal(recruit); }
    @Override public void stop() { recruit.getNavigation().stop(); }
}
