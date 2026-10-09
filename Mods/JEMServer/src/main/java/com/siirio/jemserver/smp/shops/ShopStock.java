package com.siirio.jemserver.smp.shops;

import net.minecraft.world.item.ItemStack;

import java.util.List;

public interface ShopStock {
    boolean jemOutOfStock();

    boolean jemPaymentFull();

    List<ItemStack> jemChoices();

    int jemAvailableTrades(ItemStack output, int quantity);
}
