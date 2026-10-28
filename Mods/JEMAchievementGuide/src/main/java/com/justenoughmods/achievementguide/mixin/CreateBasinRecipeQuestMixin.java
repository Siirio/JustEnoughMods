package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.create.CreateMixingRun;
import com.justenoughmods.achievementguide.integration.create.CreateRunProofs;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.recipe.HeatCondition;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.simibubi.create.content.processing.basin.BasinRecipe", remap = false)
public abstract class CreateBasinRecipeQuestMixin {
    @Inject(method = "apply(Lcom/simibubi/create/content/processing/basin/BasinBlockEntity;Lnet/minecraft/world/item/crafting/Recipe;)Z",
            at = @At("HEAD"), remap = false)
    private static void jem$begin(BasinBlockEntity basin, Recipe<?> recipe, CallbackInfoReturnable<Boolean> callback) {
        boolean heatedMixing = new ResourceLocation("create", "mixing").equals(BuiltInRegistries.RECIPE_TYPE.getKey(recipe.getType()))
                && recipe instanceof ProcessingRecipe<?> processing && processing.getRequiredHeat() != HeatCondition.NONE;
        boolean compacting = new ResourceLocation("create", "compacting").equals(BuiltInRegistries.RECIPE_TYPE.getKey(recipe.getType()))
                && basin.getLevel() != null && basin.getLevel().getBlockEntity(basin.getBlockPos().above(2))
                    instanceof com.simibubi.create.content.kinetics.press.MechanicalPressBlockEntity;
        ((CreateMixingRun)basin).jem$beginCompacting(compacting ? CreateRunProofs.pending((BlockEntity)(Object)basin, basin.inputInventory) : null);
        ((CreateMixingRun)basin).jem$beginMixing(heatedMixing
                ? CreateRunProofs.pending((BlockEntity)(Object)basin, basin.inputInventory) : null);
    }

    @Inject(method = "apply(Lcom/simibubi/create/content/processing/basin/BasinBlockEntity;Lnet/minecraft/world/item/crafting/Recipe;)Z",
            at = @At("RETURN"), remap = false)
    private static void jem$finish(BasinBlockEntity basin, Recipe<?> recipe, CallbackInfoReturnable<Boolean> callback) {
        ((CreateMixingRun)basin).jem$finishMixing(callback.getReturnValueZ());
    }
}
