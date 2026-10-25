package com.justenoughmods.achievementguide.mixin;

import com.google.gson.JsonObject;
import com.justenoughmods.achievementguide.client.handbook.HandbookNavigation;
import com.justenoughmods.achievementguide.client.handbook.HandbookWorldVisibility;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import vazkii.patchouli.client.book.BookEntry;
import vazkii.patchouli.common.book.Book;

/** Patchouli 85 has only per-player advancement gates, so world discovery is projected here. */
@Pseudo
@Mixin(value = BookEntry.class, remap = false)
public abstract class PatchouliHandbookVisibilityMixin {
    @Unique private ResourceLocation jem$dimension;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void jem$readDimension(JsonObject json, ResourceLocation id, Book book, String addedBy, CallbackInfo callback) {
        if (HandbookNavigation.BOOK.equals(book.id) && id.getPath().startsWith("handbook/bosses/")
                && json.has("jem_dimension")) {
            jem$dimension = new ResourceLocation(json.get("jem_dimension").getAsString());
        }
    }

    @Inject(method = "shouldHide", at = @At("HEAD"), cancellable = true)
    private void jem$hideClosedDimension(CallbackInfoReturnable<Boolean> callback) {
        if (HandbookWorldVisibility.hidden(jem$dimension)) callback.setReturnValue(true);
    }

    @Inject(method = "isLocked", at = @At("HEAD"), cancellable = true)
    private void jem$lockClosedDimension(CallbackInfoReturnable<Boolean> callback) {
        if (HandbookWorldVisibility.hidden(jem$dimension)) callback.setReturnValue(true);
    }
}
