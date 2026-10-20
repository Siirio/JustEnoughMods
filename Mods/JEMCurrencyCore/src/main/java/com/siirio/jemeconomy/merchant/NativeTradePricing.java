package com.siirio.jemeconomy.merchant;

import com.siirio.jemeconomy.api.CoinTradePriceEvent;
import com.siirio.jemeconomy.api.EconomyApi;
import com.siirio.jemeconomy.ledger.EconomySavedData;
import com.siirio.jemeconomy.ledger.SystemAccount;
import com.siirio.jemeconomy.market.MarketSavedData;
import com.siirio.jemeconomy.policy.ItemPolicyRegistry;
import com.siirio.jemidentity.api.IdentityPolicy;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraftforge.common.MinecraftForge;
import static com.siirio.jemeconomy.merchant.NativeTradeQuote.Direction.*;

public final class NativeTradePricing {
    private NativeTradePricing() {}
    public static boolean monetary(MerchantOffer offer) {
        return offer.getCostA().is(Items.EMERALD) || offer.getCostB().is(Items.EMERALD) || offer.getResult().is(Items.EMERALD);
    }
    public static ItemStack input(MerchantOffer offer, int slot) {
        ItemStack stack = slot == 0 ? offer.getCostA() : offer.getCostB();
        return stack.is(Items.EMERALD) ? ItemStack.EMPTY : stack;
    }
    public static NativeTradeQuote quote(ServerPlayer player, MerchantOffer offer, int index, boolean buyer) {
        if (com.siirio.jemeconomy.policy.TradeRestrictions.offerProhibited(offer)) return row(index, UNPRICED, 0, false, "prohibited");
        if (buyer && com.siirio.jemeconomy.policy.TradeRestrictions.saleProhibited(offer.getCostA())) return row(index, UNPRICED, 0, false, "prohibited");
        if (IdentityPolicy.resolve(player).isEmpty()) return row(index, UNPRICED, 0, false, "identity");
        if (!monetary(offer)) return row(index, ITEM_BARTER, 0, !offer.isOutOfStock(), offer.isOutOfStock() ? "stock" : "ok");
        try {
            if (offer.getResult().is(Items.EMERALD) && !offer.getCostA().is(Items.EMERALD) && !offer.getCostB().is(Items.EMERALD)) {
                var policy = (buyer ? ItemPolicyRegistry.policy(offer.getCostA()) : NativeItemPricing.policy(offer.getCostA())).filter(p -> p.sellable());
                if (policy.isEmpty()) return row(index, UNPRICED, 0, false, "unpriced");
                var market = MarketSavedData.get(player.getServer());
                var demand = market.demand(player.getServer());
                var p = policy.get();
                var raw = NativeItemPricing.salePrice(offer.getCostA(),offer.getCostA().getCount(),market.multiplier(player.getServer(),p.demandGroup()));
                var saleQuote = EconomySavedData.get(player.getServer()).quoteSale(IdentityPolicy.resolve(player).orElseThrow().accountId(),raw.numerator(),raw.denominator());
                long copper = saleQuote.payout();
                if (buyer && !p.disc() && !p.demandGroup().equals(demand.primary()) && !p.demandGroup().equals(demand.secondary()))
                    return row(index, PLAYER_SELLS, copper, false, "demand");
                var principal = IdentityPolicy.resolve(player).orElseThrow();
                var allowance = market.allowanceExactTrade(player.getServer(), principal.accountId(), p, offer.getCostA().getCount(), copper);
                String reason = offer.isOutOfStock() ? "stock" : EconomySavedData.get(player.getServer()).frozen() ? "unavailable"
                        : allowance.units() != offer.getCostA().getCount() ? "limit"
                        : EconomySavedData.get(player.getServer()).balance(SystemAccount.NPC_MARKET.id()) < saleQuote.fundingRequired() ? "reserve" : "ok";
                return row(index, PLAYER_SELLS, copper, reason.equals("ok"), reason);
            }
            if (!offer.getResult().is(Items.EMERALD)) {
                var policy = NativeItemPricing.policy(offer.getResult()).filter(p -> p.buyable());
                if (policy.isEmpty()) return row(index, UNPRICED, 0, false, "unpriced");
                var exactBase = new com.siirio.jemeconomy.policy.RationalPrice(policy.get().buyCopper(),policy.get().buyDenominator()).multiply(offer.getResult().getCount());
                long base = exactBase.ceil();
                // Preserve the existing JEM native-demand adjustment, excluding nonmoney inputs.
                int original = (offer.getBaseCostA().is(Items.EMERALD) ? offer.getBaseCostA().getCount() : 0)
                        + (offer.getCostB().is(Items.EMERALD) ? offer.getCostB().getCount() : 0);
                int current = (offer.getCostA().is(Items.EMERALD) ? offer.getCostA().getCount() : 0)
                        + (offer.getCostB().is(Items.EMERALD) ? offer.getCostB().getCount() : 0);
                if (original <= 0 || current <= 0) return row(index, UNPRICED, 0, false, "unpriced");
                var adjusted = exactBase.multiply(current).divide(original);
                // Bound demand, reputation and racial discounts by the same safe merchandise floor.
                long floor = NativePriceMath.minimumPurchaseBudget(base,
                        NativeItemPricing.salePrice(offer.getResult(), offer.getResult().getCount(), 150).ceil());
                var event = new CoinTradePriceEvent(player, adjusted);
                MinecraftForge.EVENT_BUS.post(event);
                long copper = NativePriceMath.afterDiscount(floor, event.price().ceil());
                long funds = Math.addExact(EconomyApi.balance(player), EconomyApi.heldCash(player).value());
                String reason = offer.isOutOfStock() ? "stock" : EconomySavedData.get(player.getServer()).frozen() ? "unavailable"
                        : funds < copper ? "funds" : "ok";
                return row(index, PLAYER_BUYS, copper, reason.equals("ok"), reason);
            }
        } catch (ArithmeticException overflow) {
            return row(index, UNPRICED, 0, false, "unavailable");
        }
        return row(index, UNPRICED, 0, false, "unpriced");
    }
    private static NativeTradeQuote row(int index, NativeTradeQuote.Direction direction, long copper, boolean available, String reason) {
        return new NativeTradeQuote(index, direction, copper, available, "jem.economy.trade." + reason);
    }
}
