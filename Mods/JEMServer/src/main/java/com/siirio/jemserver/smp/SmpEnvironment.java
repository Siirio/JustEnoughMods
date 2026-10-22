package com.siirio.jemserver.smp;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class SmpEnvironment {
    public static boolean active(MinecraftServer server) {
        return server != null && server.isDedicatedServer() && server.overworld() != null;
    }

    public static boolean active(ServerPlayer player) {
        return player != null && active(player.getServer())
                && com.siirio.jemidentity.api.IdentityPolicy.resolve(player).isPresent();
    }

    private SmpEnvironment() {}
}
