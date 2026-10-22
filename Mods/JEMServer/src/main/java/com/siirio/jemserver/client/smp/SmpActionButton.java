package com.siirio.jemserver.client.smp;

import static com.siirio.jemserver.client.ui.JemPalette.CREAM;
import static com.siirio.jemserver.client.ui.JemPalette.MUTED;
import static com.siirio.jemserver.client.ui.JemPalette.OUTLINE;

import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

public final class SmpActionButton extends AbstractButton {
    private final String role;
    private final String icon;
    private final Consumer<SmpActionButton> action;

    public SmpActionButton(int x, int y, int width, int height, Component label, String role, Consumer<SmpActionButton> action) {
        this(x, y, width, height, label, role, "", action);
    }

    public SmpActionButton(int x, int y, int width, int height, Component label, String role, String icon,
                           Consumer<SmpActionButton> action) {
        super(x, y, width, height, label);
        this.role = role;
        this.icon = icon;
        this.action = action;
    }

    @Override
    public void onPress() {
        action.accept(this);
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        SmpGuiAssets.button(graphics, getX(), getY(), width, height, role, isHoveredOrFocused(), !active);
        int color = !active ? MUTED : role.equals("primary") ? OUTLINE : CREAM;
        var font = Minecraft.getInstance().font;
        if (icon.isEmpty()) {
            graphics.drawCenteredString(font, getMessage(), getX() + width / 2, getY() + (height - 8) / 2, color);
            return;
        }
        boolean hasIcon = !icon.isEmpty() && width >= 70 && height >= 20 && SmpIcons.item(icon) != net.minecraft.world.item.Items.AIR;
        int available = width - (hasIcon ? 30 : 12);
        String text = getMessage().getString();
        int labelWidth = Math.min(font.width(text), available);
        int contentX = getX() + Math.max(6, (width - labelWidth - (hasIcon ? 20 : 0)) / 2);
        if (hasIcon) SmpIcons.draw(graphics, icon, contentX, getY() + (height - 16) / 2, 16);
        if (font.width(text) > available) text = font.plainSubstrByWidth(text, Math.max(0, available - 8)) + "\u2026";
        graphics.drawString(font, text, contentX + (hasIcon ? 20 : 0), getY() + (height - 9) / 2, color, false);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, createNarrationMessage());
    }
}
