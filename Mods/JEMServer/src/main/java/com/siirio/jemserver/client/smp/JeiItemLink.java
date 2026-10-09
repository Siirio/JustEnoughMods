package com.siirio.jemserver.client.smp;

import java.lang.reflect.Field;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.common.Internal;
import mezz.jei.gui.bookmarks.BookmarkList;
import mezz.jei.gui.overlay.bookmarks.BookmarkOverlay;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;

final class JeiItemLink {
    static boolean open(ItemStack source) {
        if (source.isEmpty() || !ModList.get().isLoaded("jei")) return false;
        try {
            var runtime = Internal.getOptionalJeiRuntime().orElse(null);
            if (runtime == null) return false;
            var stack = source.copy();
            stack.setCount(1);
            var typed = runtime.getIngredientManager().createTypedIngredient(VanillaTypes.ITEM_STACK, stack).orElse(null);
            if (typed == null) return false;
            addBookmark(runtime.getBookmarkOverlay(), typed);
            var focus = runtime.getJeiHelpers().getFocusFactory().createFocus(RecipeIngredientRole.OUTPUT, typed);
            runtime.getRecipesGui().show(focus);
            return true;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException failure) {
            return false;
        }
    }

    private static void addBookmark(mezz.jei.api.runtime.IBookmarkOverlay overlay, mezz.jei.api.ingredients.ITypedIngredient<?> ingredient) throws ReflectiveOperationException {
        if (!(overlay instanceof BookmarkOverlay bookmarks)) return;
        Field field = BookmarkOverlay.class.getDeclaredField("bookmarkList");
        field.setAccessible(true);
        if (field.get(bookmarks) instanceof BookmarkList list) list.addIngredientBookmark(ingredient);
    }

    private JeiItemLink() {}
}
