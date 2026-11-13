package com.siirio.jemlightingsandbox;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.logging.LogUtils;
import com.siirio.jemlightingsandbox.terrain.TerrainLighting;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.slf4j.Logger;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FogType;
import net.minecraftforge.client.event.RenderLevelStageEvent;

final class AtmosphereRenderer {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int VOXEL_UNIT = 7;

    private static ShaderInstance scattering;
    private static ShaderInstance composite;
    private static TextureTarget depthCopy;
    private static TextureTarget reduced;
    private static boolean failed;

    private AtmosphereRenderer() {
    }

    static void shaders(ShaderInstance scatterShader, ShaderInstance compositeShader) {
        scattering = scatterShader;
        composite = compositeShader;
        failed = false;
    }

    static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL || scattering == null || composite == null
                || failed || !LightingSettings.atmosphereEnabled()) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        ClientLevel level = client.level;
        Camera camera = event.getCamera();
        if (level == null || !level.dimensionType().hasSkyLight() || level.dimensionType().hasCeiling()
                || camera.getFluidInCamera() != FogType.NONE || hasRestrictedVision(camera)) {
            return;
        }
        var volume = TerrainLighting.currentVolume(level);
        if (volume == null) {
            return;
        }
        RenderTarget main = client.getMainRenderTarget();
        if (main.width < 2 || main.height < 2 || main.width != main.viewWidth || main.height != main.viewHeight
                || !main.useDepth || main.getDepthTextureId() <= 0) {
            return;
        }
        var sun = TerrainLighting.sun(level, event.getPartialTick());
        float weather = level.getRainLevel(event.getPartialTick());
        if (sun.strength() < 0.01F && weather < 0.01F) {
            return;
        }

        var inverseViewProjection = new Matrix4f().set(RenderSystem.getInverseViewRotationMatrix())
                .mul(new Matrix4f(event.getProjectionMatrix()).invert());
        var position = camera.getPosition();
        var sky = level.getSkyColor(position, event.getPartialTick());
        float sunHeight = Mth.clamp(sun.y() * 2.5F, 0F, 1F);
        float lowSun = 1F - sunHeight * sunHeight * (3F - 2F * sunHeight);

        DrawState state = new DrawState();
        try {
            ensureTargets(main);
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
            depthCopy.copyDepthFrom(main);
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.disableCull();
            GL11.glDisable(GL11.GL_STENCIL_TEST);
            GL11.glColorMask(true, true, true, true);

            reduced.bindWrite(true);
            scattering.setSampler("SceneDepth", depthCopy.getDepthTextureId());
            scattering.safeGetUniform("InverseViewProjection").set(inverseViewProjection);
            scattering.safeGetUniform("VolumeOrigin").set((float) (volume.x() - position.x),
                    (float) (volume.y() - position.y), (float) (volume.z() - position.z));
            scattering.safeGetUniform("VolumeSize").set((float) volume.width(), (float) volume.height(), (float) volume.depth());
            scattering.safeGetUniform("SunDirection").set(sun.x(), sun.y(), 0F);
            scattering.safeGetUniform("SunStrength").set(sun.strength());
            scattering.safeGetUniform("SunColor").set(1F, 0.94F - lowSun * 0.12F, 0.81F - lowSun * 0.26F);
            scattering.safeGetUniform("AirColor").set((float) sky.x, (float) sky.y, (float) sky.z);
            scattering.safeGetUniform("CameraAltitude").set((float) position.y - level.getSeaLevel());
            scattering.safeGetUniform("Weather").set(weather);
            drawScattering(volume.id(), volume.shapes());

            main.bindWrite(true);
            composite.setSampler("AtmosphereTex", reduced.getColorTextureId());
            composite.setSampler("SceneDepth", depthCopy.getDepthTextureId());
            composite.safeGetUniform("InverseViewProjection").set(inverseViewProjection);
            composite.apply();
            try {
                GL11.glEnable(GL11.GL_BLEND);
                GL20.glBlendEquationSeparate(GL14.GL_FUNC_ADD, GL14.GL_FUNC_ADD);
                GL14.glBlendFuncSeparate(GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ZERO, GL11.GL_ONE);
                drawQuad();
            } finally {
                composite.clear();
            }
        } catch (RuntimeException exception) {
            failed = true;
            LOGGER.error("Could not render JEM atmosphere", exception);
            releaseTargets();
        } finally {
            state.restore();
        }
    }

    private static boolean hasRestrictedVision(Camera camera) {
        return camera.getEntity() instanceof LivingEntity entity
                && (entity.hasEffect(MobEffects.BLINDNESS) || entity.hasEffect(MobEffects.DARKNESS));
    }

    private static void drawScattering(int voxelTexture, int shapeTexture) {
        scattering.apply();
        try {
            RenderSystem.disableBlend();
            int active = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
            int shapeUnit = TerrainLighting.SHAPE_TEXTURE_UNIT;
            GL13.glActiveTexture(GL13.GL_TEXTURE0 + shapeUnit);
            int previousShape = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
            GL13.glActiveTexture(GL13.GL_TEXTURE0 + VOXEL_UNIT);
            int previous = GL11.glGetInteger(GL12.GL_TEXTURE_BINDING_3D);
            try {
                GL11.glBindTexture(GL12.GL_TEXTURE_3D, voxelTexture);
                GL20.glUniform1i(GL20.glGetUniformLocation(scattering.getId(), "JemVoxels"), VOXEL_UNIT);
                GL13.glActiveTexture(GL13.GL_TEXTURE0 + shapeUnit);
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, shapeTexture);
                GL20.glUniform1i(GL20.glGetUniformLocation(scattering.getId(), "JemShapes"), shapeUnit);
                drawQuad();
            } finally {
                GL13.glActiveTexture(GL13.GL_TEXTURE0 + shapeUnit);
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, previousShape);
                GL13.glActiveTexture(GL13.GL_TEXTURE0 + VOXEL_UNIT);
                GL11.glBindTexture(GL12.GL_TEXTURE_3D, previous);
                GL13.glActiveTexture(active);
            }
        } finally {
            scattering.clear();
        }
    }

    private static void drawQuad() {
        var buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        buffer.vertex(-1, -1, 0).uv(0, 0).endVertex();
        buffer.vertex(1, -1, 0).uv(1, 0).endVertex();
        buffer.vertex(1, 1, 0).uv(1, 1).endVertex();
        buffer.vertex(-1, 1, 0).uv(0, 1).endVertex();
        BufferUploader.draw(buffer.end());
    }

    private static void ensureTargets(RenderTarget main) {
        if (depthCopy != null && depthCopy.width == main.width && depthCopy.height == main.height
                && depthCopy.isStencilEnabled() == main.isStencilEnabled()) {
            return;
        }
        releaseTargets();
        depthCopy = new TextureTarget(main.width, main.height, true, Minecraft.ON_OSX);
        if (main.isStencilEnabled()) {
            depthCopy.enableStencil();
        }
        reduced = new TextureTarget((main.width + 1) / 2, (main.height + 1) / 2, false, Minecraft.ON_OSX);
    }

    static void releaseTargets() {
        if (depthCopy != null) {
            depthCopy.destroyBuffers();
            depthCopy = null;
        }
        if (reduced != null) {
            reduced.destroyBuffers();
            reduced = null;
        }
    }

    private static final class DrawState {
        private final int drawFramebuffer = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        private final int readFramebuffer = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        private final int program = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        private final int activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        private final int activeBinding = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        private final int[] viewport = new int[4];
        private final int[] colorMask = new int[4];
        private final boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        private final boolean depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        private final boolean blend = GL11.glIsEnabled(GL11.GL_BLEND);
        private final boolean cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        private final boolean scissor = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        private final boolean stencil = GL11.glIsEnabled(GL11.GL_STENCIL_TEST);
        private final int blendSrcRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB);
        private final int blendDstRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
        private final int blendSrcAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA);
        private final int blendDstAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
        private final int equationRgb = GL11.glGetInteger(GL20.GL_BLEND_EQUATION_RGB);
        private final int equationAlpha = GL11.glGetInteger(GL20.GL_BLEND_EQUATION_ALPHA);
        private final int texture0;
        private final int texture1;

        private DrawState() {
            GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
            GL11.glGetIntegerv(GL11.GL_COLOR_WRITEMASK, colorMask);
            GlStateManager._activeTexture(GL13.GL_TEXTURE0);
            texture0 = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
            GlStateManager._activeTexture(GL13.GL_TEXTURE1);
            texture1 = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
            GlStateManager._activeTexture(activeTexture);
        }

        private void restore() {
            setEnabled(GL11.GL_SCISSOR_TEST, scissor);
            setEnabled(GL11.GL_STENCIL_TEST, stencil);
            GL11.glColorMask(colorMask[0] != 0, colorMask[1] != 0, colorMask[2] != 0, colorMask[3] != 0);
            GL20.glBlendEquationSeparate(equationRgb, equationAlpha);
            GL14.glBlendFuncSeparate(blendSrcRgb, blendDstRgb, blendSrcAlpha, blendDstAlpha);
            setEnabled(GL11.GL_BLEND, blend);
            if (depth) RenderSystem.enableDepthTest(); else RenderSystem.disableDepthTest();
            if (cull) RenderSystem.enableCull(); else RenderSystem.disableCull();
            RenderSystem.depthMask(depthMask);

            GlStateManager._activeTexture(GL13.GL_TEXTURE0);
            GlStateManager._bindTexture(texture0);
            GlStateManager._activeTexture(GL13.GL_TEXTURE1);
            GlStateManager._bindTexture(texture1);
            GlStateManager._activeTexture(activeTexture);
            GlStateManager._bindTexture(activeBinding);
            GL20.glUseProgram(program);
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, readFramebuffer);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, drawFramebuffer);
            RenderSystem.viewport(viewport[0], viewport[1], viewport[2], viewport[3]);
        }

        private static void setEnabled(int capability, boolean enabled) {
            if (enabled) GL11.glEnable(capability); else GL11.glDisable(capability);
        }
    }
}
