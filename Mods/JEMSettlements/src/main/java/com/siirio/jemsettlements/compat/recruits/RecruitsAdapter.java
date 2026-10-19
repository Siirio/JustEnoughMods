package com.siirio.jemsettlements.compat.recruits;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import java.util.UUID;
import net.minecraft.world.entity.Entity;

public final class RecruitsAdapter {
    private RecruitsAdapter() {
    }
    public static void syncNeeds(Entity entity, com.siirio.jemsettlements.resident.ResidentMood history) {
        if (entity instanceof AbstractRecruitEntity recruit) history.nativeNeeds(recruit.getMorale(), recruit.getHunger());
    }

    public static boolean ownedBy(Entity entity, UUID accountId) {
        return entity instanceof AbstractRecruitEntity recruit
                && recruit.getOwnerUUID() != null
                && recruit.getOwnerUUID().equals(accountId);
    }
}
