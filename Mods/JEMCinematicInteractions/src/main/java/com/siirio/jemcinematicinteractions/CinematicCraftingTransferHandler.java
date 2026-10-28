package com.siirio.jemcinematicinteractions;

import dev.dyslexic.cinematic.client.Scene;
import dev.dyslexic.cinematic.client.TableStation;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.crafting.CraftingRecipe;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;

public final class CinematicCraftingTransferHandler
        implements IRecipeTransferHandler<CinematicCraftingTransferMenu, CraftingRecipe> {
    private static final NativeTableRecipeBook NATIVE_RECIPE_BOOK = new NativeTableRecipeBook();

    private final IRecipeTransferHandlerHelper helper;

    public CinematicCraftingTransferHandler(IRecipeTransferHandlerHelper helper) {
        this.helper = helper;
    }

    @Override
    public Class<? extends CinematicCraftingTransferMenu> getContainerClass() {
        return CinematicCraftingTransferMenu.class;
    }

    @Override
    public Optional<net.minecraft.world.inventory.MenuType<CinematicCraftingTransferMenu>> getMenuType() {
        return Optional.empty();
    }

    @Override
    public RecipeType<CraftingRecipe> getRecipeType() {
        return RecipeTypes.CRAFTING;
    }

    @Override
    public IRecipeTransferError transferRecipe(
            CinematicCraftingTransferMenu menu,
            CraftingRecipe recipe,
            IRecipeSlotsView recipeSlots,
            Player player,
            boolean maxTransfer,
            boolean doTransfer
    ) {
        if (!(Scene.station instanceof TableStation station)) {
            return helper.createInternalError();
        }

        try {
            if (!NATIVE_RECIPE_BOOK.contains(station, recipe)) {
                return helper.createInternalError();
            }
            if (doTransfer) {
                NATIVE_RECIPE_BOOK.layOut(station, recipe, maxTransfer);
            }
            return null;
        } catch (ReflectiveOperationException exception) {
            JemCinematicInteractions.LOGGER.error("Could not transfer a JEI recipe to the Cinematic crafting table", exception);
            return helper.createInternalError();
        }
    }

    private static final class NativeTableRecipeBook {
        private final Field recipeBook;
        private final Field guide;
        private final Method entry;
        private final Method show;
        private final Method layOut;

        private NativeTableRecipeBook() {
            try {
                Class<?> recipeBookClass = Class.forName("dev.dyslexic.cinematic.client.TableRecipeBook");
                Class<?> guideClass = Class.forName("dev.dyslexic.cinematic.client.TableRecipeGuide");
                Class<?> entryClass = Class.forName("dev.dyslexic.cinematic.client.TableRecipeGuide$Entry");

                recipeBook = TableStation.class.getDeclaredField("recipeBook");
                guide = recipeBookClass.getDeclaredField("guide");
                entry = guideClass.getDeclaredMethod("entry", net.minecraft.resources.ResourceLocation.class);
                show = recipeBookClass.getDeclaredMethod("show", entryClass);
                layOut = recipeBookClass.getDeclaredMethod("layOut", boolean.class);

                recipeBook.setAccessible(true);
                guide.setAccessible(true);
                entry.setAccessible(true);
                show.setAccessible(true);
                layOut.setAccessible(true);
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException("Cinematic Interactions 0.18.11 table recipe API changed", exception);
            }
        }

        private boolean contains(TableStation station, CraftingRecipe recipe) throws ReflectiveOperationException {
            Object book = recipeBook.get(station);
            Object recipeGuide = guide.get(book);
            return entry.invoke(recipeGuide, recipe.getId()) != null;
        }

        private void layOut(TableStation station, CraftingRecipe recipe, boolean maxTransfer)
                throws ReflectiveOperationException {
            Object book = recipeBook.get(station);
            Object recipeGuide = guide.get(book);
            Object selectedEntry = entry.invoke(recipeGuide, recipe.getId());
            show.invoke(book, selectedEntry);
            layOut.invoke(book, maxTransfer);
        }
    }
}
