package com.siirio.jemlightingsandbox;

import java.util.function.IntConsumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraftforge.client.gui.widget.ForgeSlider;

final class FogSettingsScreen extends Screen {
    private static final String KEY = "screen.jem_lighting_sandbox.settings.fog";
    private final Screen parent;

    FogSettingsScreen(Screen parent) {
        super(Component.translatable(KEY));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int controlWidth = Math.min(260, width - 24);
        int left = (width - controlWidth) / 2;
        int top = (height - 76) / 2;
        addRenderableWidget(new FogSlider(left, top, controlWidth, KEY + ".distance",
                LightingSettings.MAX_FOG_DISTANCE, LightingSettings.fogDistance(), LightingSettings::setFogDistance));
        addRenderableWidget(new FogSlider(left, top + 24, controlWidth, KEY + ".density",
                LightingSettings.MAX_FOG_DENSITY, LightingSettings.fogDensity(), LightingSettings::setFogDensity));
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
                .bounds(left, top + 56, controlWidth, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, Math.max(4, (height - 76) / 2 - 20), 0xFFFFFF);
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

    private static final class FogSlider extends ForgeSlider {
        private final String label;
        private final int maximum;
        private final IntConsumer save;

        FogSlider(int x, int y, int width, String label, int max, int value, IntConsumer save) {
            super(x, y, width, 20, Component.empty(), Component.empty(), 0, max, value, true);
            this.label = label;
            this.maximum = max;
            this.save = save;
            updateMessage();
            setTooltip(Tooltip.create(Component.translatable(label + ".tooltip")));
        }

        @Override
        protected void updateMessage() {
            if (label != null) {
                setMessage(Component.translatable(label, getValueInt()));
            }
        }

        @Override
        public void onRelease(double mouseX, double mouseY) {
            super.onRelease(mouseX, mouseY);
            save.accept(getValueInt());
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            int before = getValueInt();
            boolean handled = super.keyPressed(keyCode, scanCode, modifiers);
            if (getValueInt() != before) {
                save.accept(getValueInt());
            }
            return handled;
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
            if (!isHoveredOrFocused() || amount == 0) {
                return false;
            }
            setValue(Mth.clamp(getValueInt() + (amount > 0 ? 1 : -1), 0, maximum));
            save.accept(getValueInt());
            return true;
        }
    }
}
