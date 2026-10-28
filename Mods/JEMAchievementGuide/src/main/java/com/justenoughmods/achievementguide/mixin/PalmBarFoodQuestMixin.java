package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.food.ColdSuppliesOutputProof;
import com.justenoughmods.achievementguide.integration.food.FoodOutputProof;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.satisfy.beachparty.core.block.entity.PalmBarBlockEntity;
import net.satisfy.beachparty.core.recipe.PalmBarRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Exact Beachparty 2.0.3 native craft and concrete inventory mutation seams. */
@Mixin(value = PalmBarBlockEntity.class, remap = false)
public abstract class PalmBarFoodQuestMixin {
    @Unique private ColdSuppliesOutputProof.Pending jem$coldSupplies;

    @Inject(method = "craft(Lnet/satisfy/beachparty/core/recipe/PalmBarRecipe;Lnet/minecraft/core/RegistryAccess;)V", at = @At("HEAD"))
    private void jem$beforeRecipe(PalmBarRecipe recipe, RegistryAccess registries, CallbackInfo callback) {
        jem$coldSupplies = ColdSuppliesOutputProof.before((BlockEntity)(Object)this, recipe, registries, false);
    }

    @Inject(method = "craft(Lnet/satisfy/beachparty/core/recipe/PalmBarRecipe;Lnet/minecraft/core/RegistryAccess;)V", at = @At("RETURN"))
    private void jem$completedRecipe(PalmBarRecipe recipe, RegistryAccess registries, CallbackInfo callback) {
        ColdSuppliesOutputProof.Pending pending = jem$coldSupplies;
        jem$coldSupplies = null;
        ColdSuppliesOutputProof.completed((BlockEntity)(Object)this, pending);
    }

    @Inject(method = "m_6836_(ILnet/minecraft/world/item/ItemStack;)V", at = @At("HEAD"))
    private void jem$replaceOutput(int slot, ItemStack stack, CallbackInfo callback) {
        FoodOutputProof.replaced((BlockEntity)(Object)this, slot);
    }

    @Inject(method = "m_6211_()V", at = @At("HEAD"))
    private void jem$clearOutput(CallbackInfo callback) { FoodOutputProof.replaced((BlockEntity)(Object)this, 0); }

    @Inject(method = "m_183515_(Lnet/minecraft/nbt/CompoundTag;)V", at = @At("TAIL"))
    private void jem$saveProvenance(CompoundTag tag, CallbackInfo callback) { FoodOutputProof.save((BlockEntity)(Object)this, tag); }

    @Inject(method = "m_142466_(Lnet/minecraft/nbt/CompoundTag;)V", at = @At("TAIL"))
    private void jem$loadProvenance(CompoundTag tag, CallbackInfo callback) { FoodOutputProof.load((BlockEntity)(Object)this, tag); }
}
