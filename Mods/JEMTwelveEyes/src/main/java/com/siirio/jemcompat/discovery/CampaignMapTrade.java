package com.siirio.jemcompat.discovery;

import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;

public final class CampaignMapTrade implements VillagerTrades.ItemListing {
    private final CampaignTarget target;
    private final int emeraldCost;

    public CampaignMapTrade(CampaignTarget target, int emeraldCost) {
        this.target = target;
        this.emeraldCost = emeraldCost;
    }

    @Override
    public MerchantOffer getOffer(Entity trader, RandomSource random) {
        return new MerchantOffer(
                new ItemStack(Items.EMERALD, emeraldCost),
                new ItemStack(Items.COMPASS),
                new ItemStack(CampaignItems.locator(target).get()),
                4,
                15,
                0.2F
        );
    }
}
