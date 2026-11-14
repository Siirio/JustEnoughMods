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
import org.lwjgl.opengl.GL21;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;
import org.lwjgl.opengl.GL15;
import org.slf4j.Logger;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.material.FogType;
import net.minecraftforge.client.event.RenderLevelStageEvent;

final class AtmosphereRenderer {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int SECTION_UNIT = 7;
    private static final int PALETTE_UNIT = 8;
    private static final int VOXEL_UNIT = 13;
    private static final float MAP_WIDTH = 72F;
    private static final float MAP_REACH = 110F;
    private static final double LIGHT_ANGLE_STEP = Math.PI / 360.0;

    private static ShaderInstance scattering;
    private static ShaderInstance composite;
    private static ShaderInstance visibilityShader;
    private static TextureTarget depthCopy;
    private static TextureTarget reduced;
    private static TextureTarget colorCopy;
    private static TextureTarget visibility;
    private static long visibilityRevision = -1L;
    private static ShaftMap shaftMap;
    private static ClientLevel weatherLevel;
    private static long lastRain = Long.MIN_VALUE;
    private static boolean failed;

    private AtmosphereRenderer() {
    }

    static void shaders(ShaderInstance scatterShader, ShaderInstance compositeShader, ShaderInstance shaftShader) {
        scattering = scatterShader;
        composite = compositeShader;
        visibilityShader = shaftShader;
        visibilityRevision = -1L;
        failed = false;
    }

