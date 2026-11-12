package dev.taste.engine.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import java.io.IOException;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

final class HdrLayerCompositor implements AutoCloseable {
    static final String TARGET = "fabulous-resolve";
    private ShaderInstance shader;

    void prepare() throws IOException {
        if (shader == null) shader = new ShaderInstance(Minecraft.getInstance().getResourceManager(),
                new ResourceLocation("taste", "taste_fabulous_resolve"), DefaultVertexFormat.POSITION_TEX);
    }

    void compose(RenderTarget world, RenderTarget output, Map<FabulousLayerRouter.Layer, RenderTarget> layers, Runnable draw) {
        try (GlStateScope ignored = GlStateScope.capture()) {
            output.bindWrite(true);
            RenderSystem.disableScissor();
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.disableCull();
            RenderSystem.disableBlend();
            RenderSystem.colorMask(true, true, true, true);
            GL11.glDisable(GL30.GL_FRAMEBUFFER_SRGB);
            RenderSystem.setShader(() -> shader);
            shader.setSampler("WorldColor", world.getColorTextureId());
            shader.setSampler("WorldDepth", world.getDepthTextureId());
            layers.forEach((layer, target) -> {
                shader.setSampler(layer.sampler + "Color", target.getColorTextureId());
                shader.setSampler(layer.sampler + "Depth", target.getDepthTextureId());
            });
            try {
                draw.run();
            } finally {
                shader.clear();
            }
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, output.frameBufferId);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, world.frameBufferId);
            GL30.glBlitFramebuffer(0, 0, world.width, world.height, 0, 0, world.width, world.height,
                    GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);
        }
    }

    @Override public void close() {
        if (shader != null) shader.close();
        shader = null;
    }
}
