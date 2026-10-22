package com.siirio.jemworldbosstiers.client;

import com.siirio.jemworldbosstiers.network.AdmissionPromptPacket;
import com.siirio.jemworldbosstiers.network.TierNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class StandaloneEncounterScreen extends Screen {
    private final AdmissionPromptPacket prompt;
    private StandaloneEncounterScreen(AdmissionPromptPacket prompt) { super(prompt.boss()); this.prompt = prompt; }
    public static void open(AdmissionPromptPacket prompt) { Minecraft.getInstance().setScreen(new StandaloneEncounterScreen(prompt)); }

    @Override protected void init() {
        int x = width / 2 - 100;
        var start = addRenderableWidget(Button.builder(Component.translatable("message.jem_world_boss_tiers.encounter.start"), button -> choose(1))
                .bounds(x, height / 2, 200, 20).build());
        start.active = prompt.blocked() == null;
        var raid = addRenderableWidget(Button.builder(Component.translatable("message.jem_world_boss_tiers.encounter.raid"), button -> choose(2))
                .bounds(x, height / 2 + 24, 200, 20).build());
        raid.active = prompt.raid();
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), button -> onClose())
                .bounds(x, height / 2 + 48, 200, 20).build());
    }

    private void choose(int choice) { TierNetwork.chooseAdmission(prompt.token(), choice); minecraft.setScreen(null); }
    @Override public void onClose() { choose(0); }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, height / 2 - 76, 0xFFFFFF);
        Component description = prompt.blocked() == null
                ? Component.translatable("message.jem_world_boss_tiers.encounter.prepare") : prompt.blocked();
        int y = height / 2 - 56;
        for (var line : font.split(description, Math.min(320, width - 32))) {
            graphics.drawCenteredString(font, line, width / 2, y, 0xFFFFFF);
            y += font.lineHeight + 2;
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }
}
