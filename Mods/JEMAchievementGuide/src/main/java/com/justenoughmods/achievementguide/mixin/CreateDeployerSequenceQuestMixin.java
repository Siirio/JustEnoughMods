package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.create.CreateTransportInputs;
import com.justenoughmods.achievementguide.integration.create.CreateTransportProof;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.content.kinetics.deployer.DeployerBlockEntity;
import com.simibubi.create.foundation.recipe.RecipeApplier;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.simibubi.create.content.kinetics.deployer.BeltDeployerCallbacks", remap = false)
public abstract class CreateDeployerSequenceQuestMixin {
    @Redirect(method = "activate", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/foundation/recipe/RecipeApplier;applyRecipeOn(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/crafting/Recipe;Z)Ljava/util/List;"), remap = false)
    private static List<ItemStack> jem$nativeStep(Level level, ItemStack item, Recipe<?> recipe, boolean simulate,
                                                TransportedItemStack input, TransportedItemStackHandlerBehaviour handler,
                                                DeployerBlockEntity deployer, Recipe<?> nativeRecipe) {
        return CreateTransportInputs.processing(input, () -> RecipeApplier.applyRecipeOn(level, item, recipe, simulate));
    }
    @Inject(method = "lambda$activate$0", at = @At("RETURN"), remap = false)
    private static void jem$transformed(TransportedItemStack input, ItemStack item, CallbackInfoReturnable<TransportedItemStack> callback) {
        ((CreateTransportProof)(Object)input).jem$applyAdvance((CreateTransportProof)(Object)callback.getReturnValue(), item);
    }
}
