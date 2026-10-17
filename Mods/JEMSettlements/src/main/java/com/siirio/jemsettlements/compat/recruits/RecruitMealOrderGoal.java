package com.siirio.jemsettlements.compat.recruits;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import java.util.EnumSet;
import net.minecraft.world.entity.ai.goal.Goal;

/** Reserve native movement for a meal, including spontaneous hunger and an explicit chest order during combat. */
public final class RecruitMealOrderGoal extends Goal {
    private final AbstractRecruitEntity recruit;
    private long nextCheck;
    private boolean wanted;
    public RecruitMealOrderGoal(AbstractRecruitEntity recruit) {
        this.recruit = recruit; setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }
    private boolean wantsMeal() {
        long now = recruit.level().getGameTime();
        if (now >= nextCheck) { nextCheck = now + 20L; wanted = RecruitNeedsService.hasMealTask(recruit); }
        return wanted;
    }
    @Override public boolean canUse() { return wantsMeal(); }
    @Override public boolean canContinueToUse() { return recruit.isAlive() && wantsMeal(); }
    @Override public boolean requiresUpdateEveryTick() { return true; }
    @Override public void start() { recruit.getNavigation().stop(); recruit.stopUsingItem(); RecruitNeedsService.meal(recruit); }
    @Override public void tick() { if (recruit.tickCount % 20 == 0) RecruitNeedsService.meal(recruit); }
    @Override public void stop() { recruit.getNavigation().stop(); wanted = false; }
}
