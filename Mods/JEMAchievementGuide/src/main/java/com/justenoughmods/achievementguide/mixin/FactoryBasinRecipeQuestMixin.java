package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.create.FactoryBasinReceipt;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.basin.BasinRecipe;
import net.minecraft.world.item.crafting.Recipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = BasinRecipe.class, remap = false)
public abstract class FactoryBasinRecipeQuestMixin {
    @Inject(method = "apply(Lcom/simibubi/create/content/processing/basin/BasinBlockEntity;Lnet/minecraft/world/item/crafting/Recipe;)Z", at = @At("HEAD"))
    private static void jem$beginFactoryBasinRecipe(BasinBlockEntity basin, Recipe<?> recipe, CallbackInfoReturnable<Boolean> callback) {
        ((FactoryBasinReceipt)basin).jem$factoryBegin(recipe);
    }
    @Inject(method = "apply(Lcom/simibubi/create/content/processing/basin/BasinBlockEntity;Lnet/minecraft/world/item/crafting/Recipe;)Z", at = @At("RETURN"))
    private static void jem$finishedFactoryBasinRecipe(BasinBlockEntity basin, Recipe<?> recipe, CallbackInfoReturnable<Boolean> callback) {
        ((FactoryBasinReceipt)basin).jem$factoryFinish(callback.getReturnValue());
    }
}
