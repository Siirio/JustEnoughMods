package com.siirio.jemlightingsandbox;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.gui.widget.ForgeSlider;

final class WeatherDensityScreen extends Screen {
    private static final String KEY = "screen.jem_lighting_sandbox.settings.precipitation";
    private static final int CONTENT_HEIGHT = 100;
    private final Screen parent;

    WeatherDensityScreen(Screen parent) {
        super(Component.translatable(KEY));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int controlWidth = Math.min(260, width - 24);
        int left = (width - controlWidth) / 2;
        int top = (height - CONTENT_HEIGHT) / 2;
        addRenderableWidget(new DensitySlider(left, top, controlWidth, false));
        addRenderableWidget(new DensitySlider(left, top + 24, controlWidth, true));
        addRenderableWidget(CycleButton.onOffBuilder(LightingSettings.darkCampfireSmoke())
                .create(left, top + 48, controlWidth, 20, Component.translatable(KEY + ".dark_smoke"),
                        (button, enabled) -> LightingSettings.setDarkCampfireSmoke(enabled)));
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
                .bounds(left, top + 80, controlWidth, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, Math.max(4, (height - CONTENT_HEIGHT) / 2 - 20), 0xFFFFFF);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static final class DensitySlider extends ForgeSlider {
        private final boolean storm;

        DensitySlider(int x, int y, int width, boolean storm) {
            super(x, y, width, 20, Component.empty(), Component.empty(), LightingSettings.MIN_WEATHER_DENSITY,
                    LightingSettings.MAX_WEATHER_DENSITY, LightingSettings.weatherDensity(storm), true);
            this.storm = storm;
            setTooltip(Tooltip.create(Component.translatable(KEY + ".global")));
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.translatable(KEY + (storm ? ".storm" : ".rain"), getValueInt()));
        }

        @Override
        public void onRelease(double mouseX, double mouseY) {
            super.onRelease(mouseX, mouseY);
            saveDensity();
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            int before = getValueInt();
            boolean handled = super.keyPressed(keyCode, scanCode, modifiers);
            if (getValueInt() != before) {
                saveDensity();
                return true;
            }
            return handled;
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
            if (!isHoveredOrFocused() || amount == 0) {
                return false;
            }
            setValue(Math.max(LightingSettings.MIN_WEATHER_DENSITY,
                    Math.min(LightingSettings.MAX_WEATHER_DENSITY, getValueInt() + (amount > 0 ? 5 : -5))));
            saveDensity();
            return true;
        }

        private void saveDensity() {
            LightingSettings.setWeatherDensity(storm, getValueInt());
        }
    }
}
