package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.client.handbook.HandbookNavigation;
import com.justenoughmods.achievementguide.client.handbook.HandbookSearch;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import vazkii.patchouli.client.book.BookEntry;

@Pseudo
@Mixin(value = BookEntry.class, remap = false)
public abstract class PatchouliHandbookSearchMixin {
    @Inject(method = "isFoundByQuery", at = @At("RETURN"), cancellable = true)
    private void jem$aliases(String query, CallbackInfoReturnable<Boolean> callback) {
        BookEntry entry = (BookEntry) (Object) this;
        if (!callback.getReturnValueZ() && HandbookNavigation.BOOK.equals(entry.getBook().id)
                && HandbookSearch.matches(entry.getId(), query)) callback.setReturnValue(true);
    }
}
