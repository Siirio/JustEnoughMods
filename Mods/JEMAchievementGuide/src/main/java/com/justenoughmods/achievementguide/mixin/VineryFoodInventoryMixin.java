package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.food.FoodOutputProof;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.satisfy.vinery.core.block.entity.ApplePressBlockEntity;
import net.satisfy.vinery.core.util.ImplementedInventory;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = ApplePressBlockEntity.class, remap = false)
public abstract class VineryFoodInventoryMixin implements ImplementedInventory {
    @Override public void setItem(int slot, ItemStack stack) {
        FoodOutputProof.replaced((BlockEntity)(Object)this, slot);
        ImplementedInventory.super.m_6836_(slot, stack);
    }
    @Override public void clearContent() {
        BlockEntity owner = (BlockEntity)(Object)this;
        FoodOutputProof.replaced(owner, owner.getPersistentData().getCompound(FoodOutputProof.KEY).getInt("slot"));
        ImplementedInventory.super.m_6211_();
    }
}
