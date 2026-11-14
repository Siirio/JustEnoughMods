package com.siirio.jemlightingsandbox.terrain;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import com.siirio.jemlightingsandbox.LightingSettings;
import java.util.HashMap;
import java.util.Map;
import net.irisshaders.iris.api.v0.IrisApi;
import net.minecraft.client.Minecraft;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.ModList;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL31;
import org.slf4j.Logger;

public final class TerrainLighting {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int ENTITY_SHADOW_TEXTURE_UNIT = 6;
    private static final int ENTITY_MODELS_TEXTURE_UNIT = 14;
    private static final int SECTION_TEXTURE_UNIT = 7;
    private static final int SHAPE_TEXTURE_UNIT = 8;
    private static final int LIGHT_TEXTURE_UNIT = 9;
    private static final int CLOUD_TEXTURE_UNIT = 12;
    private static final int VOXEL_TEXTURE_UNIT = 13;
    private static final ResourceLocation CLOUD_TEXTURE = ResourceLocation.tryParse("minecraft:textures/environment/clouds.png");
    private static final WorldShadowSections SHADOWS = new WorldShadowSections();
    private static final ChunkLightIndex LIGHT_INDEX = new ChunkLightIndex();
    private static final ChunkLightTexture CHUNK_LIGHTS = new ChunkLightTexture();
    private static final Map<Integer, Uniforms> PROGRAMS = new HashMap<>();

    private static boolean enabled = true;
    private static boolean failed;
    private static WorldShadowSections.Texture world;
    private static ClientLevel currentLevel;
    private static int previousSections = -1;
    private static int previousVoxels;
    private static int previousShape;
    private static int previousLights;
    private static int previousCloud;
    private static int previousEntityShadow;
    private static int previousEntityModels;

    private TerrainLighting() {
    }

    public static boolean canPatchStockShaders() {
        return !ModList.get().isLoaded("luxium");
    }

    public static void setEnabled(boolean value) {
        enabled = value;
        if (!enabled) {
            clearWorld();
        }
    }

    public static String status() {
        if (!enabled) {
            return "off";
        }
        if (!canPatchStockShaders()) {
            return "inactive (Luxium owns the terrain shaders)";
        }
        if (isShaderPackActive()) {
            return "inactive (Oculus shader pack enabled)";
        }
        if (failed) {
            return "inactive (OpenGL setup failed; see the client log)";
        }
        if (PROGRAMS.values().stream().noneMatch(uniforms -> uniforms.active >= 0)) {
            return "waiting for a compatible Embeddium terrain draw";
        }
        return "on, " + LIGHT_INDEX.coveredChunks() + " indexed chunks";
    }

    public static void tick(Minecraft client) {
        ClientLevel level = client.level;
        if (level == null || !available()) {
            if (currentLevel != null) {
                clearWorld();
            }
            return;
        }
        if (currentLevel != level) {
            clearWorld();
            currentLevel = level;
        }
        if (client.gameRenderer.getMainCamera().isInitialized()) {
            Vec3 camera = client.gameRenderer.getMainCamera().getPosition();
            LIGHT_INDEX.tick(level, camera, lightingChunks(client), LightingSettings.mode(), SHADOWS);
        }
    }

    public static void invalidate(BlockPos position) {
        LIGHT_INDEX.invalidate(position);
    }

    public static void invalidateChunk(ChunkPos position) {
        LIGHT_INDEX.invalidateChunk(position);
    }

    public static void reloadModels() {
        clearWorld();
        failed = false;
    }

    public static void clearWorld() {
        currentLevel = null;
        LIGHT_INDEX.clear();
        if (!RenderSystem.isOnRenderThread()) {
            RenderSystem.recordRenderCall(TerrainLighting::releaseTexture);
        } else {
            releaseTexture();
        }
    }

    private static void releaseTexture() {
        world = null;
        SHADOWS.clear();
        CHUNK_LIGHTS.clear();
        EntityShadowMap.clear();
    }

    public static void invalidateProgram(int program) {
        PROGRAMS.remove(program);
    }

    public static void clearProgramBindings() {
        PROGRAMS.clear();
    }

