package com.siirio.jemeconomy.api;

import com.siirio.jemeconomy.policy.ItemPolicy;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public record MarketItemSnapshot(ResourceLocation id, ItemStack stack, long buyCopper, long sellCopper,
                                 ItemPolicy.MarketCategory category, int accountSellRemaining,
                                 int globalSellRemaining, boolean buyAvailable, boolean sellAvailable) {
    public MarketItemSnapshot { stack = stack.copy(); }
    @Override public ItemStack stack() { return stack.copy(); }
}
