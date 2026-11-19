package com.siirio.jemcompat.feature.transport;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

public final class ShulkerCapacity {
    public static final int BLOCK_STACK_LIMIT = 128;
    public static final String WIDE_COUNT_TAG = "JemShulkerWideCount";

    private ShulkerCapacity() {
    }

    public static boolean isEligibleBlockStack(ItemStack stack) {
        return stack.getItem() instanceof BlockItem && stack.getItem().getMaxStackSize(stack) == 64;
    }

    public static int limit(ItemStack stack) {
        return isEligibleBlockStack(stack) ? BLOCK_STACK_LIMIT : stack.getMaxStackSize();
    }
}
