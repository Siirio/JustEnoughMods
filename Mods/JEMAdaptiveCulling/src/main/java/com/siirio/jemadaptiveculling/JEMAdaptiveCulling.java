package com.siirio.jemadaptiveculling;

import com.siirio.jemadaptiveculling.config.JEMConfig;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.common.Mod;

@Mod(JEMAdaptiveCulling.MOD_ID)
public final class JEMAdaptiveCulling {
    public static final String MOD_ID = "jem_adaptive_culling";

    public JEMAdaptiveCulling() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, JEMConfig.SPEC);
    }
}
