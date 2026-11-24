package com.siirio.jemcompat.client.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.runtime.IJeiRuntime;
import mezz.jei.common.config.IClientToggleState;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.common.MinecraftForge;
import com.siirio.jemcompat.mixin.client.IngredientListOverlayAccessor;

@JeiPlugin
public final class JEMJeiPlugin implements IModPlugin {
    private static final ResourceLocation PLUGIN_ID = new ResourceLocation("jemcompat", "jei");
    private static IJeiRuntime runtime;

    public JEMJeiPlugin() {
        MinecraftForge.EVENT_BUS.register(CuratedJeiOverlay.class);
    }

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_ID;
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime availableRuntime) {
        runtime = availableRuntime;
        var retiredStation = ForgeRegistries.ITEMS.getValue(new ResourceLocation("jemcompat", "augment_bay"));
        if (retiredStation != null && retiredStation != net.minecraft.world.item.Items.AIR) {
            availableRuntime.getIngredientManager().removeIngredientsAtRuntime(
                    mezz.jei.api.constants.VanillaTypes.ITEM_STACK,
                    java.util.List.of(new net.minecraft.world.item.ItemStack(retiredStation)));
        }
        if (net.minecraftforge.fml.ModList.get().isLoaded("handcrafted")
                && net.minecraftforge.fml.ModList.get().isLoaded("bbb")) {
            hideHammer(availableRuntime, new ResourceLocation("handcrafted", "hammer"));
            if (net.minecraftforge.fml.ModList.get().isLoaded("jem_building")) {
                hideHammer(availableRuntime, new ResourceLocation("bbb", "hammer"));
            }
        }
        com.siirio.jemcompat.feature.transport.BackpackPolicy.retiredItems()
                .forEach(id -> hideHammer(availableRuntime, id));
        CuratedJeiCatalog.rebuild(availableRuntime);
        setNativeOverlayVisible(false);
    }

    private static void hideHammer(IJeiRuntime runtime, ResourceLocation id) {
        var item = ForgeRegistries.ITEMS.getValue(id);
        if (item == null || item == net.minecraft.world.item.Items.AIR) return;
        runtime.getIngredientManager().removeIngredientsAtRuntime(
                mezz.jei.api.constants.VanillaTypes.ITEM_STACK,
                java.util.List.of(new net.minecraft.world.item.ItemStack(item)));
    }

    @Override
    public void onRuntimeUnavailable() {
        runtime = null;
        CuratedJeiCatalog.clear();
        CuratedJeiOverlay.clear();
    }

    public static boolean hasSearchFocus() {
        return runtime != null && (runtime.getIngredientListOverlay().hasKeyboardFocus()
                || CuratedJeiOverlay.hasSearchFocus());
    }

    public static IJeiRuntime runtime() {
        return runtime;
    }

    public static boolean isNativeOverlayVisible() {
        IClientToggleState state = toggleState();
        return state != null && state.isOverlayEnabled();
    }

    public static void setNativeOverlayVisible(boolean visible) {
        IClientToggleState state = toggleState();
        if (state != null && state.isOverlayEnabled() != visible) {
            state.toggleOverlayEnabled();
        }
    }

    private static IClientToggleState toggleState() {
        if (runtime == null || !(runtime.getIngredientListOverlay() instanceof IngredientListOverlayAccessor accessor)) {
            return null;
        }
        return accessor.jemcompat$getToggleState();
    }

    @Override
    public void registerRecipes(mezz.jei.api.registration.IRecipeRegistration registration) {
        if (!net.minecraftforge.fml.ModList.get().isLoaded("jem_advancements")
                || !net.minecraftforge.fml.ModList.get().isLoaded("patchouli")) return;
        java.util.List<mezz.jei.api.recipe.vanilla.IJeiIngredientInfoRecipe> recipes = new java.util.ArrayList<>();
        for (var info : com.justenoughmods.achievementguide.client.handbook.HandbookItemHelp.informationEntries()) {
            var stack = info.stack();
            var typed = registration.getIngredientManager().createTypedIngredient(mezz.jei.api.constants.VanillaTypes.ITEM_STACK, stack);
            if (typed.isEmpty()) continue;
            java.util.List<mezz.jei.api.ingredients.ITypedIngredient<?>> ingredients = java.util.List.of(typed.get());
            recipes.add(new mezz.jei.api.recipe.vanilla.IJeiIngredientInfoRecipe() {
                @Override
                public java.util.List<mezz.jei.api.ingredients.ITypedIngredient<?>> getIngredients() {
                    return ingredients;
                }

                @Override
                public java.util.List<net.minecraft.network.chat.FormattedText> getDescription() {
                    return info.information()
                            .stream().<net.minecraft.network.chat.FormattedText>map(component -> component).toList();
                }
            });
        }
        registration.addRecipes(mezz.jei.api.constants.RecipeTypes.INFORMATION, recipes);
    }
}
