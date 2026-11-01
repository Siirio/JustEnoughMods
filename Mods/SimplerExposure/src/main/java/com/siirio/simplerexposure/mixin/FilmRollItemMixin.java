package com.siirio.simplerexposure.mixin;

import com.siirio.simplerexposure.InstantPhotographs;
import io.github.mortuusars.exposure.world.item.FilmRollItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = FilmRollItem.class, remap = false)
public abstract class FilmRollItemMixin {
    @Inject(method = "canAddFrame", at = @At("HEAD"), cancellable = true)
    private void simplerExposure$limitUsableFilm(ItemStack stack, CallbackInfoReturnable<Boolean> callback) {
        if (!InstantPhotographs.isRegularFilm(stack)) callback.setReturnValue(false);
    }
}
