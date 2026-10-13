package com.siirio.jemeconomy;

import com.siirio.jemeconomy.command.CoinCommands;
import com.siirio.jemeconomy.registry.EconomyItems;
import com.siirio.jemeconomy.network.EconomyNetwork;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.common.Mod;

@Mod(JemEconomy.MOD_ID)
public final class JemEconomy {
    public static final String MOD_ID = "jem_economy";

    public JemEconomy() {
        EconomyItems.ITEMS.register(FMLJavaModLoadingContext.get().getModEventBus());
        EconomyNetwork.register();
        MinecraftForge.EVENT_BUS.register(CoinCommands.class);
        MinecraftForge.EVENT_BUS.register(EconomyNetwork.class);
    }
}
