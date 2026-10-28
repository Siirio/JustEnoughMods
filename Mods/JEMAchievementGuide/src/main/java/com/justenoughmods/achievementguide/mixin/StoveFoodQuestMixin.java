package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.food.FoodOutputProof;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.satisfy.farm_and_charm.core.block.entity.StoveBlockEntity;
import net.satisfy.farm_and_charm.core.recipe.StoveRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Installed Farm & Charm 1.0.14: craft consumes inputs and commits output in slot zero. */
@Mixin(value = StoveBlockEntity.class, remap = false)
public abstract class StoveFoodQuestMixin {
    @Unique private FoodOutputProof.Before jem$before;
    @Inject(method = "craft", at = @At("HEAD"))
    private void jem$beforeRecipe(StoveRecipe recipe, RegistryAccess registries, CallbackInfo callback) {
        jem$before = FoodOutputProof.before((BlockEntity)(Object)this, 0);
    }
    @Inject(method = "craft", at = @At("RETURN"))
    private void jem$completedRecipe(StoveRecipe recipe, RegistryAccess registries, CallbackInfo callback) {
        if (recipe != null && jem$before != null) FoodOutputProof.stoveCompleted((BlockEntity)(Object)this, recipe.getId().toString(), jem$before);
        jem$before = null;
    }
    @Inject(method = "m_6836_", at = @At("HEAD"))
    private void jem$replaceOutput(int slot, ItemStack stack, CallbackInfo callback) { FoodOutputProof.replaced((BlockEntity)(Object)this, slot); }
    @Inject(method = "m_183515_", at = @At("TAIL"))
    private void jem$saveProvenance(CompoundTag tag, CallbackInfo callback) { FoodOutputProof.save((BlockEntity)(Object)this, tag); }
    @Inject(method = "m_142466_", at = @At("TAIL"))
    private void jem$loadProvenance(CompoundTag tag, CallbackInfo callback) { FoodOutputProof.load((BlockEntity)(Object)this, tag); }
}
