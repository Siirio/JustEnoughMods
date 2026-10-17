package com.siirio.jemcurrency;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.ModList;

/** Settlements is the explicit integrated-server exception; other SMP modules stay gated. */
public final class CurrencyAuthority {
    private CurrencyAuthority() {}

    public static boolean active(MinecraftServer server) {
        return server != null && (server.isDedicatedServer() || ModList.get().isLoaded("jem_settlements"));
    }

    public static boolean active(ServerPlayer player) {
        return player != null && active(player.getServer());
    }
}
