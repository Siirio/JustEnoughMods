package com.siirio.jemlightingsandbox.terrain;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import com.siirio.jemlightingsandbox.LightingSettings;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryStack;
import org.slf4j.Logger;

/** Makes opaque color and depth available to the native translucent water draw. */
public final class WaterSurface {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int COLOR_UNIT = GL13.GL_TEXTURE0 + 10;
    private static final int DEPTH_UNIT = GL13.GL_TEXTURE0 + 11;
    private static final Map<Integer, Uniforms> UNIFORMS = new HashMap<>();
    private static TextureTarget scene;
    private static int previousColor;
    private static int previousDepth;
    private static boolean bound;
    private static boolean failed;

    private WaterSurface() {
    }

    public static void prepare(int program, Minecraft client, boolean translucent) {
        Uniforms uniforms = UNIFORMS.computeIfAbsent(program, Uniforms::new);
        if (uniforms.active < 0) {
            return;
        }
        GL20.glUniform1i(uniforms.color, COLOR_UNIT - GL13.GL_TEXTURE0);
        GL20.glUniform1i(uniforms.depth, DEPTH_UNIT - GL13.GL_TEXTURE0);
        GL20.glUniform1i(uniforms.active, 0);
        if (!translucent || failed || !LightingSettings.atmosphereEnabled() || client.level == null
                || !client.gameRenderer.getMainCamera().isInitialized()) {
            return;
        }

        RenderTarget main = client.getMainRenderTarget();
        if (!main.useDepth || main.frameBufferId <= 0 || main.width < 2 || main.height < 2
                || main.viewWidth != main.width || main.viewHeight != main.height
                || GL11.glGetInteger(GL20.GL_MAX_TEXTURE_IMAGE_UNITS) <= DEPTH_UNIT - GL13.GL_TEXTURE0) {
            return;
        }

        try {
            capture(main);
            bind();
            var camera = client.gameRenderer.getMainCamera();
            float partialTick = client.getFrameTime();
            Matrix3f view = new Matrix3f(RenderSystem.getInverseViewRotationMatrix()).transpose();
            Matrix4f viewProjection = new Matrix4f(RenderSystem.getProjectionMatrix()).mul(new Matrix4f().set(view));
            try (MemoryStack stack = MemoryStack.stackPush()) {
                GL20.glUniformMatrix4fv(uniforms.matrix, false, viewProjection.get(stack.mallocFloat(16)));
            }
            var sky = client.level.getSkyColor(camera.getPosition(), partialTick);
            GL20.glUniform3f(uniforms.sky, (float) sky.x, (float) sky.y, (float) sky.z);
            GL20.glUniform1f(uniforms.time, ((client.level.getGameTime() % 81920L) + partialTick) / 20F);
            var position = camera.getPosition();
            GL20.glUniform2f(GL20.glGetUniformLocation(program, "jemWaterOrigin"),
                    (float) (position.x - Math.floor(position.x / 128.0) * 128.0),
                    (float) (position.z - Math.floor(position.z / 128.0) * 128.0));
            GL20.glUniform2f(uniforms.viewport, main.width, main.height);
            GL20.glUniform1i(uniforms.active, 1);
        } catch (RuntimeException exception) {
            end();
            failed = true;
            LOGGER.warn("Could not prepare JEM water surface", exception);
        }
    }

    private static void capture(RenderTarget main) {
        int draw = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        int read = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        boolean scissor = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        try {
            if (scene == null || scene.width != main.width || scene.height != main.height
                    || scene.isStencilEnabled() != main.isStencilEnabled()) {
                resize(main);
            }
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, main.frameBufferId);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, scene.frameBufferId);
            GL30.glBlitFramebuffer(0, 0, main.width, main.height, 0, 0, scene.width, scene.height,
                    GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT, GL11.GL_NEAREST);
        } finally {
            if (scissor) {
                GL11.glEnable(GL11.GL_SCISSOR_TEST);
            }
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, read);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, draw);
        }
    }

    private static void resize(RenderTarget main) {
        int active = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        int texture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        boolean depthTest = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            var viewport = stack.mallocInt(4);
            var clearColor = stack.mallocFloat(4);
            var clearDepth = stack.mallocDouble(1);
            GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
            GL11.glGetFloatv(GL11.GL_COLOR_CLEAR_VALUE, clearColor);
            GL11.glGetDoublev(GL11.GL_DEPTH_CLEAR_VALUE, clearDepth);
            try {
                destroyScene();
                scene = new TextureTarget(main.width, main.height, true, Minecraft.ON_OSX);
                if (main.isStencilEnabled()) {
                    scene.enableStencil();
                }
            } finally {
                GlStateManager._activeTexture(active);
                GlStateManager._bindTexture(texture);
                if (!depthTest) {
                    RenderSystem.disableDepthTest();
                }
                RenderSystem.viewport(viewport.get(0), viewport.get(1), viewport.get(2), viewport.get(3));
                GL11.glClearColor(clearColor.get(0), clearColor.get(1), clearColor.get(2), clearColor.get(3));
                GL11.glClearDepth(clearDepth.get(0));
            }
        }
    }

    private static void bind() {
        int active = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        try {
            GL13.glActiveTexture(COLOR_UNIT);
            previousColor = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
            GL13.glActiveTexture(DEPTH_UNIT);
            previousDepth = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
            bound = true;
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, scene.getDepthTextureId());
            GL13.glActiveTexture(COLOR_UNIT);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, scene.getColorTextureId());
        } finally {
            GL13.glActiveTexture(active);
        }
    }

    public static void end() {
        if (!bound) {
            return;
        }
        int active = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        try {
            GL13.glActiveTexture(COLOR_UNIT);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, previousColor);
            GL13.glActiveTexture(DEPTH_UNIT);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, previousDepth);
        } finally {
            GL13.glActiveTexture(active);
            bound = false;
        }
    }

    public static void clearProgramBindings() {
        UNIFORMS.clear();
    }

    public static void release() {
        end();
        failed = false;
        if (scene == null) {
            return;
        }
        int draw = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        int read = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        int binding = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        try {
            destroyScene();
        } finally {
            GlStateManager._bindTexture(binding);
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, read);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, draw);
        }
    }

    private static void destroyScene() {
        if (scene != null) {
            scene.destroyBuffers();
            scene = null;
        }
    }

    private record Uniforms(int active, int color, int depth, int matrix, int time, int sky, int viewport) {
        private Uniforms(int program) {
            this(GL20.glGetUniformLocation(program, "jemWaterActive"),
                    GL20.glGetUniformLocation(program, "jemWaterScene"),
                    GL20.glGetUniformLocation(program, "jemWaterDepth"),
                    GL20.glGetUniformLocation(program, "jemWaterMatrix"),
                    GL20.glGetUniformLocation(program, "jemWaterTime"),
                    GL20.glGetUniformLocation(program, "jemWaterSky"),
                    GL20.glGetUniformLocation(program, "jemWaterViewport"));
        }
    }
}
