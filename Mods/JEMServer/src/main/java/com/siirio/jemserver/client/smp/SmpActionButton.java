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
    private final Consumer<SmpActionButton> action;

    public SmpActionButton(int x, int y, int width, int height, Component label, String role, Consumer<SmpActionButton> action) {
        super(x, y, width, height, label);
        this.role = role;
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
        graphics.drawCenteredString(Minecraft.getInstance().font, getMessage(), getX() + width / 2, getY() + (height - 8) / 2, color);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, createNarrationMessage());
    }
}
