package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.create.CreateRunProofs;
import com.simibubi.create.content.processing.recipe.ProcessingInventory;
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
@Mixin(targets = "com.simibubi.create.content.kinetics.crusher.CrushingWheelControllerBlockEntity", remap = false)
public abstract class CreateCrusherQuestMixin {
    @Shadow public ProcessingInventory inventory;
    @Unique private UUID jem$inputActor;
    @Unique private ItemStack jem$input;
    @Unique private UUID jem$processingActor;
    @Inject(method = "itemInserted", at = @At("HEAD"), remap = false)
    private void jem$unknownInput(ItemStack item, CallbackInfo callback) { CreateRunProofs.invalidate((BlockEntity)(Object)this); }
    @Inject(method = "intakeItem", at = @At("HEAD"), remap = false)
    private void jem$beforeInput(ItemEntity entity, CallbackInfo callback) {
        jem$inputActor = CreateRunProofs.actor(entity); jem$input = entity.getItem().copy();
    }
    @Inject(method = "intakeItem", at = @At("RETURN"), remap = false)
    private void jem$afterInput(ItemEntity entity, CallbackInfo callback) {
        if (!entity.isAlive() && ItemStack.matches(inventory.getStackInSlot(0), jem$input))
            CreateRunProofs.bind((BlockEntity)(Object)this, inventory, jem$inputActor);
        jem$inputActor = null; jem$input = null;
    }
    @Inject(method = "applyRecipe", at = @At("HEAD"), remap = false)
    private void jem$beforeRecipe(CallbackInfo callback) { jem$processingActor = CreateRunProofs.pending((BlockEntity)(Object)this, inventory); }
    @Inject(method = "applyRecipe", at = @At("RETURN"), remap = false)
    private void jem$afterRecipe(CallbackInfo callback) {
        BlockEntity owner = (BlockEntity)(Object)this;
        if (jem$processingActor != null && owner.getLevel() instanceof ServerLevel level && inventory.getStackInSlot(0).isEmpty()) {
            for (int slot = 1; slot < inventory.getSlots(); slot++) if (!inventory.getStackInSlot(slot).isEmpty()) {
                CreateRunProofs.emit(level, jem$processingActor, "v3_eng_crush"); break;
            }
        }
        CreateRunProofs.invalidate(owner); jem$processingActor = null;
    }
}
