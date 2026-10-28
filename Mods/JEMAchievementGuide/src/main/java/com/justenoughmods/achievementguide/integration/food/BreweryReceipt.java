package com.justenoughmods.achievementguide.integration.food;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Synchronous hand-off from the native kettle's actual draw to its native player insertion. */
public interface BreweryReceipt {
    void jem$clearDraw();
    void jem$received(ServerPlayer player, ItemStack drawn, boolean inserted);
}
