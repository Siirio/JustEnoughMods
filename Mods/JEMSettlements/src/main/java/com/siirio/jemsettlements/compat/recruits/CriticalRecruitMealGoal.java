package com.siirio.jemsettlements.compat.recruits;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import java.util.EnumSet;
import net.minecraft.world.entity.ai.goal.Goal;

/** Native goal arbitration grants one short meal pause only when critical reachable food exists. */
public final class CriticalRecruitMealGoal extends Goal {
    private final AbstractRecruitEntity recruit;
    private long until;
    private long nextAttempt;
    public CriticalRecruitMealGoal(AbstractRecruitEntity recruit) {
        this.recruit = recruit; setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }
    @Override public boolean canUse() {
        return recruit.level().getGameTime() >= nextAttempt && recruit.getTarget() != null
                && RecruitNeedsService.criticalFoodAvailable(recruit);
    }
    @Override public void start() {
        long now = recruit.level().getGameTime(); nextAttempt = now + 100L;
        recruit.getNavigation().stop(); recruit.stopUsingItem();
        until = RecruitNeedsService.criticalMeal(recruit) ? now + 32L : now;
    }
    @Override public boolean canContinueToUse() { return recruit.isAlive() && recruit.level().getGameTime() < until && RecruitNeedsService.managed(recruit); }
    @Override public boolean requiresUpdateEveryTick() { return true; }
    @Override public void tick() { recruit.getNavigation().stop(); }
}
