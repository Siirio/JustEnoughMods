package com.siirio.jemprofessions;

import com.siirio.jemprofessions.progress.ProfessionGameplay;
import com.siirio.jemprofessions.network.ProfessionsNetwork;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.ModList;

@Mod(JemProfessions.MOD_ID)
public final class JemProfessions {
    public static final String MOD_ID = "jem_professions";

    public JemProfessions() {
        ProfessionsNetwork.register();
        MinecraftForge.EVENT_BUS.register(ProfessionGameplay.class);
        if (ModList.get().isLoaded("irons_spellbooks")) {
            MinecraftForge.EVENT_BUS.register(com.siirio.jemprofessions.compat.irons.IronsProfessionAdapter.class);
        }
    }
}
