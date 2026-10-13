package com.siirio.jemserver.smp.shops;

import net.minecraft.world.item.ItemStack;

public interface ShopCoinAccess {
    boolean jemCreativeStock();
    ItemStack jemTakeCoinSale(ItemStack expected, int quantity);
    boolean jemRestoreCoinSale(ItemStack stack);
}
