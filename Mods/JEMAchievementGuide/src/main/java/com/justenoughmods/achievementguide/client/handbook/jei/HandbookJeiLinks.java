package com.justenoughmods.achievementguide.client.handbook.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** Optional client route. The exact displayed stack, including book NBT, is the JEI focus. */
@JeiPlugin
public final class HandbookJeiLinks implements IModPlugin {
    private static IJeiRuntime runtime;

    @Override public ResourceLocation getPluginUid() { return new ResourceLocation("jem_advancements", "handbook_links"); }
    @Override public void onRuntimeAvailable(IJeiRuntime value) { runtime = value; }
    @Override public void onRuntimeUnavailable() { runtime = null; }

    public static boolean open(ItemStack stack, boolean uses) {
        if (runtime == null || stack.isEmpty()) return false;
        var role = uses ? RecipeIngredientRole.INPUT : RecipeIngredientRole.OUTPUT;
        var focus = runtime.getJeiHelpers().getFocusFactory().createFocus(role, VanillaTypes.ITEM_STACK, stack.copy());
        runtime.getRecipesGui().show(focus);
        return true;
    }
}
