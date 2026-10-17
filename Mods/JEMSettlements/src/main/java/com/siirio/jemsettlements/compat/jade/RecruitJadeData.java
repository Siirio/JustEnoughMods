package com.siirio.jemsettlements.compat.jade;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.siirio.jemsettlements.town.SettlementSavedData;

/** Optional native types stay out of the common Jade provider's signatures. */
final class RecruitJadeData {
    private RecruitJadeData() { }
    static boolean append(CompoundTag tag, Entity entity) {
        if (!(entity instanceof AbstractRecruitEntity recruit)) return false;
        if (!com.siirio.jemsettlements.compat.recruits.RecruitNeedsService.managed(recruit)) return true;
        tag.putBoolean("JemResident", true);
        tag.putInt("JemHappiness", Math.max(0, Math.min(100, Math.round(recruit.getMorale()))));
        tag.putInt("JemSatiety", Math.max(0, Math.min(100, (int) Math.floor(recruit.getHunger()))));
        var data = SettlementSavedData.get(recruit.getServer());
        data.workforceEntity(recruit.getUUID()).ifPresent(record -> {
            data.town(record.townId()).ifPresent(town -> tag.putString("JemSettlementName", town.name()));
            if (!com.siirio.jemsettlements.compat.recruits.RecruitNeedsService.managed(recruit)) return;
            var history = record.foodHistory();
            tag.putBoolean("JemCraving", history.craving());
            history.requestedDish().ifPresent(value -> tag.putString("JemRequestedDish", value.toString()));
            history.repeatedDish().ifPresent(value -> tag.putString("JemRepeatedDish", value.toString()));
        });
        return true;
    }
}
