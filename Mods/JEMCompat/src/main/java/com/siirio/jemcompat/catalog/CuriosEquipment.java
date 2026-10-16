package com.siirio.jemcompat.catalog;

import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.function.Predicate;

final class CuriosEquipment implements Predicate<ItemStack> {
    @Override
    public boolean test(ItemStack stack) {
        return !CuriosApi.getItemStackSlots(stack,
                net.minecraftforge.fml.loading.FMLEnvironment.dist == net.minecraftforge.api.distmarker.Dist.CLIENT).isEmpty();
    }
}
