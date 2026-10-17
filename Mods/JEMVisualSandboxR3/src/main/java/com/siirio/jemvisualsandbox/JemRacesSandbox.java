package com.siirio.jemvisualsandbox;

import com.siirio.jemvisualsandbox.workbench.SandboxOptionalIrons;
import com.siirio.jemvisualsandbox.workbench.SandboxRaceLoot;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/** Source-owned standalone entrypoint; no binary gallery implementation is retained. */
@Mod("jem_visual_sandbox")
public final class JemRacesSandbox {
    public JemRacesSandbox(){
        var bus=FMLJavaModLoadingContext.get().getModEventBus();
        SandboxRaceLoot.register(bus);SandboxOptionalIrons.register(bus);
        net.minecraftforge.fml.ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.COMMON,
                com.siirio.jemvisualsandbox.workbench.SandboxRaceRules.SPEC,"jem-races-common.toml");
    }
}
