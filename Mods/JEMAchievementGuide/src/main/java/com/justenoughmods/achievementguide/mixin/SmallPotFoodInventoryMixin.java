package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.food.FoodOutputProof;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.satisfy.bakery.core.block.entity.SmallCookingPotBlockEntity;
import net.satisfy.farm_and_charm.core.world.ImplementedInventory;
import org.spongepowered.asm.mixin.Mixin;

/** Preserve the inherited native inventory owner and invalidate arbitrary output replacement. */
@Mixin(value = SmallCookingPotBlockEntity.class, remap = false)
public abstract class SmallPotFoodInventoryMixin implements ImplementedInventory {
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
