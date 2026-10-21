package com.siirio.jemguisandbox.mirror.jemserver.client.smp;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.screens.Screen;
public final class ServerCurrencyPresentation {
    public static void draw(GuiGraphics g, Font f, long n, int x, int y, int w, int color, boolean coins) {
        com.siirio.jemguisandbox.mirror.jemsettlements.client.SettlementPriceRenderer.draw(g,f,n,x,y,w,color,coins);
    }
    public static void openMarket(Screen parent) { com.siirio.jemguisandbox.Demo.market(parent,"","blocks",0); }
}
