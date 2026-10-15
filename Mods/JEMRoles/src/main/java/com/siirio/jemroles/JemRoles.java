package com.siirio.jemroles;

import com.siirio.jemroles.network.RolesNetwork;
import com.siirio.jemroles.trait.RaceTraitRuntime;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(JemRoles.MOD_ID)
public final class JemRoles {
    public static final String MOD_ID = "jem_roles";

    public JemRoles() {
        com.siirio.jemroles.loot.RaceLoot.register(FMLJavaModLoadingContext.get().getModEventBus());
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER,
                com.siirio.jemroles.race.RaceRules.SPEC, "jem-races-server.toml");
        RolesNetwork.register();
        MinecraftForge.EVENT_BUS.register(RaceTraitRuntime.class);
        MinecraftForge.EVENT_BUS.register(RolesNetwork.class);
        if (ModList.get().isLoaded("jem_economy")) {
            MinecraftForge.EVENT_BUS.register(com.siirio.jemroles.compat.economy.CoinTradeRaceBridge.class);
        }
    }
}
