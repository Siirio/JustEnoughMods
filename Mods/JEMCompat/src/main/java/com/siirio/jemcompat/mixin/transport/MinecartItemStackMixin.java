package com.siirio.jemcompat.mixin.transport;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Item.class)
public abstract class MinecartItemStackMixin {
    public int getMaxStackSize(ItemStack stack) {
        return (Object) this == Items.MINECART ? 16 : ((Item) (Object) this).getMaxStackSize();
    }
}
