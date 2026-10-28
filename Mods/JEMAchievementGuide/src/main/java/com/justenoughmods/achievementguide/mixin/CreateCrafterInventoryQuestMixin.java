package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.create.CreateCrafterInput;
import com.justenoughmods.achievementguide.integration.create.CreateRunProofs;
import com.simibubi.create.content.kinetics.crafter.MechanicalCrafterBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.simibubi.create.content.kinetics.crafter.MechanicalCrafterBlockEntity$Inventory", remap = false)
public abstract class CreateCrafterInventoryQuestMixin {
    // This native callback runs after mutation and before synchronous checkCompletedRecipe/begin.
    @Inject(method = "lambda$new$0", at = @At("HEAD"), remap = false)
    private void jem$committedIngredient(MechanicalCrafterBlockEntity crafter, Integer slot, CallbackInfo callback) {
        if (crafter.getLevel() != null && !crafter.getLevel().isClientSide) {
            CreateRunProofs.bind((BlockEntity)(Object)crafter, crafter.getInventory(),
                    CreateCrafterInput.supplier(crafter.getInventory().getStackInSlot(0)));
        }
    }
}
