package com.siirio.jemcinematicinteractions;

import dev.dyslexic.cinematic.client.SceneScreen;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.handlers.IGuiProperties;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;

@JeiPlugin
public final class CinematicJeiLayoutPlugin implements IModPlugin {
    private static final ResourceLocation ID = new ResourceLocation(
            JemCinematicInteractions.MOD_ID, "scene_layout");

    @Override
    public ResourceLocation getPluginUid() {
        return ID;
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGuiScreenHandler(SceneScreen.class, CinematicJeiLayoutPlugin::properties);
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        registration.addRecipeTransferHandler(
                new CinematicCraftingTransferHandler(registration.getTransferHelper()),
                RecipeTypes.CRAFTING
        );
    }

    private static IGuiProperties properties(SceneScreen screen) {
        int width = 176;
        int height = Math.min(166, screen.height);
        int left = (screen.width - width) / 2;
        int top = (screen.height - height) / 2;
        return new SceneProperties(screen.getClass(), left, top, width, height, screen.width, screen.height);
    }

    private record SceneProperties(
            Class<? extends Screen> screenClass,
            int guiLeft,
            int guiTop,
            int guiXSize,
            int guiYSize,
            int screenWidth,
            int screenHeight) implements IGuiProperties {
        @Override public Class<? extends Screen> getScreenClass() { return screenClass; }
        @Override public int getGuiLeft() { return guiLeft; }
        @Override public int getGuiTop() { return guiTop; }
        @Override public int getGuiXSize() { return guiXSize; }
        @Override public int getGuiYSize() { return guiYSize; }
        @Override public int getScreenWidth() { return screenWidth; }
        @Override public int getScreenHeight() { return screenHeight; }
    }
}
