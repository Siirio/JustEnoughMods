package com.siirio.jemeconomy.client;

import com.siirio.jemeconomy.network.EconomyNetwork;
import com.siirio.jemeconomy.network.MarketCatalogPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

public final class MarketClientState {
    private static Screen returnTo;
    private MarketClientState() {}

    public static void open(Screen parent) {
        if (!EconomyNetwork.supported()) return;
        returnTo = parent;
        EconomyNetwork.marketQuery("", "all", 0, true);
    }

    public static void accept(MarketCatalogPacket packet) {
        if (!EconomyNetwork.supported()) return;
        var client = Minecraft.getInstance();
        if (client.screen instanceof MarketScreen screen) screen.accept(packet.snapshot());
        else if (packet.open()) client.setScreen(new MarketScreen(returnTo, packet.snapshot()));
    }

    public static void clear() { returnTo = null; }
}
