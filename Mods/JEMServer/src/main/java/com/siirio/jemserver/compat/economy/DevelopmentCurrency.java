package com.siirio.jemserver.compat.economy;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** Existing bound development entitlement only, behind the world currency feature gate. */
public final class DevelopmentCurrency {
    private DevelopmentCurrency() { }
    public static boolean frozen(MinecraftServer server) { return com.siirio.jemeconomy.ledger.EconomySavedData.get(server).frozen(); }
    public static boolean grant(ServerPlayer player) { return com.siirio.jemeconomy.ledger.EconomySavedData.get(player.server).ensureTestGrant(player); }
}
