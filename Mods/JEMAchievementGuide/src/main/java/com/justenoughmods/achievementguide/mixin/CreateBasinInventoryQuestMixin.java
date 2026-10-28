package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.create.CreateRunProofs;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.simibubi.create.content.processing.basin.BasinInventory", remap = false)
public abstract class CreateBasinInventoryQuestMixin {
    @Shadow private BasinBlockEntity blockEntity;

    @Inject(method = "insertItem", at = @At("RETURN"), remap = false)
    private void jem$inputChanged(int slot, ItemStack input, boolean simulate, CallbackInfoReturnable<ItemStack> callback) {
        if (!simulate && callback.getReturnValue().getCount() < input.getCount()) {
            CreateRunProofs.invalidate((BlockEntity)(Object)blockEntity);
        }
    }
}
