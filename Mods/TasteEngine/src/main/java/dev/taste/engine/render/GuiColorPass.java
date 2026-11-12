package dev.taste.engine.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import java.io.IOException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

/** Applies optional color assistance after native destination-dependent GUI blends have finished. */
public final class GuiColorPass implements AutoCloseable {
    private static final String CAPTURE = "composed-gui";
    private ShaderInstance shader;

    public static boolean enabled(PipelineParameters config) {
        return !config.value("accessibility.uiUnaffected").getAsBoolean()
                && !config.value("accessibility.colorVision").getAsString().equals("none");
    }

    public void render(RenderTarget nativeTarget, TargetPool targets, PipelineParameters config, Runnable draw) throws IOException {
        if (!enabled(config)) {
            targets.release(CAPTURE);
            return;
        }
        if (shader == null) shader = new ShaderInstance(Minecraft.getInstance().getResourceManager(),
                new ResourceLocation("taste", "taste_gui_color"), DefaultVertexFormat.POSITION_TEX);
        TextureTarget capture = targets.acquire(new TargetDescriptor(CAPTURE, nativeTarget.width, nativeTarget.height,
                TargetDescriptor.Format.RGBA8, false, 1, 1, 1));
        RenderSystem.disableScissor();
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, nativeTarget.frameBufferId);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, capture.frameBufferId);
        GL30.glBlitFramebuffer(0, 0, nativeTarget.width, nativeTarget.height, 0, 0, capture.width, capture.height,
                GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);
        nativeTarget.bindWrite(true);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.disableBlend();
        RenderSystem.colorMask(true, true, true, true);
        GL11.glDisable(GL30.GL_FRAMEBUFFER_SRGB);
        RenderSystem.setShader(() -> shader);
        shader.setSampler("DisplaySampler", capture.getColorTextureId());
        shader.getUniform("ColorVision").set(switch (config.value("accessibility.colorVision").getAsString()) {
            case "protan" -> 1F;
            case "deutan" -> 2F;
            case "tritan" -> 3F;
            default -> 0F;
        });
        try {
            draw.run();
        } finally {
            shader.clear();
        }
    }

    @Override public void close() {
        if (shader != null) {
            shader.close();
            shader = null;
        }
    }
}
