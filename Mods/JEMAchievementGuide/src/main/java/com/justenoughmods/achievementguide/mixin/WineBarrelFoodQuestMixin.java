package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.food.FoodOutputProof;
import com.justenoughmods.achievementguide.integration.food.FoodReceiptVariant;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.satisfy.vinery.core.block.entity.FermentationBarrelBlockEntity;
import net.satisfy.vinery.core.recipe.FermentationBarrelRecipe;
import net.satisfy.vinery.core.util.WineYears;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = FermentationBarrelBlockEntity.class, remap = false)
public abstract class WineBarrelFoodQuestMixin implements FoodReceiptVariant {
    @Unique private FoodOutputProof.Before jem$before;
    @Inject(method = "craft", at = @At("HEAD"))
    private void jem$begin(FermentationBarrelRecipe recipe, RegistryAccess registries, CallbackInfo callback) { jem$before = FoodOutputProof.before((BlockEntity)(Object)this, 5); }
    @Inject(method = "craft", at = @At("RETURN"))
    private void jem$committed(FermentationBarrelRecipe recipe, RegistryAccess registries, CallbackInfo callback) {
        var owner = (FermentationBarrelBlockEntity)(Object)this;
        if (recipe != null && jem$before != null && recipe.getId().toString().equals("vinery:wine_fermentation/red_wine")
                && FoodOutputProof.item(owner.getItem(5), "vinery:red_wine"))
            FoodOutputProof.completed(owner, 5, recipe.getId().toString(), "food/wine_fermented", jem$before);
        jem$before = null;
    }
    @Override public ItemStack jem$nativeReceiptVariant(ItemStack output, ServerPlayer player) {
        ItemStack comparison = output.copy();
        WineYears.setWineYear(comparison, player.level());
        return comparison;
    }
    @Inject(method = "m_6836_", at = @At("HEAD"))
    private void jem$replaced(int slot, ItemStack stack, CallbackInfo callback) { FoodOutputProof.replaced((BlockEntity)(Object)this, slot); }
    @Inject(method = "m_6211_", at = @At("HEAD"))
    private void jem$cleared(CallbackInfo callback) { FoodOutputProof.replaced((BlockEntity)(Object)this, 5); }
    @Inject(method = "m_183515_", at = @At("TAIL"))
    private void jem$save(CompoundTag tag, CallbackInfo callback) { FoodOutputProof.save((BlockEntity)(Object)this, tag); }
    @Inject(method = "m_142466_", at = @At("TAIL"))
    private void jem$load(CompoundTag tag, CallbackInfo callback) { FoodOutputProof.load((BlockEntity)(Object)this, tag); }
}
