package com.siirio.jemsettlements.compat.recruits;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import java.util.Optional;
import net.minecraft.world.entity.Entity;

public final class RecruitReadinessAdapter {
    public record Readiness(float morale, float hunger, boolean combatReady) {
    }

    private RecruitReadinessAdapter() {
    }

    public static Optional<Readiness> read(Entity entity) {
        if (!(entity instanceof AbstractRecruitEntity recruit)) return Optional.empty();
        float morale = recruit.getMorale();
        float hunger = recruit.getHunger();
        return Optional.of(new Readiness(morale, hunger, recruit.isAlive() && morale > 0.0F && hunger > 0.0F));
    }

    /** Recruits 1.15.2 stores morale directly on a verified 0..100 scale. */
    public static void applyPayrollPenalty(AbstractRecruitEntity recruit, float points) {
        if (recruit == null || points <= 0.0F) return;
        recruit.setMoral(Math.max(0.0F, recruit.getMorale() - points));
    }
}
