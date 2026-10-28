package com.siirio.jemanimalhints;

import com.siirio.jemanimalhints.compat.jade.TamingDataLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod(JEMAnimalHints.MOD_ID)
public final class JEMAnimalHints {
    public static final String MOD_ID = "jem_animal_hints";

    public JEMAnimalHints() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void addReloadListeners(AddReloadListenerEvent event) {
        event.addListener(TamingDataLoader.instance());
    }
}
