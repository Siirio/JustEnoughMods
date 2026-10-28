package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.food.FoodOutputProof;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Native Mincer/Bowl inherit these setters. Replacement cannot masquerade as a produced output. */
@Mixin(RandomizableContainerBlockEntity.class)
public abstract class FoodRandomizableOutputMixin {
    @Inject(method = "setItem", at = @At("HEAD"))
    private void jem$replaced(int slot, ItemStack stack, CallbackInfo callback) { FoodOutputProof.replaced((BlockEntity)(Object)this, slot); }
    @Inject(method = "clearContent", at = @At("HEAD"))
    private void jem$cleared(CallbackInfo callback) {
        BlockEntity owner = (BlockEntity)(Object)this;
        FoodOutputProof.replaced(owner, owner.getPersistentData().getCompound(FoodOutputProof.KEY).getInt("slot"));
    }
}
