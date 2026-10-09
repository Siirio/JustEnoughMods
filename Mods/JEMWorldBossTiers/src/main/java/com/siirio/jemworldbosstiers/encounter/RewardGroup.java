package com.siirio.jemworldbosstiers.encounter;

import net.minecraft.world.item.ItemStack;

import java.util.List;

public record RewardGroup(String titleKey, List<ItemStack> items) {
    public RewardGroup {
        items = items.stream().map(ItemStack::copy).toList();
    }
}
