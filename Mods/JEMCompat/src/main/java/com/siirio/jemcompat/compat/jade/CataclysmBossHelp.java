package com.siirio.jemcompat.compat.jade;

import com.github.L_Ender.cataclysm.config.CMCommonConfig;
import com.github.L_Ender.cataclysm.entity.AnimationMonster.BossMonsters.Ignis_Entity;
import com.github.L_Ender.cataclysm.entity.AnimationMonster.BossMonsters.The_Harbinger_Entity;
import com.github.L_Ender.cataclysm.entity.AnimationMonster.BossMonsters.The_Leviathan.The_Leviathan_Entity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.ForgeMod;

/** Translates the installed native state on the logical server; it never admits or changes damage. */
final class CataclysmBossHelp {
    private CataclysmBossHelp() { }

    static void describe(LivingEntity entity, CompoundTag state) {
        if (entity instanceof Ignis_Entity ignis && ignis.getIsShield() && !ignis.getIsShieldBreak()) {
            state.putString("defense", "ignis_shield");
        } else if (entity instanceof The_Harbinger_Entity harbinger) {
            if (!harbinger.getIsAct()) state.putString("defense", "harbinger_star");
            else if (harbinger.deactivateProgress > 0) state.putString("defense", "harbinger_waking");
            else if (((net.minecraft.world.entity.PowerableMob) harbinger).isPowered()) state.putString("defense", "harbinger_arrows");
        } else if (entity instanceof The_Leviathan_Entity leviathan && CMCommonConfig.Leviathan.ImmuneOutofWater
                && leviathan.getEyeInFluidType() != ForgeMod.WATER_TYPE.get()) {
            state.putString("defense", "leviathan_water");
        }
    }
}
