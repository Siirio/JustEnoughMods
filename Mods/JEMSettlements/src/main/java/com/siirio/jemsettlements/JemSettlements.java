package com.siirio.jemsettlements;

import com.siirio.jemsettlements.command.SettlementCommands;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;

@Mod(JemSettlements.MOD_ID)
public final class JemSettlements {
    public static final String MOD_ID = "jem_settlements";

    public JemSettlements() {
        MinecraftForge.EVENT_BUS.register(SettlementCommands.class);
    }
}
