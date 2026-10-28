package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.food.FoodOutputProof;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.satisfy.herbalbrews.core.blocks.entity.TeaKettleBlockEntity;
import net.satisfy.herbalbrews.core.recipe.TeaKettleRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Both native kettle tiers use this same committed recipe and output owner. */
@Mixin(value = TeaKettleBlockEntity.class, remap = false)
public abstract class TeaFoodQuestMixin {
    @Unique private FoodOutputProof.Before jem$before;
    @Inject(method = "craft", at = @At("HEAD"))
    private void jem$begin(TeaKettleRecipe recipe, CallbackInfo callback) { jem$before = FoodOutputProof.before((BlockEntity)(Object)this, 0); }
    @Inject(method = "craft", at = @At("RETURN"))
    private void jem$committed(TeaKettleRecipe recipe, CallbackInfo callback) {
        if (recipe != null && jem$before != null && recipe.getId().toString().equals("herbalbrews:kettle_brewing/green_tea")
                && FoodOutputProof.item(((Container)(Object)this).getItem(0), "herbalbrews:green_tea"))
            FoodOutputProof.completed((BlockEntity)(Object)this, 0, recipe.getId().toString(), "food/tea_brewed", jem$before);
        jem$before = null;
    }
    @Inject(method = "m_183515_", at = @At("TAIL"))
    private void jem$save(CompoundTag tag, CallbackInfo callback) { FoodOutputProof.save((BlockEntity)(Object)this, tag); }
    @Inject(method = "m_142466_", at = @At("TAIL"))
    private void jem$load(CompoundTag tag, CallbackInfo callback) { FoodOutputProof.load((BlockEntity)(Object)this, tag); }
}
