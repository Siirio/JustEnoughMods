package com.siirio.jemcompat.gate;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public final class RegistryIds {
    private RegistryIds() {
    }

    public static ResourceLocation entity(Entity entity) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
    }

    public static ResourceLocation item(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem());
    }

    public static ResourceLocation block(BlockState state) {
        return BuiltInRegistries.BLOCK.getKey(state.getBlock());
    }
}
