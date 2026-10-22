package com.siirio.jemserver.compat.economy;

import com.siirio.jemeconomy.api.EconomyApi;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;

/** Optional currency bridge returns only bridge-owned statuses. */
public final class CoinPayments {
    public enum Status { RESERVED, COMMITTED, REFUNDED, REPLAYED, INSUFFICIENT_FUNDS, UNAUTHENTICATED, UNAVAILABLE }
    private CoinPayments() { }
    public static Status reserve(ServerPlayer player, long amount, UUID request) { return Status.valueOf(EconomyApi.reserveMarketPayment(player, amount, request).status().name()); }
    public static void refund(ServerPlayer player, long amount, UUID request) { EconomyApi.refundMarketPayment(player, amount, request); }
    public static Status settle(ServerPlayer player, UUID owner, long amount, UUID request) { return Status.valueOf(EconomyApi.settleMarketPayment(player, owner, amount, request).status().name()); }
}
