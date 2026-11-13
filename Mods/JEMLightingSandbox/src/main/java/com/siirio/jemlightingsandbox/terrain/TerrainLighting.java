package com.siirio.jemlightingsandbox.terrain;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import com.siirio.jemlightingsandbox.LightingSettings;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.irisshaders.iris.api.v0.IrisApi;
import net.minecraft.client.Minecraft;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.ModList;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL21;
import org.lwjgl.opengl.GL30;
import org.slf4j.Logger;

public final class TerrainLighting {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int TEXTURE_UNIT = 7;
    public static final int SHAPE_TEXTURE_UNIT = 8;
    private static final int LIGHT_TEXTURE_UNIT = 9;
    private static final int CLOUD_TEXTURE_UNIT = 12;
    private static final ResourceLocation CLOUD_TEXTURE = ResourceLocation.tryParse("minecraft:textures/environment/clouds.png");
    private static final TerrainLightVolume VOLUME = new TerrainLightVolume();
    private static final ChunkLightIndex LIGHT_INDEX = new ChunkLightIndex();
    private static final ChunkLightTexture CHUNK_LIGHTS = new ChunkLightTexture();
    private static final Map<Integer, Uniforms> PROGRAMS = new HashMap<>();

    private static boolean enabled = true;
    private static boolean failed;
    private static int voxelTexture;
    private static int shapeTexture;
    private static FloatBuffer uploadedShapes;
    private static int previousTexture = -1;
    private static int previousShape;
    private static int previousLights;
    private static int previousCloud;
    private static final LivingEntity[] CASTERS = new LivingEntity[8];
    private static final double[] CASTER_DISTANCE = new double[CASTERS.length];
    private static TerrainLightVolume.Snapshot uploaded;

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
        if (oculusShadersEnabled()) {
            return "inactive (Oculus shader pack enabled)";
        }
        if (failed) {
            return "inactive (OpenGL setup failed; see the client log)";
        }
        if (PROGRAMS.values().stream().noneMatch(uniforms -> uniforms.active >= 0)) {
            return "waiting for a compatible Embeddium terrain draw";
        }
        var snapshot = VOLUME.snapshot();
        return "on, " + LIGHT_INDEX.coveredChunks() + " indexed chunks";
    }

    public static void tick(Minecraft client) {
        ClientLevel level = client.level;
        if (level == null || !available()) {
            if (VOLUME.snapshot() != null || level == null) {
                clearWorld();
            }
            return;
        }
        if (client.gameRenderer.getMainCamera().isInitialized()) {
            Vec3 camera = client.gameRenderer.getMainCamera().getPosition();
            VOLUME.tick(level, camera);
            LIGHT_INDEX.tick(level, camera, lightingChunks(client), LightingSettings.mode());
        }
    }

    public static void invalidate(BlockPos position) {
        VOLUME.invalidate(position);
        LIGHT_INDEX.invalidate(position);
    }

    public static void invalidateChunk(ChunkPos position) {
        VOLUME.invalidateChunk(position);
        LIGHT_INDEX.invalidateChunk(position);
    }

    public static void clearWorld() {
        VOLUME.clear();
        LIGHT_INDEX.clear();
        if (!RenderSystem.isOnRenderThread()) {
            RenderSystem.recordRenderCall(TerrainLighting::releaseTexture);
        } else {
            releaseTexture();
        }
    }

    private static void releaseTexture() {
        uploaded = null;
        uploadedShapes = null;
        CHUNK_LIGHTS.clear();
        if (shapeTexture != 0) {
            GL11.glDeleteTextures(shapeTexture);
            shapeTexture = 0;
        }
        if (voxelTexture != 0) {
            GL11.glDeleteTextures(voxelTexture);
            voxelTexture = 0;
        }
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

        // Each program must keep the volume sampler apart from Embeddium's 2D samplers.
        GL20.glUniform1i(uniforms.opacity, TEXTURE_UNIT);
        GL20.glUniform1i(uniforms.shapes, SHAPE_TEXTURE_UNIT);
        GL20.glUniform1i(uniforms.chunkLights, LIGHT_TEXTURE_UNIT);
        GL20.glUniform1i(uniforms.cloudTex, CLOUD_TEXTURE_UNIT);
        GL20.glUniform1i(uniforms.active, 0);
        if (!receivesLighting) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        VOLUME.refreshChangedBlocks(client.level);
        var snapshot = VOLUME.snapshot();
        if (!available() || snapshot == null || snapshot.level() != client.level) {
            return;
        }

        if (!GL.getCapabilities().OpenGL33 || GL11.glGetInteger(GL20.GL_MAX_TEXTURE_IMAGE_UNITS) <= LIGHT_TEXTURE_UNIT) {
            failed = true;
            LOGGER.warn("JEM terrain lighting requires OpenGL 3.3 and ten fragment texture units");
            return;
        }

        int activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        GL13.glActiveTexture(GL13.GL_TEXTURE0 + SHAPE_TEXTURE_UNIT);
        previousShape = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        GL13.glActiveTexture(GL13.GL_TEXTURE0 + LIGHT_TEXTURE_UNIT);
        previousLights = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        GL13.glActiveTexture(GL13.GL_TEXTURE0 + CLOUD_TEXTURE_UNIT);
        previousCloud = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        GL13.glActiveTexture(GL13.GL_TEXTURE0 + TEXTURE_UNIT);
        try {
            previousTexture = GL11.glGetInteger(GL12.GL_TEXTURE_BINDING_3D);
            if (voxelTexture == 0) {
                voxelTexture = GL11.glGenTextures();
            }
            GL11.glBindTexture(GL12.GL_TEXTURE_3D, voxelTexture);
            if (uploaded != snapshot) {
                uploadVoxels(snapshot);
                uploaded = snapshot;
            }

            Vec3 camera = client.gameRenderer.getMainCamera().getPosition();
            GL13.glActiveTexture(GL13.GL_TEXTURE0 + SHAPE_TEXTURE_UNIT);
            bindShapes(snapshot.shapePalette());
            GL13.glActiveTexture(GL13.GL_TEXTURE0 + LIGHT_TEXTURE_UNIT);
            int chunks = lightingChunks(client);
            CHUNK_LIGHTS.reachChanged(LightingSettings.lightDistance(), camera, chunks);
            for (var changed : LIGHT_INDEX.drainChanges()) {
                CHUNK_LIGHTS.mark(changed);
            }
            int rows = CHUNK_LIGHTS.bindAndUpdate(snapshot.level(), camera, chunks, LIGHT_INDEX);
            BlockPos cameraBlock = BlockPos.containing(camera);
            GL20.glUniform3i(uniforms.cameraBlock, cameraBlock.getX(), cameraBlock.getY(), cameraBlock.getZ());
            GL20.glUniform3f(uniforms.cameraFraction, (float) (camera.x - cameraBlock.getX()),
                    (float) (camera.y - cameraBlock.getY()), (float) (camera.z - cameraBlock.getZ()));
            GL20.glUniform4i(uniforms.chunkLayout, rows, snapshot.level().getMinSection(), chunks, 0);
            GL20.glUniform3f(uniforms.origin, (float) (snapshot.minX() - camera.x),
                    (float) (snapshot.minY() - camera.y), (float) (snapshot.minZ() - camera.z));
            GL20.glUniform3i(uniforms.size, snapshot.sizeX(), snapshot.sizeY(), snapshot.sizeZ());
            GL20.glUniform1i(uniforms.lightCount, snapshot.lights().size());
            int index = 0;
            for (var light : snapshot.lights()) {
                GL20.glUniform4f(uniforms.positions[index], (float) (light.x() - camera.x),
                        (float) (light.y() - camera.y), (float) (light.z() - camera.z),
                        light.emission() * LightingSettings.lightDistance() / 15F + 0.5F);
                GL20.glUniform4f(uniforms.colors[index], light.red(), light.green(), light.blue(), light.emission() / 15F);
                index++;
            }
            setSun(uniforms, snapshot.level(), client.getFrameTime());
            setEntityCasters(uniforms, client, snapshot.level(), camera);
            setCloudShadow(uniforms, client, snapshot.level(), camera);
            GL20.glUniform1i(uniforms.active, 1);
        } catch (RuntimeException exception) {
            failed = true;
            GL20.glUniform1i(uniforms.active, 0);
            restoreTextureBinding();
            LOGGER.error("Could not prepare JEM terrain lighting", exception);
        } finally {
            GL13.glActiveTexture(activeTexture);
        }
    }

    public static void end() {
        if (previousTexture < 0) {
            return;
        }
        int activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        GL13.glActiveTexture(GL13.GL_TEXTURE0 + TEXTURE_UNIT);
        try {
            restoreTextureBinding();
        } finally {
            GL13.glActiveTexture(activeTexture);
        }
    }

    private static void restoreTextureBinding() {
        if (previousTexture >= 0) {
            GL13.glActiveTexture(GL13.GL_TEXTURE0 + TEXTURE_UNIT);
            GL11.glBindTexture(GL12.GL_TEXTURE_3D, previousTexture);
            GL13.glActiveTexture(GL13.GL_TEXTURE0 + SHAPE_TEXTURE_UNIT);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, previousShape);
            GL13.glActiveTexture(GL13.GL_TEXTURE0 + LIGHT_TEXTURE_UNIT);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, previousLights);
            GL13.glActiveTexture(GL13.GL_TEXTURE0 + CLOUD_TEXTURE_UNIT);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, previousCloud);
            previousTexture = -1;
        }
    }

    private static int lightingChunks(Minecraft client) {
        return LightingSettings.lightingChunks(client.options.getEffectiveRenderDistance());
    }

    private static void bindShapes(FloatBuffer shapes) {
        if (shapeTexture == 0) {
            shapeTexture = GL11.glGenTextures();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, shapeTexture);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        } else {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, shapeTexture);
        }
        if (uploadedShapes != shapes) {
            int buffer = GL11.glGetInteger(GL21.GL_PIXEL_UNPACK_BUFFER_BINDING);
            int rowLength = GL11.glGetInteger(GL11.GL_UNPACK_ROW_LENGTH);
            int skipPixels = GL11.glGetInteger(GL11.GL_UNPACK_SKIP_PIXELS);
            int skipRows = GL11.glGetInteger(GL11.GL_UNPACK_SKIP_ROWS);
            try {
                GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER, 0);
                GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, 0);
                GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_PIXELS, 0);
                GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_ROWS, 0);
                GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_RGBA32F,
                        BlockShadowShapes.TEXTURE_WIDTH, BlockShadowShapes.TEXTURE_HEIGHT,
                        0, GL11.GL_RGBA, GL11.GL_FLOAT, shapes);
                uploadedShapes = shapes;
            } finally {
                GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, rowLength);
                GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_PIXELS, skipPixels);
                GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_ROWS, skipRows);
                GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER, buffer);
            }
        }
    }

    private static void uploadVoxels(TerrainLightVolume.Snapshot snapshot) {
        int unpackBuffer = GL11.glGetInteger(GL21.GL_PIXEL_UNPACK_BUFFER_BINDING);
        int alignment = GL11.glGetInteger(GL11.GL_UNPACK_ALIGNMENT);
        int rowLength = GL11.glGetInteger(GL11.GL_UNPACK_ROW_LENGTH);
        int imageHeight = GL11.glGetInteger(GL12.GL_UNPACK_IMAGE_HEIGHT);
        int skipPixels = GL11.glGetInteger(GL11.GL_UNPACK_SKIP_PIXELS);
        int skipRows = GL11.glGetInteger(GL11.GL_UNPACK_SKIP_ROWS);
        int skipImages = GL11.glGetInteger(GL12.GL_UNPACK_SKIP_IMAGES);
        try {
            GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER, 0);
            GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);
            GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, 0);
            GL11.glPixelStorei(GL12.GL_UNPACK_IMAGE_HEIGHT, 0);
            GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_PIXELS, 0);
            GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_ROWS, 0);
            GL11.glPixelStorei(GL12.GL_UNPACK_SKIP_IMAGES, 0);
            if (uploaded == null || uploaded.sizeX() != snapshot.sizeX() || uploaded.sizeY() != snapshot.sizeY()
                    || uploaded.sizeZ() != snapshot.sizeZ()) {
                GL11.glTexParameteri(GL12.GL_TEXTURE_3D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
                GL11.glTexParameteri(GL12.GL_TEXTURE_3D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
                GL11.glTexParameteri(GL12.GL_TEXTURE_3D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
                GL11.glTexParameteri(GL12.GL_TEXTURE_3D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
                GL11.glTexParameteri(GL12.GL_TEXTURE_3D, GL12.GL_TEXTURE_WRAP_R, GL12.GL_CLAMP_TO_EDGE);
                GL12.glTexImage3D(GL12.GL_TEXTURE_3D, 0, GL30.GL_RG8, snapshot.sizeX(), snapshot.sizeY(), snapshot.sizeZ(),
                        0, GL30.GL_RG, GL11.GL_UNSIGNED_BYTE, snapshot.voxelData());
            } else {
                GL12.glTexSubImage3D(GL12.GL_TEXTURE_3D, 0, 0, 0, 0, snapshot.sizeX(), snapshot.sizeY(),
                        snapshot.sizeZ(), GL30.GL_RG, GL11.GL_UNSIGNED_BYTE, snapshot.voxelData());
            }
        } finally {
            GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, alignment);
            GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, rowLength);
            GL11.glPixelStorei(GL12.GL_UNPACK_IMAGE_HEIGHT, imageHeight);
            GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_PIXELS, skipPixels);
            GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_ROWS, skipRows);
            GL11.glPixelStorei(GL12.GL_UNPACK_SKIP_IMAGES, skipImages);
            GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER, unpackBuffer);
        }
    }

    private static void setSun(Uniforms uniforms, ClientLevel level, float partialTick) {
        Sun sun = sun(level, partialTick);
        GL20.glUniform4f(uniforms.sun, sun.x(), sun.y(), 0F, sun.strength());
        GL20.glUniform3f(uniforms.sunColor, 1F, 1F, 1F);
    }

    private static void setEntityCasters(Uniforms uniforms, Minecraft client, ClientLevel level, Vec3 camera) {
        int count = 0;
        if (LightingSettings.atmosphereEnabled()) {
            for (var entity : level.entitiesForRendering()) {
                if (!(entity instanceof LivingEntity living) || !living.isAlive() || living.isInvisible()
                        || living.isSpectator()
                        || (living == client.getCameraEntity() && client.options.getCameraType().isFirstPerson())) {
                    continue;
                }
                double distance = living.distanceToSqr(camera);
                if (distance > 16 * 16) {
                    continue;
                }
                int slot = count;
                while (slot > 0 && CASTER_DISTANCE[slot - 1] > distance) {
                    slot--;
                }
                if (slot >= CASTERS.length) {
                    continue;
                }
                for (int index = Math.min(count, CASTERS.length - 1); index > slot; index--) {
                    CASTER_DISTANCE[index] = CASTER_DISTANCE[index - 1];
                    CASTERS[index] = CASTERS[index - 1];
                }
                CASTER_DISTANCE[slot] = distance;
                CASTERS[slot] = living;
                count = Math.min(count + 1, CASTERS.length);
            }
        }
        GL20.glUniform1i(uniforms.entityCount, count);
        for (int index = 0; index < count; index++) {
            LivingEntity entity = CASTERS[index];
            CASTERS[index] = null;
            var box = entity.getBoundingBox().move(entity.getPosition(client.getFrameTime()).subtract(entity.position()));
            GL20.glUniform3f(uniforms.entityMin[index], (float) (box.minX - camera.x),
                    (float) (box.minY - camera.y), (float) (box.minZ - camera.z));
            GL20.glUniform3f(uniforms.entityMax[index], (float) (box.maxX - camera.x),
                    (float) (box.maxY - camera.y), (float) (box.maxZ - camera.z));
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

    public static VolumeTexture currentVolume(ClientLevel level) {
        if (!available() || uploaded == null || uploaded.level() != level || voxelTexture == 0) {
            return null;
        }
        return new VolumeTexture(voxelTexture, shapeTexture, uploaded.minX(), uploaded.minY(), uploaded.minZ(),
                uploaded.sizeX(), uploaded.sizeY(), uploaded.sizeZ());
    }

    public static List<TerrainLightVolume.Light> localLights(ClientLevel level) {
        return uploaded == null || uploaded.level() != level ? List.of() : uploaded.lights();
    }

    public record Sun(float x, float y, float strength) {}

    public record VolumeTexture(int id, int shapes, int x, int y, int z, int width, int height, int depth) {}

    private static boolean available() {
        return enabled && !failed && canPatchStockShaders() && !oculusShadersEnabled();
    }

    private static boolean oculusShadersEnabled() {
        return ModList.get().isLoaded("oculus") && Oculus.shadersEnabled();
    }

    private static final class Oculus {
        private static boolean shadersEnabled() {
            return IrisApi.getInstance().getConfig().areShadersEnabled();
        }
    }

    private static final class Uniforms {
        private final int active;
        private final int opacity;
        private final int shapes;
        private final int chunkLights;
        private final int cameraBlock;
        private final int cameraFraction;
        private final int chunkLayout;
        private final int origin;
        private final int size;
        private final int lightCount;
        private final int sun;
        private final int sunColor;
        private final int cloud;
        private final int cloudTex;
        private final int entityCount;
        private final int[] entityMin = new int[CASTERS.length];
        private final int[] entityMax = new int[CASTERS.length];
        private final int[] positions = new int[8];
        private final int[] colors = new int[8];

        private Uniforms(int program) {
            active = GL20.glGetUniformLocation(program, "jemActive");
            opacity = GL20.glGetUniformLocation(program, "jemOpacity");
            shapes = GL20.glGetUniformLocation(program, "jemShapes");
            chunkLights = GL20.glGetUniformLocation(program, "jemChunkLights");
            cameraBlock = GL20.glGetUniformLocation(program, "jemCameraBlock");
            cameraFraction = GL20.glGetUniformLocation(program, "jemCameraFraction");
            chunkLayout = GL20.glGetUniformLocation(program, "jemChunkLayout");
            origin = GL20.glGetUniformLocation(program, "jemVolumeOrigin");
            size = GL20.glGetUniformLocation(program, "jemVolumeSize");
            lightCount = GL20.glGetUniformLocation(program, "jemLightCount");
            sun = GL20.glGetUniformLocation(program, "jemSunDirectionStrength");
            sunColor = GL20.glGetUniformLocation(program, "jemSunColor");
            cloud = GL20.glGetUniformLocation(program, "jemCloud");
            cloudTex = GL20.glGetUniformLocation(program, "jemCloudTexture");
            entityCount = GL20.glGetUniformLocation(program, "jemEntityCount");
            for (int index = 0; index < entityMin.length; index++) {
                entityMin[index] = GL20.glGetUniformLocation(program, "jemEntityMin[" + index + "]");
                entityMax[index] = GL20.glGetUniformLocation(program, "jemEntityMax[" + index + "]");
            }
            for (int index = 0; index < positions.length; index++) {
                positions[index] = GL20.glGetUniformLocation(program, "jemLightPositionRange[" + index + "]");
                colors[index] = GL20.glGetUniformLocation(program, "jemLightColorStrength[" + index + "]");
            }
        }
    }
}
