package com.siirio.jemeconomy.merchant;

import com.siirio.jemeconomy.market.MarketSavedData;
import com.siirio.jemeconomy.policy.ItemPolicyRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraftforge.registries.ForgeRegistries;

public final class BuyerTradeOffers {
    private BuyerTradeOffers() {}
    public static void refresh(AbstractVillager buyer, ServerPlayer player) {
        var demand = MarketSavedData.get(player.getServer()).demand(player.getServer());
        MerchantOffers offers = new MerchantOffers();
        ItemPolicyRegistry.reviewed().stream().filter(policy -> policy.sellable()
                && (policy.demandGroup().equals(demand.primary()) || policy.demandGroup().equals(demand.secondary())))
                .sorted(java.util.Comparator.comparing(policy -> policy.item().toString())).forEach(policy -> {
                    var item = ForgeRegistries.ITEMS.getValue(policy.item());
                    if (item != null) offers.add(new MerchantOffer(new ItemStack(item), new ItemStack(Items.EMERALD),
                            Integer.MAX_VALUE, 0, 0.0F));
                });
        // Emerald is only the native monetary-leg descriptor, never the delivered payout.
        buyer.overrideOffers(offers);
    }
}
