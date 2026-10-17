package com.siirio.jemeconomy.merchant;

import com.siirio.jemeconomy.api.EconomyApi;
import com.siirio.jemeconomy.ledger.SystemAccount;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MerchantContainer;
import net.minecraft.world.item.trading.MerchantOffer;

/** Coin settlement. Taxed sandbox purchases bind payment to Economy's durable output journal;
 * the native session retains offer selection, stock and merchant callbacks. */
public final class NativeTradeSettlement {
    private NativeTradeSettlement() {}
    public static String commit(ServerPlayer player, MerchantContainer inputs, MerchantOffer offer, NativeTradeQuote quote, int inputOrder, UUID operation) {
        return commit(player,inputs,offer,quote,inputOrder,operation,null);
    }
    public static String commit(ServerPlayer player, MerchantContainer inputs, MerchantOffer offer, NativeTradeQuote quote, int inputOrder, UUID operation, net.minecraft.world.entity.Entity merchant) {
        if (com.siirio.jemeconomy.policy.TradeRestrictions.offerProhibited(offer)) return "jem.economy.trade.prohibited";
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
        com.siirio.jemeconomy.sandbox.NativeTradePaymentEvent tax = null;
        if (com.siirio.jemeconomy.sandbox.SandboxCommerce.active(player.getServer())) {
            tax = new com.siirio.jemeconomy.sandbox.NativeTradePaymentEvent(player, merchant, operation, quote.copper());
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(tax);
        }
        boolean taxed=tax!=null&&tax.town()!=null&&tax.percent()>0&&(quote.copper()/100)*tax.percent()+((quote.copper()%100)*tax.percent())/100>0;
        if(taxed){
            String prepared=com.siirio.jemeconomy.sandbox.SandboxCommerce.prepareNativePurchase(player,operation,tax.town(),tax.percent(),quote.copper(),offer.getResult());
            if(!prepared.isEmpty())return prepared;
        }
        var reserve = EconomyApi.reserveMarketPayment(player, quote.copper(), operation);
        if (reserve.status() != EconomyApi.MarketStatus.RESERVED&&!(taxed&&reserve.status()==EconomyApi.MarketStatus.REPLAYED)){
            if(taxed)com.siirio.jemeconomy.sandbox.SandboxCommerce.abortNativePurchase(player,operation);
            return reserve.status() == EconomyApi.MarketStatus.INSUFFICIENT_FUNDS ? "jem.economy.trade.funds" : "jem.economy.trade.unavailable";
        }
        boolean settled;
        if (taxed) {
            var split = com.siirio.jemeconomy.sandbox.SandboxCommerce.settleResidentIncome(player,tax.town(),tax.percent(),quote.copper(),operation);
            settled = split.status() == com.siirio.jemeconomy.ledger.TransferResult.Status.COMMITTED||split.status()==com.siirio.jemeconomy.ledger.TransferResult.Status.REPLAYED;
        } else settled = EconomyApi.settleMarketPayment(player, SystemAccount.NPC_MARKET.id(), quote.copper(), operation).status() == EconomyApi.MarketStatus.COMMITTED;
        if (!settled) {
            var refund=EconomyApi.refundMarketPayment(player, quote.copper(), operation);
            if(taxed&&refund.status()!=EconomyApi.MarketStatus.REFUNDED&&refund.status()!=EconomyApi.MarketStatus.REPLAYED)
                throw new IllegalStateException("Native payment refund remains a pending journal obligation");
            if(taxed)com.siirio.jemeconomy.sandbox.SandboxCommerce.abortNativePurchase(player,operation);
            return "jem.economy.trade.unavailable";
        }
        if(taxed&&!com.siirio.jemeconomy.sandbox.SandboxCommerce.completeNativePurchase(player,operation))throw new IllegalStateException("Paid native output commitment cannot be completed");
        return "";
    }
}
