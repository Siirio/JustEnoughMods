package com.siirio.jemserver.client.smp;

import net.minecraft.client.Minecraft;

public final class ClaimsScreenBridge {
    private static SmpScreen parent;

    public static void capture(SmpScreen screen) {
        parent = screen;
    }

    public static boolean active() {
        return parent != null;
    }

    public static void clear() {
        parent = null;
    }

    public static void back() {
        var previous = parent;
        parent = null;
        var client = Minecraft.getInstance();
        if (client.player != null) client.player.closeContainer();
        if (previous != null) client.setScreen(previous);
        else SmpScreen.openRoute("events", null);
    }

    public static void open(String route) {
        if (route.equals("claims")) return;
        var previous = parent;
        parent = null;
        var client = Minecraft.getInstance();
        if (client.player != null) client.player.closeContainer();
        if (previous != null) client.setScreen(previous);
        SmpScreen.openRoute(route, null);
    }

    private ClaimsScreenBridge() {}
}
