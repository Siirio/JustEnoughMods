package com.siirio.jemlightingsandbox.terrain;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.logging.LogUtils;
import com.siirio.jemlightingsandbox.LightingSettings;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL21;
import org.lwjgl.opengl.GL30;
import org.slf4j.Logger;

/** Produces a small directional depth map from animated entity model quads. */
public final class EntityShadowMap {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static int size;
    private static final float COVERAGE = 48F;
    // Match the directional entity-shadow reach in lighting.glsl.
    private static final double DIRECTIONAL_REACH = 24.0;
    private static final int TILE_SIZE = 32;
    private static final int MAX_SPARSE_UPLOADS = 16;
    private static float pixelsPerBlock;
    private static final double RANGE_SQUARED = 32 * 32;
    private static final int MAX_ENTITIES = 12;
    private static final int MAX_MODEL_QUADS = 2048;
    private static final int MAX_TEXTURE_MASKS = 64;
    private static final int MAX_TEXTURE_PIXELS = 256 * 256;
    private static final float EMPTY = -10_000F;
    private static FloatBuffer upload;
    private static float[] depth;
    private static int tilesPerSide;
    private static final BitSet DEPTH_TILES = new BitSet();
    private static final BitSet UPLOADED_TILES = new BitSet();
    private static final BitSet DIRTY_TILES = new BitSet();
    private static final int ROW = 16;
    private static final FloatBuffer MODELS = BufferUtils.createFloatBuffer(ROW * (MAX_ENTITIES + MAX_MODEL_QUADS));
    private static final Entity[] NEAREST = new Entity[MAX_ENTITIES];
    private static final double[] DISTANCES = new double[MAX_ENTITIES];
    private static final Set<Integer> CASTERS = new HashSet<>();
    private static final Set<Class<?>> UNSUPPORTED_RENDERERS = new HashSet<>();
    private static final java.util.Map<ResourceLocation, AlphaMask> MASKS =
            new LinkedHashMap<>(MAX_TEXTURE_MASKS, 0.75F, true);
    private static final java.util.Map<RenderType, ResourceLocation> LAYERS = new HashMap<>();
    private static final AlphaMask OPAQUE = new AlphaMask(0, 0, null);
    private static final GeometrySink SINK = new GeometrySink();
    private static ClientLevel currentLevel;
    private static Map current;
    private static int texture;
    private static int modelsTexture;
    private static boolean maskLoadedThisFrame;

    private EntityShadowMap() {
    }

    public record Map(int texture, Vec3 origin, float sunX, float sunY, int models, int casters) {
    }

    public static Map current(ClientLevel level) {
        return currentLevel == level ? current : null;
    }

    public static boolean replacesBlob(Entity entity) {
        return current != null && entity.level() == currentLevel && CASTERS.contains(entity.getId());
    }

    public static void clear() {
        current = null;
        currentLevel = null;
        CASTERS.clear();
        Arrays.fill(NEAREST, null);
        if (texture != 0) {
            GL11.glDeleteTextures(texture);
            texture = 0;
        }
        if (modelsTexture != 0) {
            GL11.glDeleteTextures(modelsTexture);
            modelsTexture = 0;
        }
        DEPTH_TILES.clear();
        UPLOADED_TILES.clear();
        DIRTY_TILES.clear();
        size = 0;
        depth = null;
        upload = null;
        clearTextureMasks();
    }

