package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.food.FoodOutputProof;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.satisfy.bakery.core.block.entity.SmallCookingPotBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = SmallCookingPotBlockEntity.class, remap = false)
public abstract class SmallPotFoodQuestMixin {
    @Unique private FoodOutputProof.Before jem$before;
    @Inject(method = "craft", at = @At("HEAD"))
    private void jem$begin(Recipe<?> recipe, RegistryAccess registries, CallbackInfo callback) {
        jem$before = FoodOutputProof.before((BlockEntity)(Object)this, 7);
    }
    @Inject(method = "craft", at = @At("RETURN"))
    private void jem$committed(Recipe<?> recipe, RegistryAccess registries, CallbackInfo callback) {
        if (recipe != null && jem$before != null && !((Container)(Object)this).getItem(7).isEmpty())
            FoodOutputProof.completed((BlockEntity)(Object)this, 7, recipe.getId().toString(), "food_small_pot_meal", jem$before);
        jem$before = null;
    }
    @Inject(method = "m_183515_", at = @At("TAIL"))
    private void jem$save(CompoundTag tag, CallbackInfo callback) { FoodOutputProof.save((BlockEntity)(Object)this, tag); }
    @Inject(method = "m_142466_", at = @At("TAIL"))
    private void jem$load(CompoundTag tag, CallbackInfo callback) { FoodOutputProof.load((BlockEntity)(Object)this, tag); }
}
