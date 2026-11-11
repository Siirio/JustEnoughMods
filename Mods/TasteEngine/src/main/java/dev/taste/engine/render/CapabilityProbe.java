package dev.taste.engine.render;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.LinkedHashSet;
import java.util.Set;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

/** Queries the active context. It never infers support from a reported version alone. */
public final class CapabilityProbe {
    public record Result(boolean baseline, int drawBuffers, int textureUnits, Set<String> flags, String reason) {
        public Result { flags = Set.copyOf(flags); }
    }

    private CapabilityProbe() { }

    public static Result probe() {
        RenderSystem.assertOnRenderThread();
        Set<String> flags = new LinkedHashSet<>();
        if (!GL.getCapabilities().OpenGL33) {
            return new Result(false, 0, 0, flags, "OpenGL 3.3 is required");
        }
        int drawBuffers = GL11.glGetInteger(GL20.GL_MAX_DRAW_BUFFERS);
        int colorAttachments = GL11.glGetInteger(GL30.GL_MAX_COLOR_ATTACHMENTS);
        int textureUnits = GL11.glGetInteger(GL20.GL_MAX_TEXTURE_IMAGE_UNITS);
        boolean framebufferFormats = drawBuffers >= 4 && colorAttachments >= 4 && probeFourTargetFloatFramebuffer();
        if (framebufferFormats) flags.add("rgba16f-mrt");
        if (GL.getCapabilities().OpenGL43) flags.add("gl43");
        flags.add("post");
        boolean baseline = framebufferFormats && textureUnits >= 16;
        String reason = baseline ? "" : "Taste requires a complete four-target RGBA16F framebuffer and sixteen fragment samplers";
        return new Result(baseline, drawBuffers, textureUnits, flags, reason);
    }

    private static boolean probeFourTargetFloatFramebuffer() {
        int priorFramebuffer = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
        int priorTexture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        int framebuffer = GL30.glGenFramebuffers();
        int[] textures = new int[4];
        try {
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, framebuffer);
            IntBuffer attachments = BufferUtils.createIntBuffer(4);
            for (int index = 0; index < textures.length; index++) {
                textures[index] = GL11.glGenTextures();
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, textures[index]);
                GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_RGBA16F, 2, 2, 0,
                        GL11.GL_RGBA, GL11.GL_FLOAT, (ByteBuffer) null);
                GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0 + index,
                        GL11.GL_TEXTURE_2D, textures[index], 0);
                attachments.put(GL30.GL_COLOR_ATTACHMENT0 + index);
            }
            attachments.flip();
            GL20.glDrawBuffers(attachments);
            return GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER) == GL30.GL_FRAMEBUFFER_COMPLETE;
        } finally {
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, priorFramebuffer);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, priorTexture);
            for (int texture : textures) if (texture != 0) GL11.glDeleteTextures(texture);
            GL30.glDeleteFramebuffers(framebuffer);
        }
    }
}
