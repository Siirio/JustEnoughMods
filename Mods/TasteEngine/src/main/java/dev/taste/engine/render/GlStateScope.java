package dev.taste.engine.render;

import com.mojang.blaze3d.systems.RenderSystem;
import java.nio.IntBuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

/** Restores the shared state touched by the integrated fullscreen pass. */
public final class GlStateScope implements AutoCloseable {
    private final int readFramebuffer;
    private final int drawFramebuffer;
    private final int program;
    private final int vertexArray;
    private final int activeTexture;
    private final int texture2d;
    private final int[] viewport = new int[4];
    private final boolean depth;
    private final boolean blend;
    private final boolean cull;
    private final boolean depthMask;

    private GlStateScope() {
        readFramebuffer = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        drawFramebuffer = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        program = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        vertexArray = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        texture2d = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        IntBuffer values = BufferUtils.createIntBuffer(4);
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, values);
        values.get(viewport);
        depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        blend = GL11.glIsEnabled(GL11.GL_BLEND);
        cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
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
        GL13.glActiveTexture(activeTexture);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture2d);
        set(GL11.GL_DEPTH_TEST, depth);
        set(GL11.GL_BLEND, blend);
        set(GL11.GL_CULL_FACE, cull);
        GL11.glDepthMask(depthMask);
    }

    private static void set(int capability, boolean enabled) {
        if (enabled) GL11.glEnable(capability);
        else GL11.glDisable(capability);
    }
}
