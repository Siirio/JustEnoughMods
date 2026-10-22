package com.siirio.jemeconomy.merchant;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;

public interface NativeTradeContainer {
    void jem$bind(NativeTradeSession session);
    void jem$projection(MerchantOffer offer, ItemStack result, int xp);
    void jem$input(int slot, ItemStack stack);
}
