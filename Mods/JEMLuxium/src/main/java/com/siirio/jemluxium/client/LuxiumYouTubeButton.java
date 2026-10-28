package com.siirio.jemluxium.client;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class LuxiumYouTubeButton extends AbstractWidget {
    public static final int WIDTH = 64;
    public static final int HEIGHT = 62;

    private static final String URL = "https://www.youtube.com/@Vinlanx";
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("luxium", "textures/youtube.png");

    public LuxiumYouTubeButton(int x, int y) {
        super(x, y, WIDTH, HEIGHT, Component.translatable("jem_luxium.options.youtube"));
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        Minecraft minecraft = Minecraft.getInstance();
        Screen parent = minecraft.screen;
        minecraft.setScreen(new ConfirmLinkScreen(confirmed -> {
            if (confirmed) {
                Util.getPlatform().openUri(URL);
            }
            minecraft.setScreen(parent);
        }, URL, false));
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        graphics.blit(TEXTURE, getX(), getY(), 0.0F, 0.0F, getWidth(), getHeight(), getWidth(), getHeight());
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
