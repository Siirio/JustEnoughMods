package com.siirio.jembuilding.client;

import com.siirio.jembuilding.blueprint.BlueprintMode;
import com.siirio.jembuilding.network.BuildingNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;

public final class BlueprintCaptureScreen extends Screen {
    private final BlockPos second;
    private final InteractionHand hand;

    public BlueprintCaptureScreen(BlockPos second, InteractionHand hand) {
        super(Component.translatable("screen.jem_building.blueprint.capture"));
        this.second = second.immutable();
        this.hand = hand;
    }

    @Override
    protected void init() {
        int x = width / 2 - 102;
        int y = height / 2 - 10;
        addRenderableWidget(Button.builder(Component.translatable("screen.jem_building.blueprint.mode.copy"),
                button -> choose(BlueprintMode.COPY)).bounds(x, y, 100, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.jem_building.blueprint.mode.move"),
                button -> choose(BlueprintMode.MOVE)).bounds(x + 104, y, 100, 20).build());
    }

    private void choose(BlueprintMode mode) {
        BuildingNetwork.captureBlueprint(hand, second, mode);
        BlueprintInput.clearPendingFirst();
        onClose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, height / 2 - 38, 0xFFFFFF);
        graphics.drawCenteredString(font, Component.translatable("screen.jem_building.blueprint.capture_hint"), width / 2, height / 2 - 25, 0xA0A0A0);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
