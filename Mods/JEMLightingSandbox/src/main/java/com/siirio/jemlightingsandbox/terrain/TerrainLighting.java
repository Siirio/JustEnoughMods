package com.siirio.jemlightingsandbox.terrain;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import com.siirio.jemlightingsandbox.LightingSettings;
import java.util.HashMap;
import java.util.Map;
import net.irisshaders.iris.api.v0.IrisApi;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
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
    private static final TerrainLightVolume VOLUME = new TerrainLightVolume();
    private static final Map<Integer, Uniforms> PROGRAMS = new HashMap<>();

    private static boolean enabled = true;
    private static boolean failed;
    private static int voxelTexture;
    private static int previousTexture = -1;
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
        return snapshot == null ? "building the terrain volume" : "on, " + snapshot.lights().size() + " local lights";
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
            VOLUME.tick(level, client.gameRenderer.getMainCamera().getPosition());
        }
    }

    public static void invalidate(BlockPos position) {
        VOLUME.invalidate(position);
    }

    public static void invalidateChunk(ChunkPos position) {
        VOLUME.invalidateChunk(position);
    }

    public static void clearWorld() {
        VOLUME.clear();
        if (!RenderSystem.isOnRenderThread()) {
            RenderSystem.recordRenderCall(TerrainLighting::releaseTexture);
        } else {
            releaseTexture();
        }
    }

    private static void releaseTexture() {
        uploaded = null;
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

        if (!GL.getCapabilities().OpenGL33 || GL11.glGetInteger(GL20.GL_MAX_TEXTURE_IMAGE_UNITS) <= TEXTURE_UNIT) {
            failed = true;
            LOGGER.warn("JEM terrain lighting requires OpenGL 3.3 and eight fragment texture units");
            return;
        }

        int activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
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
            GL11.glBindTexture(GL12.GL_TEXTURE_3D, previousTexture);
            previousTexture = -1;
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
        return new VolumeTexture(voxelTexture, uploaded.minX(), uploaded.minY(), uploaded.minZ(),
                uploaded.sizeX(), uploaded.sizeY(), uploaded.sizeZ());
    }

    public record Sun(float x, float y, float strength) {}

    public record VolumeTexture(int id, int x, int y, int z, int width, int height, int depth) {}

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
        private final int origin;
        private final int size;
        private final int lightCount;
        private final int sun;
        private final int sunColor;
        private final int[] positions = new int[8];
        private final int[] colors = new int[8];

        private Uniforms(int program) {
            active = GL20.glGetUniformLocation(program, "jemActive");
            opacity = GL20.glGetUniformLocation(program, "jemOpacity");
            origin = GL20.glGetUniformLocation(program, "jemVolumeOrigin");
            size = GL20.glGetUniformLocation(program, "jemVolumeSize");
            lightCount = GL20.glGetUniformLocation(program, "jemLightCount");
            sun = GL20.glGetUniformLocation(program, "jemSunDirectionStrength");
            sunColor = GL20.glGetUniformLocation(program, "jemSunColor");
            for (int index = 0; index < positions.length; index++) {
                positions[index] = GL20.glGetUniformLocation(program, "jemLightPositionRange[" + index + "]");
                colors[index] = GL20.glGetUniformLocation(program, "jemLightColorStrength[" + index + "]");
            }
        }
    }
}
