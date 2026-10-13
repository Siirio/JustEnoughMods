package com.siirio.jemserver.client.xaero;

import com.siirio.jemserver.MapNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class ProjectNameScreen extends Screen {
    private final Screen parent; private final ResourceLocation dimension; private final int x1, z1, x2, z2; private EditBox name;
    public ProjectNameScreen(Screen parent, ResourceLocation dimension, int x1, int z1, int x2, int z2) {
        super(Component.translatable("gui.jem_server.project_name")); this.parent = parent; this.dimension = dimension; this.x1 = x1; this.z1 = z1; this.x2 = x2; this.z2 = z2;
    }
    @Override protected void init() {
        name = new EditBox(font, width / 2 - 110, height / 2 - 18, 220, 20, title); name.setMaxLength(48); addRenderableWidget(name);
        addRenderableWidget(Button.builder(Component.translatable("gui.jem_server.project_create"), button -> {
            String value = name.getValue().strip(); if (value.length() >= 3) { MapNetwork.createProject(value, dimension, x1, z1, x2, z2); minecraft.setScreen(parent); }
        }).bounds(width / 2 - 110, height / 2 + 12, 106, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), button -> minecraft.setScreen(parent)).bounds(width / 2 + 4, height / 2 + 12, 106, 20).build());
    }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) { renderBackground(graphics); graphics.drawCenteredString(font, title, width / 2, height / 2 - 42, 0xFFFFE2B8); super.render(graphics, mouseX, mouseY, partialTick); }
}
