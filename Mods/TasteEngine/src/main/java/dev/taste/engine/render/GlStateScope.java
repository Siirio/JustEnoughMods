package dev.taste.engine.render;

import com.mojang.blaze3d.systems.RenderSystem;
import java.nio.IntBuffer;
import java.nio.ByteBuffer;
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
        GL11.glViewport(viewport[0], viewport[1], viewport[2], viewport[3]);
        GL20.glUseProgram(program);
        GL30.glBindVertexArray(vertexArray);
        for (int unit = 0; unit < texture2d.length; unit++) {
            GL13.glActiveTexture(GL13.GL_TEXTURE0 + unit);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture2d[unit]);
        }
        GL13.glActiveTexture(activeTexture);
        set(GL11.GL_DEPTH_TEST, depth);
        set(GL11.GL_BLEND, blend);
        set(GL11.GL_CULL_FACE, cull);
        set(GL11.GL_SCISSOR_TEST, scissor);
        set(GL30.GL_FRAMEBUFFER_SRGB, framebufferSrgb);
        GL11.glDepthMask(depthMask);
        GL11.glColorMask(colorMask[0], colorMask[1], colorMask[2], colorMask[3]);
        GL11.glFrontFace(frontFace);
        GL14.glBlendFuncSeparate(blendSourceRgb, blendDestinationRgb, blendSourceAlpha, blendDestinationAlpha);
        GL20.glBlendEquationSeparate(blendEquationRgb, blendEquationAlpha);
        GL11.glScissor(scissorBox[0], scissorBox[1], scissorBox[2], scissorBox[3]);
    }

    private static void set(int capability, boolean enabled) {
        if (enabled) GL11.glEnable(capability);
        else GL11.glDisable(capability);
    }
}
