package com.siirio.jemcompat.mixin.transport;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(targets = "net.favouriteless.trotting_wagons.common.entities.wagons.RoyalWagon", remap = false)
public abstract class RoyalWagonInventoryMixin {
    private static final int ROYAL_WAGON_INVENTORY_SIZE = 18;

    @ModifyConstant(method = "<init>", constant = @Constant(intValue = 54))
    private static int jemcompat$limitInventory(int original) {
        return ROYAL_WAGON_INVENTORY_SIZE;
    }
}
