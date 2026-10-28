package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.create.CreateRunProofs;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.content.processing.recipe.ProcessingInventory;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.simibubi.create.content.kinetics.saw.SawBlockEntity", remap = false)
public abstract class CreateSawRecipeQuestMixin {
    @Shadow public ProcessingInventory inventory;
    @Shadow private FilteringBehaviour filtering;
    @Unique private UUID jem$inputActor;
    @Unique private ItemStack jem$input;
    @Unique private boolean jem$empty;
    @Unique private UUID jem$processingActor;
    @Unique private ItemStack jem$filter;
    @Inject(method = "start", at = @At("HEAD"), remap = false)
    private void jem$unknownInput(ItemStack item, CallbackInfo callback) { CreateRunProofs.invalidate((BlockEntity)(Object)this); }
    @Inject(method = "insertItem", at = @At("HEAD"), remap = false)
    private void jem$beforeInput(ItemEntity entity, CallbackInfo callback) {
        jem$inputActor = CreateRunProofs.actor(entity); jem$input = entity.getItem().copy(); jem$empty = inventory.isEmpty();
    }
    @Inject(method = "insertItem", at = @At("RETURN"), remap = false)
    private void jem$afterInput(ItemEntity entity, CallbackInfo callback) {
        if (jem$empty && (!entity.isAlive() || entity.getItem().getCount() < jem$input.getCount())
                && ItemStack.matches(inventory.getStackInSlot(0), jem$input)) CreateRunProofs.bind((BlockEntity)(Object)this, inventory, jem$inputActor);
        jem$inputActor = null; jem$input = null;
    }
    @Inject(method = "applyRecipe", at = @At("HEAD"), remap = false)
    private void jem$beforeRecipe(CallbackInfo callback) {
        jem$processingActor = filtering.getFilter().isEmpty() || PackageItem.isPackage(inventory.getStackInSlot(0)) ? null
                : CreateRunProofs.pending((BlockEntity)(Object)this, inventory);
        jem$filter = filtering.getFilter().copy();
    }
    @Inject(method = "applyRecipe", at = @At("RETURN"), remap = false)
    private void jem$afterRecipe(CallbackInfo callback) {
        BlockEntity owner = (BlockEntity)(Object)this;
        if (jem$processingActor != null && owner.getLevel() instanceof ServerLevel level && inventory.getStackInSlot(0).isEmpty()
                && ItemStack.matches(jem$filter, filtering.getFilter())) {
            for (int slot = 1; slot < inventory.getSlots(); slot++) if (!inventory.getStackInSlot(slot).isEmpty() && filtering.test(inventory.getStackInSlot(slot))) {
                CreateRunProofs.emit(level, jem$processingActor, "v3_eng_saw_recipe"); break;
            }
        }
        CreateRunProofs.invalidate(owner); jem$processingActor = null; jem$filter = null;
    }
}
