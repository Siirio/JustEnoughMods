package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.food.FoodOutputProof;
import com.justenoughmods.achievementguide.integration.food.FoodRecipeRun;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.satisfy.vinery.core.block.entity.ApplePressBlockEntity;
import net.satisfy.vinery.core.recipe.ApplePressFermentingRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The native press has its own mashing/juice-processing route, independent of the wine barrel. */
@Mixin(value = ApplePressBlockEntity.class, remap = false)
public abstract class ApplePressFoodQuestMixin implements FoodRecipeRun {
    @Unique private FoodOutputProof.Before jem$before;
    @Inject(method = "processFermenting", at = @At("HEAD"))
    private static void jem$begin(ApplePressBlockEntity owner, ApplePressFermentingRecipe recipe, CallbackInfo callback) {
        ((FoodRecipeRun)owner).jem$beginOutput(3);
    }
    @Inject(method = "processFermenting", at = @At("RETURN"))
    private static void jem$committed(ApplePressBlockEntity owner, ApplePressFermentingRecipe recipe, CallbackInfo callback) {
        String action = recipe.getId().toString().equals("vinery:apple_fermenting/apple_juice")
                && FoodOutputProof.item(owner.getItem(3), "vinery:apple_juice") ? "food/apple_pressed" : "";
        ((FoodRecipeRun)owner).jem$finishOutput(3, recipe.getId().toString(), action);
    }
    @Override public void jem$beginOutput(int slot) { jem$before = FoodOutputProof.before((BlockEntity)(Object)this, slot); }
    @Override public void jem$finishOutput(int slot, String recipe, String action) {
        if (jem$before != null) FoodOutputProof.completed((BlockEntity)(Object)this, slot, recipe, action, jem$before);
        jem$before = null;
    }
    @Inject(method = "m_183515_", at = @At("TAIL"))
    private void jem$save(CompoundTag tag, CallbackInfo callback) { FoodOutputProof.save((BlockEntity)(Object)this, tag); }
    @Inject(method = "m_142466_", at = @At("TAIL"))
    private void jem$load(CompoundTag tag, CallbackInfo callback) { FoodOutputProof.load((BlockEntity)(Object)this, tag); }
}
