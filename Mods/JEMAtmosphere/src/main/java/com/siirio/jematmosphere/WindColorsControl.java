package com.siirio.jematmosphere;

import java.util.function.Supplier;
import me.jellysquid.mods.sodium.client.gui.options.Option;
import me.jellysquid.mods.sodium.client.gui.options.control.Control;
import me.jellysquid.mods.sodium.client.gui.options.control.ControlElement;
import me.jellysquid.mods.sodium.client.util.Dim2i;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

final class WindColorsControl implements Control<String> {
    private static final int MAX_WIDTH = 190;
    private static final int MAX_LENGTH = 32767;
    private static final Component EXAMPLE = Component.literal("minecraft:desert=#D9A767");
    private final Option<String> option;
    private final Supplier<String> savedValue;

    WindColorsControl(Option<String> option, Supplier<String> savedValue) {
        this.option = option;
        this.savedValue = savedValue;
    }

    @Override
    public Option<String> getOption() {
        return option;
    }

    @Override
    public ControlElement<String> createElement(Dim2i dim) {
        return new Element(option, dim, savedValue);
    }

    @Override
    public int getMaxWidth() {
        return MAX_WIDTH;
    }

    private static final class Element extends ControlElement<String> {
        private final EditBox editor;
        private final Supplier<String> savedValue;
        private String visibleOptionValue;
        private boolean invalid;

        Element(Option<String> option, Dim2i dim, Supplier<String> savedValue) {
            super(option, dim);
            this.savedValue = savedValue;
            int width = Math.min(MAX_WIDTH, dim.width() / 2);
            editor = new EditBox(Minecraft.getInstance().font, dim.getLimitX() - width - 3, dim.y() + 1,
                    width, dim.height() - 2, option.getName());
            editor.setMaxLength(MAX_LENGTH);
            editor.setHint(EXAMPLE);
            visibleOptionValue = option.getValue();
            editor.setValue(visibleOptionValue);
            editor.moveCursorToStart();
            editor.setResponder(this::edit);
        }

        private void edit(String value) {
            invalid = AtmosphereSettings.parseWindColors(value) == null;
            visibleOptionValue = invalid ? savedValue.get() : value;
            option.setValue(visibleOptionValue);
            editor.setTextColor(invalid ? 0xFF7777 : EditBox.DEFAULT_TEXT_COLOR);
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            super.render(graphics, mouseX, mouseY, partialTick);
            String optionValue = option.getValue();
            if (!optionValue.equals(visibleOptionValue)) {
                editor.setValue(optionValue);
                editor.moveCursorToStart();
            }
            editor.setEditable(option.isAvailable());
            editor.render(graphics, mouseX, mouseY, partialTick);
        }

        @Override
        public boolean mouseClicked(double x, double y, int button) {
            return option.isAvailable() && editor.mouseClicked(x, y, button);
        }

        @Override
        public boolean keyPressed(int key, int scanCode, int modifiers) {
            return option.isAvailable() && editor.keyPressed(key, scanCode, modifiers);
        }

        @Override
        public boolean charTyped(char character, int modifiers) {
            return option.isAvailable() && editor.charTyped(character, modifiers);
        }

        @Override
        public void setFocused(boolean focused) {
            super.setFocused(focused);
            editor.setFocused(focused && option.isAvailable());
        }

        @Override
        public void updateNarration(NarrationElementOutput output) {
            editor.updateNarration(output);
        }
    }
}