    static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL || scattering == null || composite == null
                || failed || !LightingSettings.atmosphereEnabled() || TerrainLighting.isShaderPackActive()) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        ClientLevel level = client.level;
        Camera camera = event.getCamera();
        FogType medium = camera.getFluidInCamera();
        if (level == null || !level.dimensionType().hasSkyLight() || level.dimensionType().hasCeiling()
                || (medium != FogType.NONE && medium != FogType.WATER) || hasRestrictedVision(camera)) {
            return;
        }
        boolean underwater = medium == FogType.WATER;
        var shadowWorld = TerrainLighting.currentWorld(level);
        RenderTarget main = client.getMainRenderTarget();
        if (main.width < 2 || main.height < 2 || main.width != main.viewWidth || main.height != main.viewHeight
                || !main.useDepth || main.getDepthTextureId() <= 0) {
            return;
        }
        var sun = TerrainLighting.sun(level, event.getPartialTick());
        var celestial = TerrainLighting.celestial(level, event.getPartialTick());
        boolean moon = sun.y() < 0F;
        float weather = level.getRainLevel(event.getPartialTick());
        var block = camera.getBlockPosition();
        var biome = level.getBiome(block).value();
        long gameTime = level.getGameTime();
        if (weatherLevel != level || gameTime < lastRain) {
            weatherLevel = level;
            lastRain = Long.MIN_VALUE;
        }
        if (weather > 0.55F) {
            lastRain = gameTime;
        }
        float openSky = Mth.clamp((level.getBrightness(LightLayer.SKY, block) - 8F) / 7F, 0F, 1F);
        float aurora = biome.coldEnoughToSnow(block) ? Mth.clamp(-sun.y() * 3F, 0F, 1F) * (1F - weather) : 0F;
        float rainbow = lastRain == Long.MIN_VALUE ? 0F : (1F - weather)
                * Mth.clamp(1F - (gameTime - lastRain) / 900F, 0F, 1F) * sun.strength();
        float flash = 0F;
        if (!client.options.hideLightningFlash().get()) {
            float thunder = level.getThunderLevel(event.getPartialTick());
            float timeSinceFlash = (gameTime % 227L + event.getPartialTick());
            float distant = thunder * 0.35F * Math.max(0F, 1F - timeSinceFlash / 7F);
            flash = Math.max(distant, Mth.clamp(level.getSkyFlashTime() / 3F, 0F, 1F));
        }
        float clock = ((gameTime % 81920L) + event.getPartialTick()) / 20F;
        float heat = underwater ? 0F : SandboxWeather.heat(client);
        if (!underwater
                && Math.max(Math.max(celestial.strength(), weather), Math.max(heat, Math.max(aurora, flash))) < 0.01F) {
            return;
        }

        var inverseViewProjection = new Matrix4f().set(RenderSystem.getInverseViewRotationMatrix())
                .mul(new Matrix4f(event.getProjectionMatrix()).invert());
        var position = camera.getPosition();
        float rayStrength = moon ? (float) Math.sqrt(celestial.strength()) * 0.82F : celestial.strength();
        float sunHeight = Mth.clamp(sun.y() * 2.5F, 0F, 1F);
        float lowSun = 1F - sunHeight * sunHeight * (3F - 2F * sunHeight);

        DrawState state = new DrawState();
        try {
            ensureTargets(main);
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
            depthCopy.copyDepthFrom(main);
            if (underwater || heat > 0.01F) {
                copySceneColor(main);
            }
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.disableCull();
            GL11.glDisable(GL11.GL_STENCIL_TEST);
            GL11.glColorMask(true, true, true, true);
            RenderSystem.disableBlend();

            boolean hasMap = shadowWorld != null && visibilityShader != null && rayStrength >= 0.01F;
            if (hasMap) {
                updateVisibility(shadowWorld, celestial, position);
            }
            reduced.bindWrite(true);
            scattering.setSampler("SceneDepth", depthCopy.getDepthTextureId());
            boolean shaftsEnabled = TerrainLighting.isTerrainEnabled();
            scattering.setSampler("ShaftVisibility", hasMap ? visibility.getColorTextureId() : depthCopy.getDepthTextureId());
            scattering.safeGetUniform("InverseViewProjection").set(inverseViewProjection);
            if (hasMap) {
                Vec3 origin = shaftMap.origin().subtract(position);
                scattering.safeGetUniform("ShaftMapOrigin").set((float) origin.x, (float) origin.y, (float) origin.z);
                scattering.safeGetUniform("ShaftMapDirection").set(shaftMap.x(), shaftMap.y(), 0F);
            }
            scattering.safeGetUniform("ShaftMapReach").set(hasMap ? MAP_REACH : 0F);
            scattering.safeGetUniform("ShaftMapWidth").set(MAP_WIDTH);
            scattering.safeGetUniform("ShaftStrength").set(shaftsEnabled ? rayStrength : 0F);
            scattering.safeGetUniform("SunDirection").set(celestial.x(), celestial.y(), 0F);
            scattering.safeGetUniform("SunStrength").set(rayStrength);
            scattering.safeGetUniform("SunColor").set(moon ? 0.48F : 1F, moon ? 0.67F : 0.94F - lowSun * 0.12F,
                    moon ? 1F : 0.81F - lowSun * 0.26F);
            scattering.safeGetUniform("Underwater").set(underwater ? 1F : 0F);
            scattering.safeGetUniform("OpenSky").set(openSky);
            scattering.safeGetUniform("Aurora").set(aurora);
            scattering.safeGetUniform("Rainbow").set(rainbow);
            scattering.safeGetUniform("DistantFlash").set(flash);
            scattering.safeGetUniform("AtmosphereTime").set(clock);
            int water = biome.getWaterFogColor();
            scattering.safeGetUniform("WaterColor").set((water >> 16 & 255) / 255F, (water >> 8 & 255) / 255F,
                    (water & 255) / 255F);
            int tint = biome.getWaterColor();
            float clarity = Mth.clamp(0.30F + ((tint & 255) - (tint >> 16 & 255)) / 400F, 0.15F, 0.85F);
            scattering.safeGetUniform("WaterClarity").set(clarity);
            scattering.apply();
            try {
                drawQuad();
            } finally {
                scattering.clear();
            }

            main.bindWrite(true);
            composite.setSampler("AtmosphereTex", reduced.getColorTextureId());
            composite.setSampler("SceneDepth", depthCopy.getDepthTextureId());
            composite.setSampler("SceneColor", underwater || heat > 0.01F ? colorCopy.getColorTextureId() : reduced.getColorTextureId());
            composite.safeGetUniform("Underwater").set(underwater ? 1F : 0F);
            composite.safeGetUniform("Heat").set(heat);
            var heatOrigin = SandboxWeather.heatOrigin(client);
            if (heatOrigin == null) {
                composite.safeGetUniform("HeatOrigin").set(0F, 0F, 0F, 0F);
            } else {
                composite.safeGetUniform("HeatOrigin").set((float) (heatOrigin.x - position.x),
                        (float) (heatOrigin.y - position.y + 0.8), (float) (heatOrigin.z - position.z), 1.3F);
            }
            composite.safeGetUniform("AtmosphereTime").set(clock);
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

    private static void copySceneColor(RenderTarget main) {
        if (colorCopy == null) {
            colorCopy = new TextureTarget(main.width, main.height, false, Minecraft.ON_OSX);
        }
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, main.frameBufferId);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, colorCopy.frameBufferId);
        GL30.glBlitFramebuffer(0, 0, main.width, main.height, 0, 0, main.width, main.height,
                GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);
    }

    private static boolean hasRestrictedVision(Camera camera) {
        return camera.getEntity() instanceof LivingEntity entity
                && (entity.hasEffect(MobEffects.BLINDNESS) || entity.hasEffect(MobEffects.DARKNESS));
    }

    private static void updateVisibility(TerrainLighting.ShadowWorld world, TerrainLighting.Sun light, Vec3 camera) {
        int angle = (int) Math.round(Math.atan2(light.y(), light.x()) / LIGHT_ANGLE_STEP);
        int size = LightingSettings.shadowResolution();
        boolean sameProjection = shaftMap != null && shaftMap.angle() == angle
                && camera.distanceToSqr(shaftMap.center()) < 36;
        boolean sameResources = shaftMap != null && visibility.width == size && world.revision() == visibilityRevision;
        if (sameProjection && sameResources) {
            return;
        }

        double radians = angle * LIGHT_ANGLE_STEP;
        float x = (float) Math.cos(radians);
        float y = (float) Math.sin(radians);
        double texel = MAP_WIDTH / size;
        var direction = new Vec3(x, y, 0F);
        var side = new Vec3(y, -x, 0F);
        Vec3 center = sameProjection ? shaftMap.center() : camera;
        Vec3 origin = side.scale(Math.floor(center.dot(side) / texel) * texel)
                .add(direction.scale(Math.floor(center.dot(direction) / texel) * texel + 54F))
                .add(0.0, 0.0, Math.floor(center.z / texel) * texel);
        if (visibility == null || visibility.width != size) {
            if (visibility != null) visibility.destroyBuffers();
            visibility = createVisibilityTarget(size);
        }
        BlockPos cameraBlock = BlockPos.containing(camera);
        Vec3 sectionOrigin = origin.subtract(cameraBlock.getX() & ~15, cameraBlock.getY() & ~15, cameraBlock.getZ() & ~15);
        visibility.bindWrite(true);
        visibilityShader.safeGetUniform("jemWorldSections").set(world.minSection(), world.sectionCount());
        visibilityShader.safeGetUniform("jemCameraBlock").set(cameraBlock.getX(), cameraBlock.getY(), cameraBlock.getZ());
        visibilityShader.safeGetUniform("MapOrigin").set((float) sectionOrigin.x, (float) sectionOrigin.y, (float) sectionOrigin.z);
        visibilityShader.safeGetUniform("MapDirection").set(x, y, 0F);
        visibilityShader.safeGetUniform("MapWidth").set(MAP_WIDTH);
        visibilityShader.safeGetUniform("MapReach").set(MAP_REACH);
        visibilityShader.apply();
        try {
            int active = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
            GL13.glActiveTexture(GL13.GL_TEXTURE0 + SECTION_UNIT);
            int previousSection = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
            GL13.glActiveTexture(GL13.GL_TEXTURE0 + PALETTE_UNIT);
            int previousShape = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
            GL13.glActiveTexture(GL13.GL_TEXTURE0 + VOXEL_UNIT);
            int previousVoxels = GL11.glGetInteger(GL31.GL_TEXTURE_BINDING_BUFFER);
            try {
                bindShadowSampler("jemSections", SECTION_UNIT, GL11.GL_TEXTURE_2D, world.sections());
                bindShadowSampler("jemShapes", PALETTE_UNIT, GL11.GL_TEXTURE_2D, world.shapes());
                bindShadowSampler("jemVoxels", VOXEL_UNIT, GL31.GL_TEXTURE_BUFFER, world.voxels());
                drawQuad();
            } finally {
                GL13.glActiveTexture(GL13.GL_TEXTURE0 + SECTION_UNIT);
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, previousSection);
                GL13.glActiveTexture(GL13.GL_TEXTURE0 + PALETTE_UNIT);
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, previousShape);
                GL13.glActiveTexture(GL13.GL_TEXTURE0 + VOXEL_UNIT);
                GL11.glBindTexture(GL31.GL_TEXTURE_BUFFER, previousVoxels);
                GL13.glActiveTexture(active);
            }
        } finally {
            visibilityShader.clear();
        }
        shaftMap = new ShaftMap(angle, x, y, center, origin);
        visibilityRevision = world.revision();
    }

    private static void bindShadowSampler(String name, int unit, int target, int id) {
        GL13.glActiveTexture(GL13.GL_TEXTURE0 + unit);
        GL11.glBindTexture(target, id);
        GL20.glUniform1i(GL20.glGetUniformLocation(visibilityShader.getId(), name), unit);
    }

    private static TextureTarget createVisibilityTarget(int size) {
        int unpack = GL11.glGetInteger(GL21.GL_PIXEL_UNPACK_BUFFER_BINDING);
        try {
            GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER, 0);
            TextureTarget target = new TextureTarget(size, size, false, Minecraft.ON_OSX);
            GlStateManager._bindTexture(target.getColorTextureId());
            // Store sub-block distances without a fixed-point conversion or filtered depth edges.
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_RGBA32F, size, size,
                    0, GL11.GL_RGBA, GL11.GL_FLOAT, (java.nio.FloatBuffer) null);
            return target;
        } finally {
            GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER, unpack);
        }
    }

    private record ShaftMap(int angle, float x, float y, Vec3 center, Vec3 origin) {}

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
        if (visibility != null) {
            visibility.destroyBuffers();
            visibility = null;
        }
        visibilityRevision = -1L;
        shaftMap = null;
        if (depthCopy != null) {
            depthCopy.destroyBuffers();
            depthCopy = null;
        }
        if (reduced != null) {
            reduced.destroyBuffers();
            reduced = null;
        }
        if (colorCopy != null) {
            colorCopy.destroyBuffers();
            colorCopy = null;
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
        private final int texture2;

        private DrawState() {
            GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
            GL11.glGetIntegerv(GL11.GL_COLOR_WRITEMASK, colorMask);
            GlStateManager._activeTexture(GL13.GL_TEXTURE0);
            texture0 = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
            GlStateManager._activeTexture(GL13.GL_TEXTURE1);
            texture1 = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
            GlStateManager._activeTexture(GL13.GL_TEXTURE2);
            texture2 = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
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
            GlStateManager._activeTexture(GL13.GL_TEXTURE2);
            GlStateManager._bindTexture(texture2);
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
