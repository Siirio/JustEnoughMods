package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.photo.PhotoPrintingProof;
import io.github.mortuusars.exposure.world.item.ChromaticSheetItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ChromaticSheetItem.class, remap = false)
public abstract class PhotoChromaticResultQuestMixin {
    @Inject(method = "combineIntoPhotograph", at = @At("RETURN"))
    private void jem$created(ServerPlayer player, ItemStack sheet, boolean printed, CallbackInfoReturnable<ItemStack> callback) {
        if (printed) PhotoPrintingProof.composite(player, callback.getReturnValue());
    }
}
