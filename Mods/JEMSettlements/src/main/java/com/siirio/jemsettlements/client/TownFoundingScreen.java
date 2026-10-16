package com.siirio.jemsettlements.client;

import com.siirio.jemsettlements.network.SettlementNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

public final class TownFoundingScreen extends Screen {
    private final BlockPos bell;
    private final long foundingCost;
    private EditBox name;
    public TownFoundingScreen(CompoundTag view) { super(Component.translatable("jem.settlements.found.title")); bell = BlockPos.of(view.getLong("Bell")); foundingCost = view.getLong("FoundingCost"); }
    @Override protected void init() {
        int x = width / 2 - 120, y = height / 2 - 35;
        name = new EditBox(font, x, y, 240, 20, Component.translatable("jem.settlements.found.name"));
        name.setMaxLength(32); name.setHint(Component.translatable("jem.settlements.found.name")); addRenderableWidget(name);
        addRenderableWidget(Button.builder(Component.translatable("jem.settlements.found.confirm"), button -> {
            String value = name.getValue().strip(); if (value.length() >= 3) { SettlementNetwork.found(value, bell); minecraft.setScreen(null); }
        }).bounds(x, y + 30, 116, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), button -> minecraft.setScreen(null)).bounds(x + 124, y + 30, 116, 20).build());
    }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics); graphics.drawCenteredString(font, title, width / 2, height / 2 - 70, 0xFFFFE3BB);
        graphics.drawCenteredString(font, Component.translatable("jem.settlements.found.help"), width / 2, height / 2 - 54, 0xFFCBB99E);
        com.siirio.jemeconomy.client.CoinPriceRenderer.draw(graphics, font, foundingCost, width / 2 - 60, height / 2 + 26, 120, 0xFFFFE3BB);
        super.render(graphics, mouseX, mouseY, partialTick);
    }
}
