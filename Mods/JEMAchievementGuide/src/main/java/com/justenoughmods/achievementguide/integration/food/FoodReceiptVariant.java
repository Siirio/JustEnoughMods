package com.justenoughmods.achievementguide.integration.food;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Exact native on-take presentation can prepare a comparison copy without changing the delivered stack. */
public interface FoodReceiptVariant {
    ItemStack jem$nativeReceiptVariant(ItemStack output, ServerPlayer player);
}
