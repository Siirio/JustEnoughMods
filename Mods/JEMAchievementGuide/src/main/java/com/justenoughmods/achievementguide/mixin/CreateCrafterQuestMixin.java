package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.create.CreateCrafterGrid;
import com.justenoughmods.achievementguide.integration.create.CreateRunProofs;
import com.simibubi.create.content.kinetics.crafter.MechanicalCrafterBlockEntity;
import com.simibubi.create.content.kinetics.crafter.RecipeGridHandler;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.simibubi.create.content.kinetics.crafter.MechanicalCrafterBlockEntity", remap = false)
public abstract class CreateCrafterQuestMixin {
    @Shadow protected RecipeGridHandler.GroupedItems groupedItems;
    @Unique private UUID jem$startingActor;
    @Unique private UUID jem$craftingActor;
    @Unique private ItemStack jem$result = ItemStack.EMPTY;

    @Inject(method = "begin", at = @At("HEAD"), remap = false)
    private void jem$consumeProof(CallbackInfo callback) {
        MechanicalCrafterBlockEntity crafter = (MechanicalCrafterBlockEntity)(Object)this;
        jem$startingActor = CreateRunProofs.pending((BlockEntity)(Object)this, crafter.getInventory());
        CreateRunProofs.invalidate((BlockEntity)(Object)this);
    }
    @Inject(method = "begin", at = @At("RETURN"), remap = false)
    private void jem$beginNativeGrid(CallbackInfo callback) {
        ((CreateCrafterGrid)(Object)groupedItems).jem$supplier(jem$startingActor);
        jem$startingActor = null;
    }
    @Inject(method = "tick", at = @At("HEAD"), remap = false)
    private void jem$clearTick(CallbackInfo callback) { jem$craftingActor = null; jem$result = ItemStack.EMPTY; }

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/kinetics/crafter/RecipeGridHandler;tryToApplyRecipe(Lnet/minecraft/world/level/Level;Lcom/simibubi/create/content/kinetics/crafter/RecipeGridHandler$GroupedItems;)Lnet/minecraft/world/item/ItemStack;"), remap = false)
    private ItemStack jem$nativeRecipe(Level level, RecipeGridHandler.GroupedItems ingredients) {
        ItemStack result = RecipeGridHandler.tryToApplyRecipe(level, ingredients);
        if (level instanceof ServerLevel && result != null && !result.isEmpty()) {
            jem$craftingActor = ((CreateCrafterGrid)(Object)ingredients).jem$supplier();
            jem$result = result.copy();
        }
        return result;
    }
    // In6.0.8 tick's second sendData follows output-grid replacement and CRAFTING/countDown commit.
    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/kinetics/crafter/MechanicalCrafterBlockEntity;sendData()V", ordinal = 1), remap = false)
    private void jem$committedCraft(CallbackInfo callback) {
        if (jem$craftingActor != null && !jem$result.isEmpty()
                && ((CreateCrafterGrid)(Object)groupedItems).jem$containsOutput(jem$result)
                && ((BlockEntity)(Object)this).getLevel() instanceof ServerLevel level) {
            CreateRunProofs.emit(level, jem$craftingActor, "create_mechanical_craft");
            jem$craftingActor = null;
        }
    }
}
