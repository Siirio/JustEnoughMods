package com.siirio.jemserver.client.smp;

import com.siirio.jemserver.smp.events.EventNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import static com.siirio.jemserver.client.ui.JemPalette.*;

public final class EventEntryScreen extends Screen {
    private static final int PANEL_WIDTH = 448;
    private static final int PANEL_HEIGHT = 232;
    private static final int ART_WIDTH = 132;
    private final EventNetwork.Prompt prompt;
    private final EntityPreview bossPreview = new EntityPreview();
    private String mode;
    private boolean groupStep;

    private EventEntryScreen(EventNetwork.Prompt prompt) {
        super(Component.literal(prompt.title()));
        this.prompt = prompt;
        groupStep = !bossPrompt() || !prompt.raidAvailable();
        mode = bossPrompt() ? "BOSS_FIGHT" : prompt.type();
    }

    public static void open(EventNetwork.Prompt prompt) {
        Minecraft.getInstance().setScreen(new EventEntryScreen(prompt));
    }

    @Override
    protected void init() {
        clearWidgets();
        int panelWidth = Math.min(PANEL_WIDTH, width - 24);
        int left = (width - panelWidth) / 2;
        int top = (height - PANEL_HEIGHT) / 2;
        int contentX = left + Math.min(ART_WIDTH, panelWidth / 3) + 18;
        int contentWidth = left + panelWidth - contentX - 14;
        int buttonY = top + PANEL_HEIGHT - 43;
        if (!prompt.state().equals("FREE")) {
            addRenderableWidget(new SmpActionButton(contentX, buttonY, contentWidth, 27,
                    Component.translatable("jem.event.close"), "secondary", button -> onClose()));
            return;
        }
        if (!groupStep) {
            int gap = 8;
            int modeWidth = prompt.raidAvailable() ? (contentWidth - gap) / 2 : contentWidth;
            addRenderableWidget(new SmpActionButton(contentX, buttonY, modeWidth, 27,
                    Component.translatable("jem.event.mode.normal"), "secondary", button -> selectMode("BOSS_FIGHT")));
            if (prompt.raidAvailable()) addRenderableWidget(new SmpActionButton(contentX + modeWidth + gap, buttonY, modeWidth, 27,
                    Component.translatable("jem.event.mode.raid"), "danger", button -> selectMode("BOSS_RAID")));
            return;
        }
        int gap = 8;
        int buttonWidth = (contentWidth - gap) / 2;
        addRenderableWidget(new SmpActionButton(contentX, buttonY, buttonWidth, 27,
                Component.translatable("jem.event.party"), "primary", button -> choose(false)));
        addRenderableWidget(new SmpActionButton(contentX + buttonWidth + gap, buttonY, buttonWidth, 27,
                Component.translatable("jem.event.solo"), "secondary", button -> choose(true)));
    }

    private boolean bossPrompt() {
        return prompt.type().equals("BOSS_STRUCTURE") || prompt.type().equals("BOSS_FIGHT");
    }

    private void selectMode(String selected) {
        mode = selected;
        groupStep = true;
        rebuildWidgets();
    }

