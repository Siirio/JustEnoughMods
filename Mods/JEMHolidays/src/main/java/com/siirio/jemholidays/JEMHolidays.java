package com.siirio.jemholidays;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;

@Mod(JEMHolidays.MOD_ID)
public final class JEMHolidays {
    public static final String MOD_ID = "jem_holidays";

    public JEMHolidays() {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> HalloweenMobPumpkins::register);
    }
}
