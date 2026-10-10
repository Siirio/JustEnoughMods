package com.siirio.jemcompat.client.jei;

import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.function.Predicate;

final class CuriosEquipment implements Predicate<ItemStack> {
    @Override
    public boolean test(ItemStack stack) {
        return !CuriosApi.getItemStackSlots(stack, true).isEmpty();
    }
}
