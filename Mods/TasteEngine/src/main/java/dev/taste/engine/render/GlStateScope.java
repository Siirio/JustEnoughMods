package dev.taste.engine.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import java.nio.IntBuffer;
import java.nio.ByteBuffer;
import net.minecraft.client.renderer.ShaderInstance;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

/** Restores the shared state touched by the integrated fullscreen pass. */
public final class GlStateScope implements AutoCloseable {
    private final int readFramebuffer;
    private final int drawFramebuffer;
    private final int program;
    private final ShaderInstance shader;
    private final int vertexArray;
    private final int activeTexture;
    private final int[] texture2d = new int[16];
    private final int[] viewport = new int[4];
    private final int[] scissorBox = new int[4];
    private final boolean depth;
    private final boolean blend;
    private final boolean cull;
    private final boolean scissor;
    private final boolean framebufferSrgb;
    private final boolean depthMask;
    private final boolean[] colorMask = new boolean[4];
    private final int frontFace;
    private final int blendSourceRgb;
    private final int blendDestinationRgb;
    private final int blendSourceAlpha;
    private final int blendDestinationAlpha;
    private final int blendEquationRgb;
    private final int blendEquationAlpha;

    private GlStateScope() {
        readFramebuffer = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        drawFramebuffer = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        program = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        shader = RenderSystem.getShader();
        vertexArray = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        for (int unit = 0; unit < texture2d.length; unit++) {
            GL13.glActiveTexture(GL13.GL_TEXTURE0 + unit);
            texture2d[unit] = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        }
        GL13.glActiveTexture(activeTexture);
        IntBuffer values = BufferUtils.createIntBuffer(4);
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, values);
        values.get(viewport);
        values.clear();
        GL11.glGetIntegerv(GL11.GL_SCISSOR_BOX, values);
        values.get(scissorBox);
        ByteBuffer masks = BufferUtils.createByteBuffer(4);
        GL11.glGetBooleanv(GL11.GL_COLOR_WRITEMASK, masks);
        for (int index = 0; index < colorMask.length; index++) colorMask[index] = masks.get(index) != 0;
        depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        blend = GL11.glIsEnabled(GL11.GL_BLEND);
        cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        scissor = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        framebufferSrgb = GL11.glIsEnabled(GL30.GL_FRAMEBUFFER_SRGB);
        depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        frontFace = GL11.glGetInteger(GL11.GL_FRONT_FACE);
        blendSourceRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB);
        blendDestinationRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
        blendSourceAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA);
        blendDestinationAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
        blendEquationRgb = GL11.glGetInteger(GL20.GL_BLEND_EQUATION_RGB);
        blendEquationAlpha = GL11.glGetInteger(GL20.GL_BLEND_EQUATION_ALPHA);
    }

    public static GlStateScope capture() {
        RenderSystem.assertOnRenderThread();
        return new GlStateScope();
    }

    @Override
    public void close() {
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, readFramebuffer);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, drawFramebuffer);
        RenderSystem.viewport(viewport[0], viewport[1], viewport[2], viewport[3]);
        RenderSystem.setShader(() -> shader);
        GlStateManager._glUseProgram(program);
        GlStateManager._glBindVertexArray(vertexArray);
        // BufferUploader caches the last immediate VertexBuffer separately from OpenGL.
        // Restoring an earlier VAO without invalidating that cache makes the next upload
        // skip its bind and draw against VAO 0.
        BufferUploader.reset();
        for (int unit = 0; unit < texture2d.length; unit++) {
            RenderSystem.activeTexture(GL13.GL_TEXTURE0 + unit);
            RenderSystem.bindTexture(texture2d[unit]);
        }
        RenderSystem.activeTexture(activeTexture);
        if (depth) RenderSystem.enableDepthTest();
        else RenderSystem.disableDepthTest();
        if (blend) RenderSystem.enableBlend();
        else RenderSystem.disableBlend();
        if (cull) RenderSystem.enableCull();
        else RenderSystem.disableCull();
        if (scissor) RenderSystem.enableScissor(scissorBox[0], scissorBox[1], scissorBox[2], scissorBox[3]);
        else RenderSystem.disableScissor();
        set(GL30.GL_FRAMEBUFFER_SRGB, framebufferSrgb);
        RenderSystem.depthMask(depthMask);
        RenderSystem.colorMask(colorMask[0], colorMask[1], colorMask[2], colorMask[3]);
        GL11.glFrontFace(frontFace);
        RenderSystem.blendFuncSeparate(blendSourceRgb, blendDestinationRgb, blendSourceAlpha, blendDestinationAlpha);
        if (blendEquationRgb == blendEquationAlpha) RenderSystem.blendEquation(blendEquationRgb);
        else GL20.glBlendEquationSeparate(blendEquationRgb, blendEquationAlpha);
    }

    private static void set(int capability, boolean enabled) {
        if (enabled) GL11.glEnable(capability);
        else GL11.glDisable(capability);
    }
}
