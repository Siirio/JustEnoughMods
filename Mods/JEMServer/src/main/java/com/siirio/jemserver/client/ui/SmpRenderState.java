package com.siirio.jemserver.client.ui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;

public final class SmpRenderState {
    public static void restoreMainTarget() {
        var minecraft=Minecraft.getInstance();
        minecraft.getMainRenderTarget().bindWrite(true);
        RenderSystem.viewport(0,0,minecraft.getWindow().getWidth(),minecraft.getWindow().getHeight());
        RenderSystem.setShaderColor(1,1,1,1);
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableBlend();
    }

    public static void restoreAfterScreen() {
        RenderSystem.disableScissor();
        restoreMainTarget();
    }

    private SmpRenderState() {}
}
