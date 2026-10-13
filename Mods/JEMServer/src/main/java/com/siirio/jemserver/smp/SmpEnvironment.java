package com.siirio.jemserver.smp;

import com.siirio.jemidentity.api.DedicatedSmp;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class SmpEnvironment {
    public static boolean active(MinecraftServer server) {
        return DedicatedSmp.active(server);
    }

    public static boolean active(ServerPlayer player) {
        return DedicatedSmp.active(player);
    }

    private SmpEnvironment() {}
}
