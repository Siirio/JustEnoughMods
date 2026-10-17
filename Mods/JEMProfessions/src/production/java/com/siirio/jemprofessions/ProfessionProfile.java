package com.siirio.jemprofessions;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.network.simple.SimpleChannel;

/** Production entry points remain separate from the explicitly installed sandbox profile. */
public final class ProfessionProfile {
    private ProfessionProfile() {}
    public static String networkVersion() { return "4"; }
    public static void registerPackets(SimpleChannel channel) {}
    public static void initialize() {
        MinecraftForge.EVENT_BUS.register(com.siirio.jemprofessions.progress.ProfessionGameplay.class);
        if (ModList.get().isLoaded("irons_spellbooks"))
            MinecraftForge.EVENT_BUS.register(com.siirio.jemprofessions.compat.irons.IronsProfessionAdapter.class);
    }
}
