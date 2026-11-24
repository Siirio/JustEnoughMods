package com.siirio.jematmosphere;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;

@Mod(JemAtmosphere.ID)
public final class JemAtmosphere {
    public static final String ID = "jem_atmosphere";

    public JemAtmosphere() {
        AtmosphereSettings.register();
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> AtmosphereOptions::register);
    }
}
