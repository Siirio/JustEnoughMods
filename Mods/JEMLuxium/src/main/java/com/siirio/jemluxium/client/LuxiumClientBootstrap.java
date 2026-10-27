package com.siirio.jemluxium.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import org.embeddedt.embeddium.api.OptionGUIConstructionEvent;

@OnlyIn(Dist.CLIENT)
public final class LuxiumClientBootstrap {
    private LuxiumClientBootstrap() {
    }

    public static void register() {
        OptionGUIConstructionEvent.BUS.addListener(event -> event.addPage(new LuxiumOptionPage()));
        MinecraftForge.EVENT_BUS.addListener(LuxiumClientBootstrap::onClientTick);
    }

    private static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            LuxiumShaderController.reconcileOculusState();
        }
    }
}