    public static void begin(int program, boolean receivesLighting) {
        Uniforms uniforms = PROGRAMS.computeIfAbsent(program, Uniforms::new);
        if (uniforms.active < 0) {
            return;
        }

        // The section table, palette and voxel buffer have distinct sampler types.
        GL20.glUniform1i(uniforms.entityShadowTexture, ENTITY_SHADOW_TEXTURE_UNIT);
        GL20.glUniform1i(uniforms.entityModelsTexture, ENTITY_MODELS_TEXTURE_UNIT);
        GL20.glUniform1i(uniforms.sections, SECTION_TEXTURE_UNIT);
        GL20.glUniform1i(uniforms.voxels, VOXEL_TEXTURE_UNIT);
        GL20.glUniform1i(uniforms.shapes, SHAPE_TEXTURE_UNIT);
        GL20.glUniform1i(uniforms.chunkLights, LIGHT_TEXTURE_UNIT);
        GL20.glUniform1i(uniforms.cloudTex, CLOUD_TEXTURE_UNIT);
        GL20.glUniform1i(uniforms.active, 0);
        if (!receivesLighting || !available()) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        ClientLevel level = client.level;
        if (level == null || level != currentLevel || !client.gameRenderer.getMainCamera().isInitialized()) {
            return;
        }
        if (!GL.getCapabilities().OpenGL33 || GL11.glGetInteger(GL20.GL_MAX_TEXTURE_IMAGE_UNITS) <= ENTITY_MODELS_TEXTURE_UNIT) {
            failed = true;
            LOGGER.warn("JEM terrain lighting requires OpenGL 3.3 and fifteen fragment texture units");
            return;
        }
        int active = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        try {
            previousSections = capture2D(SECTION_TEXTURE_UNIT);
            GL13.glActiveTexture(GL13.GL_TEXTURE0 + VOXEL_TEXTURE_UNIT);
            previousVoxels = GL11.glGetInteger(GL31.GL_TEXTURE_BINDING_BUFFER);
            previousShape = capture2D(SHAPE_TEXTURE_UNIT);
            previousLights = capture2D(LIGHT_TEXTURE_UNIT);
            previousCloud = capture2D(CLOUD_TEXTURE_UNIT);
            previousEntityShadow = capture2D(ENTITY_SHADOW_TEXTURE_UNIT);
            previousEntityModels = capture2D(ENTITY_MODELS_TEXTURE_UNIT);

            world = SHADOWS.bind(level, SECTION_TEXTURE_UNIT, SHAPE_TEXTURE_UNIT, VOXEL_TEXTURE_UNIT);
            if (world == null) {
                return;
            }
            Vec3 camera = client.gameRenderer.getMainCamera().getPosition();
            GL13.glActiveTexture(GL13.GL_TEXTURE0 + LIGHT_TEXTURE_UNIT);
            int chunks = lightingChunks(client);
            CHUNK_LIGHTS.reachChanged(LightingSettings.lightDistance(), camera, chunks);
            for (var changed : LIGHT_INDEX.drainChanges()) {
                CHUNK_LIGHTS.mark(changed);
            }
            int rows = CHUNK_LIGHTS.bindAndUpdate(level, camera, chunks, LIGHT_INDEX);
            BlockPos cameraBlock = BlockPos.containing(camera);
            GL20.glUniform3i(uniforms.cameraBlock, cameraBlock.getX(), cameraBlock.getY(), cameraBlock.getZ());
            GL20.glUniform3f(uniforms.cameraFraction, (float) (camera.x - cameraBlock.getX()),
                    (float) (camera.y - cameraBlock.getY()), (float) (camera.z - cameraBlock.getZ()));
            GL20.glUniform2i(uniforms.worldSections, world.minSection(), world.sections());
            GL20.glUniform4i(uniforms.chunkLayout, rows, level.getMinSection(), chunks, 0);
            GL20.glUniform1i(uniforms.shadowSamples, LightingSettings.pointShadowSamples());
            float time = worldTime(level, client.getFrameTime());
            GL20.glUniform1f(uniforms.shadowTime, time);
            setSun(uniforms, level, client.getFrameTime());
            setEntityShadow(uniforms, level, camera);
            setCloudShadow(uniforms, client, level, camera);
            GL20.glUniform1i(uniforms.active, 1);
        } catch (RuntimeException exception) {
            failed = true;
            GL20.glUniform1i(uniforms.active, 0);
            restoreTextureBinding();
            LOGGER.error("Could not prepare JEM terrain lighting", exception);
        } finally {
            GL13.glActiveTexture(active);
        }
    }

