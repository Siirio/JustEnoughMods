package com.siirio.jemsecurity;

import com.siirio.jemsecurity.network.SecurityNetwork;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;

@Mod(JEMSecurity.MOD_ID)
public final class JEMSecurity {
    public static final String MOD_ID = "jem_security";

    public JEMSecurity() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, SecurityConfig.SPEC, "jem-security-server.toml");
        SecurityNetwork.register();
    }
}
