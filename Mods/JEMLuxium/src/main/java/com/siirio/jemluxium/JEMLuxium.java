package com.siirio.jemluxium;

import com.siirio.jemluxium.client.LuxiumClientBootstrap;
import com.siirio.jemluxium.config.LuxiumClientConfig;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;

@Mod(JEMLuxium.MOD_ID)
public final class JEMLuxium {
    public static final String MOD_ID = "jem_luxium";

    public JEMLuxium() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, LuxiumClientConfig.SPEC);
        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> LuxiumClientBootstrap::register);
    }
}
