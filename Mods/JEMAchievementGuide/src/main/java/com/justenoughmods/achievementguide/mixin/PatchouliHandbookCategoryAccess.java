package com.justenoughmods.achievementguide.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;
import vazkii.patchouli.client.book.BookCategory;
import vazkii.patchouli.client.book.gui.GuiBookCategory;

/** Patchouli exposes no category getter; equality also includes the current spread. */
@Pseudo
@Mixin(value = GuiBookCategory.class, remap = false)
public interface PatchouliHandbookCategoryAccess {
    @Accessor("category")
    BookCategory jem$category();
}
