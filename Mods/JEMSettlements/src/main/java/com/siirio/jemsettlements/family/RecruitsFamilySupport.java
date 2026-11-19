package com.siirio.jemsettlements.family;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

public final class RecruitsFamilySupport {
    private RecruitsFamilySupport() { }
    public static boolean isRecruit(LivingEntity entity) { return entity instanceof AbstractRecruitEntity; }
    public static boolean ready(LivingEntity entity) { return entity instanceof AbstractRecruitEntity recruit && recruit.getHunger() >= 60F && recruit.getMorale() >= 60F; }
    public static boolean inferTableOrigin(LivingEntity entity) { return entity instanceof AbstractRecruitEntity recruit && recruit.getIsOwned(); }
    public static void initializeSpawn(LivingEntity entity) { if (entity instanceof AbstractRecruitEntity recruit) { recruit.initSpawn(); recruit.setEquipment(); recruit.setHunger(80F); recruit.setMoral(70F); } }
}