    public static void end() {
        if (previousSections < 0) {
            return;
        }
        int active = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        try {
            restoreTextureBinding();
        } finally {
            GL13.glActiveTexture(active);
        }
    }

    private static int capture2D(int unit) {
        GL13.glActiveTexture(GL13.GL_TEXTURE0 + unit);
        return GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
    }

    private static void restoreTextureBinding() {
        if (previousSections < 0) {
            return;
        }
        restore2D(SECTION_TEXTURE_UNIT, previousSections);
        restore2D(SHAPE_TEXTURE_UNIT, previousShape);
        restore2D(LIGHT_TEXTURE_UNIT, previousLights);
        restore2D(ENTITY_SHADOW_TEXTURE_UNIT, previousEntityShadow);
        restore2D(ENTITY_MODELS_TEXTURE_UNIT, previousEntityModels);
        restore2D(CLOUD_TEXTURE_UNIT, previousCloud);
        GL13.glActiveTexture(GL13.GL_TEXTURE0 + VOXEL_TEXTURE_UNIT);
        GL11.glBindTexture(GL31.GL_TEXTURE_BUFFER, previousVoxels);
        previousSections = -1;
    }

    private static void restore2D(int unit, int id) {
        GL13.glActiveTexture(GL13.GL_TEXTURE0 + unit);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, id);
    }

    private static int lightingChunks(Minecraft client) {
        return LightingSettings.lightingChunks(client.options.getEffectiveRenderDistance());
    }

    public static float worldTime(ClientLevel level, float partialTick) {
        return Math.floorMod(level.getGameTime(), 100L) + partialTick;
    }

    private static void setSun(Uniforms uniforms, ClientLevel level, float partialTick) {
        Sun sun = celestial(level, partialTick);
        GL20.glUniform4f(uniforms.sun, sun.x(), sun.y(), 0F, sun.strength());
        boolean moon = Mth.cos(level.getSunAngle(partialTick)) < 0F;
        GL20.glUniform3f(uniforms.sunColor, moon ? 0.64F : 1F, moon ? 0.79F : 1F, 1F);
    }

    private static void setEntityShadow(Uniforms uniforms, ClientLevel level, Vec3 camera) {
        var map = EntityShadowMap.current(level);
        GL20.glUniform1i(uniforms.entityShadowActive, map == null || map.texture() == 0 ? 0 : 1);
        GL20.glUniform1i(uniforms.entityCasterCount, map == null ? 0 : map.casters());
        if (map != null) {
            GL13.glActiveTexture(GL13.GL_TEXTURE0 + ENTITY_SHADOW_TEXTURE_UNIT);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, map.texture());
            GL13.glActiveTexture(GL13.GL_TEXTURE0 + ENTITY_MODELS_TEXTURE_UNIT);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, map.models());
            Vec3 origin = map.origin().subtract(camera);
            GL20.glUniform3f(uniforms.entityShadowOrigin, (float) origin.x, (float) origin.y, (float) origin.z);
            GL20.glUniform2f(uniforms.entityShadowDirection, map.sunX(), map.sunY());
        }
    }

    private static void setCloudShadow(Uniforms uniforms, Minecraft client, ClientLevel level, Vec3 camera) {
        float height = level.effects().getCloudHeight();
        if (!LightingSettings.atmosphereEnabled() || client.options.getCloudsType() == CloudStatus.OFF
                || ModList.get().isLoaded("betterclouds") || Float.isNaN(height)) {
            GL20.glUniform4f(uniforms.cloud, 0F, 0F, 0F, 0F);
            return;
        }
        int texture = client.getTextureManager().getTexture(CLOUD_TEXTURE).getId();
        GL13.glActiveTexture(GL13.GL_TEXTURE0 + CLOUD_TEXTURE_UNIT);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
        double x = (camera.x + (client.levelRenderer.getTicks() + client.getFrameTime()) * 0.03) / 3072.0;
        double z = (camera.z / 12.0 + 0.33) / 256.0;
        GL20.glUniform4f(uniforms.cloud, (float) (x - Math.floor(x)), (float) (z - Math.floor(z)),
                height + 0.33F - (float) camera.y, 0.26F);
    }

    public static Sun sun(ClientLevel level, float partialTick) {
        float angle = level.getSunAngle(partialTick);
        float altitude = Mth.cos(angle);
        float strength = 0F;
        if (level.dimensionType().hasSkyLight() && !level.dimensionType().hasCeiling()) {
            strength = Mth.clamp(altitude * 4F, 0F, 1F) * (1F - level.getRainLevel(partialTick) * 0.85F);
        }
        return new Sun(-Mth.sin(angle), altitude, strength);
    }

    public static Sun celestial(ClientLevel level, float partialTick) {
        Sun day = sun(level, partialTick);
        if (day.y() >= 0F) {
            return day;
        }
        float strength = 0F;
        if (level.dimensionType().hasSkyLight() && !level.dimensionType().hasCeiling()) {
            float phase = level.getMoonBrightness();
            strength = Mth.clamp(-day.y() * 4F, 0F, 1F) * (0.16F + 0.24F * phase)
                    * (1F - level.getRainLevel(partialTick) * 0.85F);
        }
        return new Sun(-day.x(), -day.y(), strength);
    }

    public static ShadowWorld currentWorld(ClientLevel level) {
        if (!available() || currentLevel != level || world == null) {
            return null;
        }
        return new ShadowWorld(world.table(), world.palette(), world.voxels(), world.minSection(), world.sections(),
                world.revision());
    }

    public static boolean hasBlockLights(ClientLevel level) {
        return currentLevel == level && LIGHT_INDEX.hasLights();
    }

    public static boolean isTerrainEnabled() {
        return available();
    }

    public record Sun(float x, float y, float strength) {}

    public record ShadowWorld(int sections, int shapes, int voxels, int minSection, int sectionCount, long revision) {}

    static boolean available() {
        return enabled && !failed && canPatchStockShaders() && !isShaderPackActive();
    }

    public static boolean isShaderPackActive() {
        return ModList.get().isLoaded("oculus") && Oculus.shadersEnabled();
    }

    private static final class Oculus {
        private static boolean shadersEnabled() {
            return IrisApi.getInstance().getConfig().areShadersEnabled();
        }
    }

    private static final class Uniforms {
        private final int active;
        private final int sections;
        private final int voxels;
        private final int worldSections;
        private final int shadowSamples;
        private final int shadowTime;
        private final int shapes;
        private final int chunkLights;
        private final int cameraBlock;
        private final int cameraFraction;
        private final int chunkLayout;
        private final int sun;
        private final int sunColor;
        private final int cloud;
        private final int cloudTex;
        private final int entityShadowActive;
        private final int entityShadowTexture;
        private final int entityShadowOrigin;
        private final int entityShadowDirection;
        private final int entityCasterCount;
        private final int entityModelsTexture;

        private Uniforms(int program) {
            active = GL20.glGetUniformLocation(program, "jemActive");
            sections = GL20.glGetUniformLocation(program, "jemSections");
            voxels = GL20.glGetUniformLocation(program, "jemVoxels");
            worldSections = GL20.glGetUniformLocation(program, "jemWorldSections");
            shadowSamples = GL20.glGetUniformLocation(program, "jemShadowSamples");
            shadowTime = GL20.glGetUniformLocation(program, "jemShadowTime");
            shapes = GL20.glGetUniformLocation(program, "jemShapes");
            chunkLights = GL20.glGetUniformLocation(program, "jemChunkLights");
            cameraBlock = GL20.glGetUniformLocation(program, "jemCameraBlock");
            cameraFraction = GL20.glGetUniformLocation(program, "jemCameraFraction");
            chunkLayout = GL20.glGetUniformLocation(program, "jemChunkLayout");
            sun = GL20.glGetUniformLocation(program, "jemSunDirectionStrength");
            sunColor = GL20.glGetUniformLocation(program, "jemSunColor");
            cloud = GL20.glGetUniformLocation(program, "jemCloud");
            cloudTex = GL20.glGetUniformLocation(program, "jemCloudTexture");
            entityShadowActive = GL20.glGetUniformLocation(program, "jemEntityShadowActive");
            entityShadowTexture = GL20.glGetUniformLocation(program, "jemEntityShadowTexture");
            entityShadowOrigin = GL20.glGetUniformLocation(program, "jemEntityShadowOrigin");
            entityShadowDirection = GL20.glGetUniformLocation(program, "jemEntityShadowDirection");
            entityCasterCount = GL20.glGetUniformLocation(program, "jemEntityCasterCount");
            entityModelsTexture = GL20.glGetUniformLocation(program, "jemEntityModels");
        }
    }
}
