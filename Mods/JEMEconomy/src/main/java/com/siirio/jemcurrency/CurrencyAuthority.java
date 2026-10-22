package com.siirio.jemcurrency;

import com.siirio.jemeconomy.api.EconomyApi;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** Every currency operation is authorized by the logical world's feature setting. */
public final class CurrencyAuthority {
    private CurrencyAuthority() { }
    public static boolean active(MinecraftServer server) { return EconomyApi.isEnabled(server); }
    public static boolean active(ServerPlayer player) { return player != null && active(player.getServer()); }
    public static boolean merchantActive(ServerPlayer player) { return active(player); }
}
