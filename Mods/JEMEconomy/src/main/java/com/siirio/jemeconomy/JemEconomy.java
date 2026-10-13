package com.siirio.jemeconomy;

import com.siirio.jemeconomy.command.CoinCommands;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;

@Mod(JemEconomy.MOD_ID)
public final class JemEconomy {
    public static final String MOD_ID = "jem_economy";

    public JemEconomy() {
        MinecraftForge.EVENT_BUS.register(CoinCommands.class);
    }
}
