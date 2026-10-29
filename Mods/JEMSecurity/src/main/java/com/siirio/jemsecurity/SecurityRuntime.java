package com.siirio.jemsecurity;

import net.minecraft.server.MinecraftServer;

public final class SecurityRuntime {
    private SecurityRuntime() { }

    public static boolean active(MinecraftServer server) {
        return server != null && !server.isSingleplayer() && SecurityConfig.ENABLED.get();
    }
}
