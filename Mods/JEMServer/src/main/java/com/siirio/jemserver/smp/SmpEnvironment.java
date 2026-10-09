package com.siirio.jemserver.smp;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class SmpEnvironment {
    public static boolean active(MinecraftServer server) {
        return server != null && server.isDedicatedServer();
    }

    public static boolean active(ServerPlayer player) {
        return active(player.server);
    }

    private SmpEnvironment() {}
}
