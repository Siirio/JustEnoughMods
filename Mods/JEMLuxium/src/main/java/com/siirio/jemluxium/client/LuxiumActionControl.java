package com.siirio.jemluxium.client;

import me.jellysquid.mods.sodium.client.gui.options.Option;
import me.jellysquid.mods.sodium.client.gui.options.control.Control;
import me.jellysquid.mods.sodium.client.gui.options.control.ControlElement;
import me.jellysquid.mods.sodium.client.gui.widgets.FlatButtonWidget;
import me.jellysquid.mods.sodium.client.util.Dim2i;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.CommonInputs;
import net.minecraft.network.chat.Component;

final class LuxiumActionControl implements Control<Boolean> {
    private static final int BUTTON_WIDTH = 70;

    private final Option<Boolean> option;
    private final Component buttonLabel;
    private final Runnable action;

    LuxiumActionControl(Option<Boolean> option, Component buttonLabel, Runnable action) {
        this.option = option;
        this.buttonLabel = buttonLabel;
        this.action = action;
    }

    @Override
    public Option<Boolean> getOption() {
        return option;
    }

    @Override
    public ControlElement<Boolean> createElement(Dim2i dim) {
        return new Element(option, dim, buttonLabel, action);
    }

    @Override
    public int getMaxWidth() {
        return BUTTON_WIDTH;
    }

    private static final class Element extends ControlElement<Boolean> {
        private final FlatButtonWidget button;
        private final Runnable action;

        private Element(Option<Boolean> option, Dim2i dim, Component label, Runnable action) {
            super(option, dim);
            this.action = action;
            this.button = new FlatButtonWidget(
                    new Dim2i(dim.getLimitX() - BUTTON_WIDTH, dim.y() + 1, BUTTON_WIDTH - 6, dim.height() - 2),
                    label,
                    action
            );
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
            super.render(graphics, mouseX, mouseY, delta);
            button.render(graphics, mouseX, mouseY, delta);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int buttonCode) {
            return button.mouseClicked(mouseX, mouseY, buttonCode);
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (isFocused() && CommonInputs.selected(keyCode)) {
                action.run();
                playClickSound();
                return true;
            }
            return false;
        }
    }
}
