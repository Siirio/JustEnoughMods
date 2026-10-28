package com.siirio.jemseasonshud;

import com.siirio.jemseasonshud.config.HudConfig;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.common.Mod;

@Mod(JEMSeasonsHud.MOD_ID)
public final class JEMSeasonsHud {
    public static final String MOD_ID = "jem_seasons_hud";

    public JEMSeasonsHud() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, HudConfig.SPEC);
    }
}
