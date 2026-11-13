package com.siirio.jemlightingsandbox;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.network.chat.Component;

final class LightingScreen extends Screen {
    private static final int MARGIN = 8;
    private static final int CONTROL_HEIGHT = 20;
    private static final int SCENE_TOP = 26;
    private static final float FULL_TURN = (float) (Math.PI * 2.0);
    private boolean redEnabled = true;
    private boolean blueEnabled = true;
    private boolean sunEnabled = true;
    private float orbit = 0.45F;

    LightingScreen() {
        super(Component.translatable("screen.jem_lighting_sandbox.title"));
    }

    @Override
    protected void init() {
        int buttonWidth = Math.min(108, (width - 2 * MARGIN - 8) / 3);
        int left = (width - 3 * buttonWidth - 8) / 2;
        int top = height - MARGIN - CONTROL_HEIGHT;
        addRenderableWidget(CycleButton.onOffBuilder(redEnabled).create(left, top, buttonWidth, CONTROL_HEIGHT,
                Component.translatable("screen.jem_lighting_sandbox.red"), (button, enabled) -> redEnabled = enabled));
        addRenderableWidget(CycleButton.onOffBuilder(blueEnabled).create(left + buttonWidth + 4, top, buttonWidth,
                CONTROL_HEIGHT, Component.translatable("screen.jem_lighting_sandbox.blue"),
                (button, enabled) -> blueEnabled = enabled));
        addRenderableWidget(CycleButton.onOffBuilder(sunEnabled).create(left + 2 * buttonWidth + 8, top, buttonWidth,
                CONTROL_HEIGHT, Component.translatable("screen.jem_lighting_sandbox.sun"),
                (button, enabled) -> sunEnabled = enabled));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xFF15171C);
        int bottom = height - 2 * MARGIN - CONTROL_HEIGHT;
        ShaderInstance shader = LightingClient.sceneShader();
        if (shader != null && bottom > SCENE_TOP && width > 2 * MARGIN) {
            renderScene(graphics, shader, bottom);
        } else {
            graphics.drawCenteredString(font, Component.translatable("screen.jem_lighting_sandbox.unavailable"),
                    width / 2, height / 2, 0xFFFFFFFF);
        }
        graphics.drawCenteredString(font, title, width / 2, MARGIN, 0xFFFFFFFF);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderScene(GuiGraphics graphics, ShaderInstance shader, int bottom) {
        int right = width - MARGIN;
        graphics.flush();
        ShaderInstance previous = RenderSystem.getShader();
        RenderSystem.setShader(() -> shader);
        try {
            shader.safeGetUniform("Aspect").set((float) (right - MARGIN) / (bottom - SCENE_TOP));
            shader.safeGetUniform("Orbit").set(orbit);
            shader.safeGetUniform("RedEnabled").set(redEnabled ? 1.0F : 0.0F);
            shader.safeGetUniform("BlueEnabled").set(blueEnabled ? 1.0F : 0.0F);
            shader.safeGetUniform("SunEnabled").set(sunEnabled ? 1.0F : 0.0F);
            var matrix = graphics.pose().last().pose();
            var buffer = Tesselator.getInstance().getBuilder();
            buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
            buffer.vertex(matrix, MARGIN, SCENE_TOP, 0).uv(0, 0).endVertex();
            buffer.vertex(matrix, MARGIN, bottom, 0).uv(0, 1).endVertex();
            buffer.vertex(matrix, right, bottom, 0).uv(1, 1).endVertex();
            buffer.vertex(matrix, right, SCENE_TOP, 0).uv(1, 0).endVertex();
            BufferUploader.drawWithShader(buffer.end());
        } finally {
            RenderSystem.setShader(() -> previous);
        }
    }

    @Override
    public boolean mouseDragged(double x, double y, int button, double dragX, double dragY) {
        int bottom = height - 2 * MARGIN - CONTROL_HEIGHT;
        if (button == 0 && x >= MARGIN && x < width - MARGIN && y >= SCENE_TOP && y < bottom) {
            orbit = (orbit + (float) dragX * 0.012F) % FULL_TURN;
            return true;
        }
        return super.mouseDragged(x, y, button, dragX, dragY);
    }
}
