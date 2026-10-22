package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.client.handbook.HandbookNavigation;
import net.minecraft.client.gui.components.EditBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vazkii.patchouli.client.book.gui.GuiBookEntryList;

@Pseudo
@Mixin(value = GuiBookEntryList.class, remap = false)
public abstract class PatchouliHandbookReadingMixin {
    @Shadow private EditBox searchField;
    @Shadow abstract void buildEntryButtons();
    @Unique private String jem$query;

    @Inject(method = "m_7856_()V", at = @At("HEAD"))
    private void jem$rememberQuery(CallbackInfo callback) {
        if (HandbookNavigation.BOOK.equals(((GuiBookEntryList) (Object) this).book.id) && searchField != null) {
            jem$query = searchField.getValue();
        }
    }

    @Inject(method = "m_7856_()V", at = @At("RETURN"))
    private void jem$restoreQuery(CallbackInfo callback) {
        if (jem$query != null && !jem$query.isEmpty()) {
            searchField.setValue(jem$query);
            buildEntryButtons();
        }
    }
}
