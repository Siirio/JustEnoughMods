package dev.taste.engine.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import java.io.IOException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

public final class DebugViewPass implements AutoCloseable {
    public enum View {
        NONE, DEPTH, NORMALS, ALBEDO, EMISSION, VELOCITY, SHADOWS, MATERIALS, REACTIVE, GI, EXPOSURE
    }
    private ShaderInstance shader;

    public void render(RenderTarget output, View view, int input, int secondary, int fallback, FrameSnapshot frame, Runnable draw)
            throws IOException {
        if (shader == null) shader = new ShaderInstance(Minecraft.getInstance().getResourceManager(),
                new ResourceLocation("taste", "taste_debug_view"), DefaultVertexFormat.POSITION_TEX);
        output.bindWrite(true);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.disableScissor();
        RenderSystem.disableBlend();
        RenderSystem.colorMask(true, true, true, true);
        GL11.glDisable(GL30.GL_FRAMEBUFFER_SRGB);
        RenderSystem.setShader(() -> shader);
        shader.setSampler("DataSampler", input != 0 ? input : fallback);
        shader.setSampler("SecondarySampler", secondary != 0 ? secondary : input != 0 ? input : fallback);
        shader.getUniform("Mode").set(input == 0 ? 0F : (float) view.ordinal());
        shader.getUniform("InverseProjection").set(new Matrix4f().set(frame.inverseProjection().columnMajor()));
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
