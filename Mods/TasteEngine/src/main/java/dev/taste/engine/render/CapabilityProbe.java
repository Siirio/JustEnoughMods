package dev.taste.engine.render;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.LinkedHashSet;
import java.util.Set;
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
        if (GL.getCapabilities().OpenGL43) flags.add("gl43");
        flags.add("post");
        boolean baseline = drawBuffers >= 4 && colorAttachments >= 4 && textureUnits >= 16;
        String reason = baseline ? "" : "Taste requires four draw buffers, four color attachments and sixteen fragment samplers";
        return new Result(baseline, drawBuffers, textureUnits, flags, reason);
    }
}
