package com.siirio.jemlightingsandbox;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;

@Mod(JemLightingSandbox.ID)
public final class JemLightingSandbox {
    public static final String ID = "jem_lighting_sandbox";

    public JemLightingSandbox() {
        LightingSettings.register();
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> LightingOptions::register);
    }
}
