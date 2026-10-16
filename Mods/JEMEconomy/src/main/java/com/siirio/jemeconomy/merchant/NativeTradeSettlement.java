package com.siirio.jemeconomy.merchant;

import com.siirio.jemeconomy.api.EconomyApi;
import com.siirio.jemeconomy.ledger.SystemAccount;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MerchantContainer;
import net.minecraft.world.item.trading.MerchantOffer;

/** Exact monetary commit; native item movement is owned by the calling session. */
public final class NativeTradeSettlement {
    private NativeTradeSettlement() {}
    public static String commit(ServerPlayer player, MerchantContainer inputs, MerchantOffer offer, NativeTradeQuote quote, int inputOrder, UUID operation) {
        if (quote.direction() == NativeTradeQuote.Direction.PLAYER_SELLS) {
            var result = EconomyApi.processNativeNpcSaleFromContainer(player, inputs, offer.getCostA(),
                    offer.getCostA().getCount(), quote.copper(), inputOrder == 0 ? 0 : 1, operation);
            return switch (result.status()) {
                case COMMITTED -> "";
                case LIMIT_REACHED -> "jem.economy.trade.limit";
                case MARKET_BUDGET_EXHAUSTED -> "jem.economy.trade.reserve";
                case UNAUTHENTICATED -> "jem.economy.trade.identity";
                default -> "jem.economy.trade.unavailable";
            };
        }
        var reserve = EconomyApi.reserveMarketPayment(player, quote.copper(), operation);
        if (reserve.status() != EconomyApi.MarketStatus.RESERVED)
            return reserve.status() == EconomyApi.MarketStatus.INSUFFICIENT_FUNDS ? "jem.economy.trade.funds" : "jem.economy.trade.unavailable";
        var settle = EconomyApi.settleMarketPayment(player, SystemAccount.NPC_MARKET.id(), quote.copper(), operation);
        if (settle.status() != EconomyApi.MarketStatus.COMMITTED) {
            EconomyApi.refundMarketPayment(player, quote.copper(), operation);
            return "jem.economy.trade.unavailable";
        }
        return "";
    }
}