    public static void clearTextureMasks() {
        MASKS.clear();
        LAYERS.clear();
    }

    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) {
            return;
        }
        current = null;
        CASTERS.clear();
        var client = Minecraft.getInstance();
        var level = client.level;
        if (level == null || !client.options.entityShadows().get() || TerrainLighting.currentWorld(level) == null) {
            return;
        }
        var sun = TerrainLighting.celestial(level, event.getPartialTick());
        boolean directional = sun.strength() > 0.01F && sun.y() > 0.10F;
        boolean local = TerrainLighting.hasBlockLights(level);
        if (!directional && !local) {
            return;
        }
        var camera = event.getCamera().getPosition();
        int count = nearest(level, camera);
        if (count == 0) {
            return;
        }
        try {
            int requested = LightingSettings.shadowResolution();
            if (requested != size) {
                if (texture != 0) {
                    GL11.glDeleteTextures(texture);
                    texture = 0;
                }
                size = requested;
                pixelsPerBlock = size / COVERAGE;
                depth = new float[size * size];
                Arrays.fill(depth, EMPTY);
                upload = BufferUtils.createFloatBuffer(size * size);
                tilesPerSide = Mth.positiveCeilDiv(size, TILE_SIZE);
                DEPTH_TILES.clear();
                UPLOADED_TILES.clear();
            }
            // Snapping in light space keeps depth texels on the same world grid as the camera moves.
            double texel = 1.0 / pixelsPerBlock;
            double lateral = camera.x * sun.y() - camera.y * sun.x();
            double cameraDepth = camera.x * sun.x() + camera.y * sun.y();
            lateral = Math.floor(lateral / texel) * texel;
            var origin = new Vec3(lateral * sun.y() + cameraDepth * sun.x(), -lateral * sun.x() + cameraDepth * sun.y(),
                    Math.floor(camera.z / texel) * texel);
            SINK.begin(sun.x(), sun.y(), directional, local);
            if (directional) {
                clearDepthTiles();
            }
            LAYERS.clear();
            maskLoadedThisFrame = false;
            Frustum frustum = event.getFrustum();
            for (int index = 0; index < count && SINK.hasCapacity(); index++) {
                capture(client, NEAREST[index], origin, event.getPartialTick(), frustum);
            }
            if (CASTERS.isEmpty() && SINK.casters == 0) {
                return;
            }
            boolean hasDirectionalDepth = !DEPTH_TILES.isEmpty() && directional;
            uploadDepth(hasDirectionalDepth);
            currentLevel = level;
            current = new Map(hasDirectionalDepth ? texture : 0, origin, sun.x(), sun.y(), modelsTexture, SINK.casters);
        } finally {
            Arrays.fill(NEAREST, 0, count, null);
        }
    }

    private static int nearest(ClientLevel level, Vec3 camera) {
        int count = 0;
        for (var entity : level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity) || !entity.isAlive() || entity.isInvisible() || entity.isSpectator()) {
                continue;
            }
            double distance = entity.distanceToSqr(camera);
            if (distance > RANGE_SQUARED || count == MAX_ENTITIES && distance >= DISTANCES[count - 1]) {
                continue;
            }
            int index = Math.min(count, MAX_ENTITIES - 1);
            while (index > 0 && distance < DISTANCES[index - 1]) {
                NEAREST[index] = NEAREST[index - 1];
                DISTANCES[index] = DISTANCES[index - 1];
                index--;
            }
            NEAREST[index] = entity;
            DISTANCES[index] = distance;
            count = Math.min(count + 1, MAX_ENTITIES);
        }
        return count;
    }

    private static <T extends Entity> void capture(Minecraft client, T entity, Vec3 origin, float partialTick,
            Frustum frustum) {
        var dispatcher = client.getEntityRenderDispatcher();
        EntityRenderer<? super T> renderer = dispatcher.getRenderer(entity);
        Class<?> rendererClass = renderer.getClass();
        if (UNSUPPORTED_RENDERERS.contains(rendererClass) || !(renderer instanceof PlayerRenderer
                || rendererClass.getPackageName().equals("net.minecraft.client.renderer.entity"))) {
            return;
        }
        var position = entity.getPosition(partialTick).add(renderer.getRenderOffset(entity, partialTick)).subtract(origin);
        var poses = new PoseStack();
        poses.translate(position.x, position.y, position.z);
        int first = SINK.quads;
        SINK.beginCaster();
        try {
            // Calling the model renderer directly bypasses the dispatcher's blob, fire and hitbox rendering.
            renderer.render(entity, entity.getViewYRot(partialTick), partialTick, poses, SINK,
                    dispatcher.getPackedLightCoords(entity, partialTick));
        } catch (RuntimeException exception) {
            UNSUPPORTED_RENDERERS.add(rendererClass);
            LOGGER.warn("Could not capture entity shadow vertices from {}", rendererClass.getName(), exception);
        }
        if (SINK.endCaster(first, origin, frustum)) {
            CASTERS.add(entity.getId());
        }
    }

    private static void uploadDepth(boolean directional) {
        int active = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        // Upload on the caller's active unit and restore it before the terrain draw.
        int previous = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        int unpack = GL11.glGetInteger(GL21.GL_PIXEL_UNPACK_BUFFER_BINDING);
        int alignment = GL11.glGetInteger(GL11.GL_UNPACK_ALIGNMENT);
        int rowLength = GL11.glGetInteger(GL11.GL_UNPACK_ROW_LENGTH);
        int skipRows = GL11.glGetInteger(GL11.GL_UNPACK_SKIP_ROWS);
        int skipPixels = GL11.glGetInteger(GL11.GL_UNPACK_SKIP_PIXELS);
        int swapBytes = GL11.glGetInteger(GL11.GL_UNPACK_SWAP_BYTES);
        try {
            GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER, 0);
            GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 4);
            GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, 0);
            GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_ROWS, 0);
            GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_PIXELS, 0);
            GL11.glPixelStorei(GL11.GL_UNPACK_SWAP_BYTES, 0);
            if (SINK.casters > 0) {
                if (modelsTexture == 0) {
                    modelsTexture = GL11.glGenTextures();
                    GL11.glBindTexture(GL11.GL_TEXTURE_2D, modelsTexture);
                    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
                    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
                    GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_RGBA32F, 4, MAX_ENTITIES + MAX_MODEL_QUADS,
                            0, GL11.GL_RGBA, GL11.GL_FLOAT, (FloatBuffer) null);
                } else {
                    GL11.glBindTexture(GL11.GL_TEXTURE_2D, modelsTexture);
                }
                int rows = MAX_ENTITIES + SINK.quads;
                MODELS.position(0).limit(rows * ROW);
                GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, 4, rows, GL11.GL_RGBA, GL11.GL_FLOAT, MODELS);
                MODELS.clear();
            }
            if (!directional) {
                return;
            }
            if (texture == 0) {
                texture = GL11.glGenTextures();
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
                upload.clear().put(depth).flip();
                GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_R32F, size, size, 0,
                        GL11.GL_RED, GL11.GL_FLOAT, upload);
            } else {
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
                uploadDirtyDepthTiles();
            }
            UPLOADED_TILES.clear();
            UPLOADED_TILES.or(DEPTH_TILES);
        } finally {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, previous);
            GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, alignment);
            GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, rowLength);
            GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_ROWS, skipRows);
            GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_PIXELS, skipPixels);
            GL11.glPixelStorei(GL11.GL_UNPACK_SWAP_BYTES, swapBytes);
            GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER, unpack);
            GL13.glActiveTexture(active);
        }
    }

    private static int tileRunEnd(BitSet tiles, int first) {
        int rowEnd = (first / tilesPerSide + 1) * tilesPerSide;
        return Math.min(tiles.nextClearBit(first), rowEnd);
    }

    private static void clearDepthTiles() {
        int after;
        for (int first = DEPTH_TILES.nextSetBit(0); first >= 0; first = DEPTH_TILES.nextSetBit(after)) {
            after = tileRunEnd(DEPTH_TILES, first);
            int x = first % tilesPerSide * TILE_SIZE;
            int y = first / tilesPerSide * TILE_SIZE;
            int right = Math.min(size, x + (after - first) * TILE_SIZE);
            for (int row = y; row < Math.min(size, y + TILE_SIZE); row++) {
                Arrays.fill(depth, row * size + x, row * size + right, EMPTY);
            }
        }
        DEPTH_TILES.clear();
    }

    private static void uploadDirtyDepthTiles() {
        DIRTY_TILES.clear();
        DIRTY_TILES.or(UPLOADED_TILES);
        DIRTY_TILES.or(DEPTH_TILES);
        int after;
        int uploads = 0;
        for (int first = DIRTY_TILES.nextSetBit(0); first >= 0; first = DIRTY_TILES.nextSetBit(after)) {
            if (++uploads > MAX_SPARSE_UPLOADS) {
                upload.clear().put(depth).flip();
                GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, size, size, GL11.GL_RED, GL11.GL_FLOAT, upload);
                return;
            }
            after = tileRunEnd(DIRTY_TILES, first);
        }
        for (int first = DIRTY_TILES.nextSetBit(0); first >= 0; first = DIRTY_TILES.nextSetBit(after)) {
            after = tileRunEnd(DIRTY_TILES, first);
            int x = first % tilesPerSide * TILE_SIZE;
            int y = first / tilesPerSide * TILE_SIZE;
            int width = Math.min(size - x, (after - first) * TILE_SIZE);
            int height = Math.min(size - y, TILE_SIZE);
            upload.clear();
            for (int row = y; row < y + height; row++) {
                upload.put(depth, row * size + x, width);
            }
            upload.flip();
            GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, x, y, width, height, GL11.GL_RED, GL11.GL_FLOAT, upload);
        }
    }

    private static void markDepthTiles(int minX, int minY, int maxX, int maxY) {
        for (int row = minY / TILE_SIZE; row <= maxY / TILE_SIZE; row++) {
            int base = row * tilesPerSide;
            DEPTH_TILES.set(base + minX / TILE_SIZE, base + maxX / TILE_SIZE + 1);
        }
    }

    private static ResourceLocation cutoutTexture(RenderType type) {
        String description = type.toString();
        if (!(description.startsWith("RenderType[entity_solid:")
                || description.startsWith("RenderType[entity_cutout:")
                || description.startsWith("RenderType[entity_cutout_no_cull:")
                || description.startsWith("RenderType[entity_cutout_no_cull_z_offset:")
                || description.startsWith("RenderType[entity_translucent:")
                || description.startsWith("RenderType[entity_translucent_cull:")
                || description.startsWith("RenderType[armor_cutout_no_cull:"))) {
            return null;
        }
        // TextureStateShard describes a single texture as Optional[namespace:path](blur=...).
        final String prefix = "texture[Optional[";
        int start = description.indexOf(prefix);
        int end = start < 0 ? -1 : description.indexOf(']', start + prefix.length());
        return end < 0 ? null : ResourceLocation.tryParse(description.substring(start + prefix.length(), end));
    }

    private static AlphaMask alphaMask(ResourceLocation id) {
        AlphaMask mask = MASKS.get(id);
        if (mask != null) {
            return mask;
        }
        if (maskLoadedThisFrame) {
            return OPAQUE;
        }
        maskLoadedThisFrame = true;
        mask = OPAQUE;
        var resource = Minecraft.getInstance().getResourceManager().getResource(id);
        if (resource.isPresent()) {
            try (var input = resource.orElseThrow().open(); var image = NativeImage.read(input)) {
                int width = image.getWidth();
                int height = image.getHeight();
                if ((long) width * height <= MAX_TEXTURE_PIXELS) {
                    var opacity = new byte[width * height];
                    boolean opaque = true;
                    for (int y = 0; y < height; y++) {
                        for (int x = 0; x < width; x++) {
                            int alpha = image.getPixelRGBA(x, y) >>> 24;
                            opacity[y * width + x] = (byte) alpha;
                            opaque &= alpha == 255;
                        }
                    }
                    if (!opaque) {
                        mask = new AlphaMask(width, height, opacity);
                    }
                }
            } catch (IOException | RuntimeException exception) {
                LOGGER.debug("Could not read entity shadow texture {}", id, exception);
            }
        }
        if (MASKS.size() >= MAX_TEXTURE_MASKS) {
            Entry<ResourceLocation, AlphaMask> oldest = MASKS.entrySet().iterator().next();
            MASKS.remove(oldest.getKey());
        }
        MASKS.put(id, mask);
        return mask;
    }

    private record AlphaMask(int width, int height, byte[] opacity) {
        boolean visible(float u, float v, float alpha) {
            if (opacity == null) {
                return alpha >= 0.10F;
            }
            int x = Math.floorMod(Mth.floor(u * width), width);
            int y = Math.floorMod(Mth.floor(v * height), height);
            return Byte.toUnsignedInt(opacity[y * width + x]) * alpha >= 25.5F;
        }
    }

    private static final class GeometrySink implements MultiBufferSource, VertexConsumer {
        private static final VertexConsumer DISCARD = new VertexConsumer() {
            @Override public VertexConsumer vertex(double x, double y, double z) { return this; }
            @Override public VertexConsumer color(int r, int g, int b, int a) { return this; }
            @Override public VertexConsumer uv(float u, float v) { return this; }
            @Override public VertexConsumer overlayCoords(int u, int v) { return this; }
            @Override public VertexConsumer uv2(int u, int v) { return this; }
            @Override public VertexConsumer normal(float x, float y, float z) { return this; }
            @Override public void endVertex() {}
            @Override public void defaultColor(int r, int g, int b, int a) {}
            @Override public void unsetDefaultColor() {}
        };
        private final float[] x = new float[4];
        private final float[] y = new float[4];
        private final float[] z = new float[4];
        private final float[] u = new float[4];
        private final float[] v = new float[4];
        private final float[] alpha = new float[4];
        private final float[][] projectedComponents = {x, y, z, u, v, alpha};
        private final float[] projectedQuads = new float[MAX_MODEL_QUADS * 24];
        private final AlphaMask[] projectedMasks = new AlphaMask[MAX_MODEL_QUADS];
        private final float[] vertices = new float[12];
        private final float[] min = new float[3];
        private final float[] max = new float[3];
        private int casters;
        private boolean directional;
        private boolean local;
        private float sunX;
        private float sunY;
        private float defaultAlpha = 1F;
        private int slot;
        private int quads;
        private AlphaMask mask = OPAQUE;

        void begin(float x, float y, boolean directional, boolean local) {
            sunX = x;
            sunY = y;
            quads = 0;
            casters = 0;
            this.directional = directional;
            this.local = local;
        }

        void beginCaster() {
            Arrays.fill(min, Float.POSITIVE_INFINITY);
            Arrays.fill(max, Float.NEGATIVE_INFINITY);
        }

        boolean endCaster(int first, Vec3 origin, Frustum frustum) {
            if (first == quads) {
                return false;
            }
            if (local) {
                int row = casters++ * ROW;
                for (int axis = 0; axis < 3; axis++) {
                    MODELS.put(row + axis, min[axis]).put(row + 4 + axis, max[axis]);
                }
                MODELS.put(row + 3, MAX_ENTITIES + first).put(row + 7, MAX_ENTITIES + quads);
            }

            boolean drawn = false;
            if (directional) {
                if (frustum == null || shadowCanReachView(origin, frustum)) {
                    for (int quad = first; quad < quads; quad++) {
                        loadProjectedQuad(quad);
                        drawn |= rasterize(0, 1, 2) | rasterize(0, 2, 3);
                    }
                }
                Arrays.fill(projectedMasks, first, quads, null);
            }
            return local || drawn;
        }

        private boolean shadowCanReachView(Vec3 origin, Frustum frustum) {
            // The receiver can be visible when the caster itself is off-screen.
            double texelMargin = 2.0 / pixelsPerBlock;
            AABB receivers = new AABB(min[0] + origin.x, min[1] + origin.y, min[2] + origin.z,
                    max[0] + origin.x, max[1] + origin.y, max[2] + origin.z)
                    .expandTowards(-sunX * DIRECTIONAL_REACH, -sunY * DIRECTIONAL_REACH, 0)
                    .inflate(texelMargin);
            return frustum.isVisible(receivers);
        }

        private void storeProjectedQuad(int quad) {
            int base = quad * 24;
            for (int i = 0; i < projectedComponents.length; i++) {
                System.arraycopy(projectedComponents[i], 0, projectedQuads, base + i * 4, 4);
            }
            projectedMasks[quad] = mask;
        }

        private void loadProjectedQuad(int quad) {
            int base = quad * 24;
            for (int i = 0; i < projectedComponents.length; i++) {
                System.arraycopy(projectedQuads, base + i * 4, projectedComponents[i], 0, 4);
            }
            mask = projectedMasks[quad];
        }

        boolean hasCapacity() {
            return quads < MAX_MODEL_QUADS;
        }

        @Override
        public VertexConsumer getBuffer(RenderType type) {
            if (type.mode() != VertexFormat.Mode.QUADS || !hasCapacity()) {
                return DISCARD;
            }
            if (!LAYERS.containsKey(type)) {
                LAYERS.put(type, cutoutTexture(type));
            }
            var texture = LAYERS.get(type);
            if (texture == null) {
                return DISCARD;
            }
            mask = directional ? alphaMask(texture) : OPAQUE;
            defaultAlpha = 1F;
            slot = 0;
            return this;
        }

        @Override
        public VertexConsumer vertex(double worldX, double worldY, double worldZ) {
            vertices[slot * 3] = (float) worldX;
            vertices[slot * 3 + 1] = (float) worldY;
            vertices[slot * 3 + 2] = (float) worldZ;
            if (directional) {
                x[slot] = (float) ((worldX * sunY - worldY * sunX) * pixelsPerBlock + size * 0.5F);
                y[slot] = (float) (worldZ * pixelsPerBlock + size * 0.5F);
                z[slot] = (float) (worldX * sunX + worldY * sunY);
                u[slot] = 0F;
                v[slot] = 0F;
                alpha[slot] = defaultAlpha;
            }
            return this;
        }

        @Override public VertexConsumer color(int r, int g, int b, int a) { alpha[slot] = a / 255F; return this; }
        @Override public VertexConsumer uv(float texU, float texV) { u[slot] = texU; v[slot] = texV; return this; }
        @Override public VertexConsumer overlayCoords(int u, int v) { return this; }
        @Override public VertexConsumer uv2(int u, int v) { return this; }
        @Override public VertexConsumer normal(float x, float y, float z) { return this; }
        @Override public void defaultColor(int r, int g, int b, int a) { defaultAlpha = a / 255F; }
        @Override public void unsetDefaultColor() { defaultAlpha = 1F; }

        @Override
        public void endVertex() {
            slot++;
            if (slot != 4) {
                return;
            }
            slot = 0;
            if (hasCapacity()) {
                int row = (MAX_ENTITIES + quads) * ROW;
                for (int vertex = 0; vertex < 4; vertex++) {
                    for (int axis = 0; axis < 3; axis++) {
                        float value = vertices[vertex * 3 + axis];
                        min[axis] = Math.min(min[axis], value);
                        max[axis] = Math.max(max[axis], value);
                        if (local) {
                            MODELS.put(row + vertex * 4 + axis, value);
                        }
                    }
                }
                if (directional) {
                    storeProjectedQuad(quads);
                }
                quads++;
            }
        }

        private boolean rasterize(int a, int b, int c) {
            float bx = x[b] - x[a];
            float by = y[b] - y[a];
            float cx = x[c] - x[a];
            float cy = y[c] - y[a];
            float determinant = bx * cy - by * cx;
            if (Math.abs(determinant) < 0.0001F) {
                return false;
            }
            float reciprocal = 1F / determinant;
            int minX = Math.max(0, Mth.floor(Math.min(x[a], Math.min(x[b], x[c]))));
            int minY = Math.max(0, Mth.floor(Math.min(y[a], Math.min(y[b], y[c]))));
            int maxX = Math.min(size - 1, Mth.floor(Math.max(x[a], Math.max(x[b], x[c]))));
            int maxY = Math.min(size - 1, Mth.floor(Math.max(y[a], Math.max(y[b], y[c]))));
            boolean drawn = false;
            boolean opaque = mask == OPAQUE && alpha[a] == 1F && alpha[b] == 1F && alpha[c] == 1F;
            for (int py = minY; py <= maxY; py++) {
                float dy = py + 0.5F - y[a];
                float rowB = dy * cx;
                float rowC = bx * dy;
                for (int px = minX; px <= maxX; px++) {
                    float dx = px + 0.5F - x[a];
                    float wb = (dx * cy - rowB) * reciprocal;
                    float wc = (rowC - by * dx) * reciprocal;
                    float wa = 1F - wb - wc;
                    if (wa < 0 || wb < 0 || wc < 0) {
                        continue;
                    }
                    float sample = wa * z[a] + wb * z[b] + wc * z[c];
                    int index = py * size + px;
                    if (sample > depth[index] && (opaque || mask.visible(wa * u[a] + wb * u[b] + wc * u[c],
                            wa * v[a] + wb * v[b] + wc * v[c], wa * alpha[a] + wb * alpha[b] + wc * alpha[c]))) {
                        depth[index] = sample;
                        drawn = true;
                    }
                }
            }
            if (drawn) {
                markDepthTiles(minX, minY, maxX, maxY);
            }
            return drawn;
        }
    }
}