    private void choose(boolean solo) {
        minecraft.setScreen(null);
        EventNetwork.select(prompt, mode, solo);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        com.siirio.jemserver.client.ui.SmpRenderState.restoreMainTarget();
        renderBackground(graphics);
        int panelWidth = Math.min(PANEL_WIDTH, width - 24);
        int left = (width - panelWidth) / 2;
        int top = (height - PANEL_HEIGHT) / 2;
        int artWidth = Math.min(ART_WIDTH, panelWidth / 3);
        int accent = SmpGuiAssets.accent(mode.isEmpty() ? prompt.type() : mode);
        SmpGuiAssets.panel(graphics, left, top, panelWidth, PANEL_HEIGHT, PANEL);
        SmpGuiAssets.frame(graphics, left, top, panelWidth, PANEL_HEIGHT, accent);
        graphics.fillGradient(left + 4, top + 4, left + artWidth, top + PANEL_HEIGHT - 4, 0xFF261B18, 0xFF120F0E);
        int previewX = left + 10, previewY = top + 10, previewWidth = artWidth - 18, previewHeight = PANEL_HEIGHT - 20;
        SmpGuiAssets.coverBackground(graphics, mode.isEmpty() ? prompt.type() : mode, previewX, previewY, previewWidth, previewHeight);
        if (prompt.entityType().isBlank())
            SmpIcons.draw(graphics, mode.isEmpty() ? prompt.type() : mode, previewX + (previewWidth - Math.min(previewWidth, previewHeight)) / 2,
                    previewY + (previewHeight - Math.min(previewWidth, previewHeight)) / 2, Math.min(previewWidth, previewHeight));
        else bossPreview.render(graphics, prompt.entityType(), previewX, previewY, previewWidth, previewHeight, mouseX, mouseY);
        graphics.fill(left + artWidth, top + 4, left + artWidth + 1, top + PANEL_HEIGHT - 4, EDGE);
        int contentX = left + artWidth + 18;
        int contentWidth = left + panelWidth - contentX - 14;
        graphics.drawString(font, Component.translatable(groupStep ? "jem.event.group_step" : "jem.event.mode_step"), contentX, top + 16, PEACH, false);
        graphics.drawString(font, font.plainSubstrByWidth(title.getString(), contentWidth), contentX, top + 34, CREAM, false);
        graphics.fill(contentX, top + 51, contentX + Math.min(86, contentWidth), top + 53, accent);
        if (!prompt.state().equals("FREE")) renderBusy(graphics, contentX, top + 66, contentWidth);
        else if (groupStep) renderGroupExplanation(graphics, contentX, top + 66, contentWidth);
        else renderModeExplanation(graphics, contentX, top + 66, contentWidth);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderModeExplanation(GuiGraphics graphics, int x, int y, int width) {
        drawWrapped(graphics, Component.translatable("jem.event.mode_description"), x, y, width, MUTED);
        int cardY = y + 43;
        SmpGuiAssets.panel(graphics, x, cardY, width, 41, ROW);
        graphics.fill(x, cardY, x + 3, cardY + 41, GREEN);
        graphics.drawString(font, Component.translatable("jem.event.mode.normal_hint"), x + 10, cardY + 15, CREAM, false);
        if (prompt.raidAvailable()) {
            graphics.fill(x + width / 2, cardY, x + width / 2 + 1, cardY + 41, EDGE);
            graphics.drawString(font, Component.translatable("jem.event.mode.raid_hint"), x + width / 2 + 9, cardY + 15, PEACH, false);
        }
    }

    private void renderGroupExplanation(GuiGraphics graphics, int x, int y, int width) {
        Component selected = Component.translatable(mode.equals("BOSS_RAID") ? "jem.event.mode.raid" :
                bossPrompt() ? "jem.event.mode.normal" : "jem.smp." + prompt.type());
        graphics.drawString(font, Component.translatable("jem.event.selected_mode", selected), x, y, PEACH, false);
        drawWrapped(graphics, Component.translatable("jem.event.group_description"), x, y + 20, width, MUTED);
        SmpGuiAssets.panel(graphics, x, y + 58, width, 31, INSET);
        graphics.drawString(font, Component.translatable("jem.event.group_hint"), x + 9, y + 69, CREAM, false);
    }

    private void renderBusy(GuiGraphics graphics, int x, int y, int width) {
        Component state = Component.translatable("jem.event.arena_state." + prompt.state().toLowerCase());
        SmpGuiAssets.panel(graphics, x, y, width, 42, ROW);
        graphics.fill(x, y, x + 3, y + 42, RED);
        graphics.drawString(font, state, x + 11, y + 9, CREAM, false);
        graphics.drawString(font, Component.translatable("jem.event.arena_busy_hint"), x + 11, y + 24, MUTED, false);
        if (!prompt.occupants().isEmpty()) {
            graphics.drawString(font, Component.translatable("jem.event.arena_occupants"), x, y + 55, PEACH, false);
            graphics.drawString(font, font.plainSubstrByWidth(String.join(", ", prompt.occupants()), width), x, y + 69, CREAM, false);
        }
    }

    private void drawWrapped(GuiGraphics graphics, Component text, int x, int y, int width, int color) {
        int line = 0;
        for (var value : font.split(text, width)) graphics.drawString(font, value, x, y + line++ * 11, color, false);
    }

    @Override
    public void removed() {
        com.siirio.jemserver.client.ui.SmpRenderState.restoreAfterScreen();
        super.removed();
    }
}
