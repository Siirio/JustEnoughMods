package com.siirio.jemsettlements;

import com.siirio.jemsettlements.command.SettlementCommands;
import com.siirio.jemsettlements.network.SettlementNetwork;
import com.siirio.jemsettlements.runtime.SettlementRuntime;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;

@Mod(JemSettlements.MOD_ID)
public final class JemSettlements {
    public static final String MOD_ID = "jem_settlements";

    public JemSettlements() {
        net.minecraftforge.fml.ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.SERVER, com.siirio.jemsettlements.payroll.SettlementEconomyConfig.SPEC);
        net.minecraftforge.fml.ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.SERVER, com.siirio.jemsettlements.happiness.SettlementMetricsConfig.SPEC, "jem-settlements-metrics.toml");
        SettlementNetwork.register();
        if (!net.minecraftforge.fml.loading.FMLLoader.getDist().isDedicatedServer()) return;
        MinecraftForge.EVENT_BUS.register(SettlementCommands.class);
        MinecraftForge.EVENT_BUS.register(SettlementNetwork.class);
        MinecraftForge.EVENT_BUS.register(com.siirio.jemsettlements.network.SettlementActions.class);
        MinecraftForge.EVENT_BUS.register(com.siirio.jemsettlements.network.SettlementWarnings.class);
        MinecraftForge.EVENT_BUS.register(SettlementRuntime.class);
        if (net.minecraftforge.fml.ModList.get().isLoaded("recruits")) MinecraftForge.EVENT_BUS.register(com.siirio.jemsettlements.compat.recruits.RecruitRuntime.class);
    }
}
