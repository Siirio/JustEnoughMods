package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.create.FactoryIngredients;
import com.simibubi.create.content.kinetics.crafter.MechanicalCrafterBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "com.simibubi.create.content.kinetics.crafter.MechanicalCrafterBlockEntity$Inventory", remap = false)
public abstract class FactoryCrafterInventoryQuestMixin {
    @Inject(method = "lambda$new$0", at = @At("HEAD"))
    private void jem$actualFactoryInput(MechanicalCrafterBlockEntity crafter, Integer slot, CallbackInfo callback) {
        FactoryIngredients.inserted(crafter);
    }
}
