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
}
