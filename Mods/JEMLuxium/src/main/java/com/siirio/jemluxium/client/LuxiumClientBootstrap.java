package com.siirio.jemluxium.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.embeddedt.embeddium.api.OptionGUIConstructionEvent;

@OnlyIn(Dist.CLIENT)
public final class LuxiumClientBootstrap {
    private LuxiumClientBootstrap() {
    }

    public static void register() {
        OptionGUIConstructionEvent.BUS.addListener(event -> event.addPage(new LuxiumOptionPage()));
    }
}
