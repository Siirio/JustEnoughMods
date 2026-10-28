package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.create.FactoryGridReceipt;
import com.justenoughmods.achievementguide.integration.create.FactoryRecipeProof;
import com.justenoughmods.achievementguide.integration.create.FactoryResultReceipt;
import com.simibubi.create.content.kinetics.crafter.RecipeGridHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = RecipeGridHandler.class, remap = false)
public abstract class FactoryRecipeResultQuestMixin {
    @Inject(method = "tryToApplyRecipe", at = @At("RETURN"))
    private static void jem$actualNativeFactoryRecipe(Level world, RecipeGridHandler.GroupedItems ingredients,
                                                      CallbackInfoReturnable<ItemStack> callback) {
        ItemStack result = callback.getReturnValue();
        if (!(world instanceof ServerLevel level) || result == null || result.isEmpty()) return;
        FactoryGridReceipt group = (FactoryGridReceipt)(Object)ingredients;
        CompoundTag input = group.jem$factoryReceipt(), origin = input.getCompound("origin");
        if (!input.getBoolean("confirmed") || !input.getString("phase").equals("ingredients")
                || FactoryRecipeProof.panel(level, origin) == null || !group.jem$factoryContents().equals(FactoryRecipeProof.expected(origin))
                || !ItemStack.isSameItemSameTags(result, ItemStack.of(origin.getCompound("product"))) || result.getCount() < origin.getInt("batch")) return;
        CompoundTag product = new CompoundTag();
        product.put("origin", origin.copy()); product.putString("phase", "product");
        product.putInt("produced", result.getCount()); product.putInt("remaining", result.getCount()); product.putInt("delivered", 0);
        ((FactoryResultReceipt)(Object)result).jem$factoryResult(product);
    }
}
