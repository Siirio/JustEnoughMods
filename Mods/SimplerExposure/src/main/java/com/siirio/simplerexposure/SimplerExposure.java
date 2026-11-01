package com.siirio.simplerexposure;

import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import org.slf4j.Logger;

@Mod(SimplerExposure.MOD_ID)
public final class SimplerExposure {
    public static final String MOD_ID = "simpler_exposure";
    public static final Logger LOGGER = LogUtils.getLogger();

    public SimplerExposure() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SimplerExposureConfig.SPEC);
        MinecraftForge.EVENT_BUS.addListener(InstantPhotographs::onFrameAdded);
    }
}
