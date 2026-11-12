package dev.taste.engine.light;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexBuffer;
import dev.taste.engine.api.LightProvider.Light;
import dev.taste.engine.effects.VegetationMotion;
import dev.taste.engine.render.FrameSnapshot;
import dev.taste.engine.render.TargetDescriptor;
import dev.taste.engine.render.TargetPool;
import dev.taste.engine.render.backend.TerrainBackend;
import dev.taste.engine.render.embeddium.Embeddium031Backend;
import dev.taste.engine.render.vanilla.VanillaTerrainBackend;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

/** Builds directional depth maps from terrain meshes in independent light views. */
public final class ShadowRenderer implements AutoCloseable {
    public record MapLayer(CascadeController.LightView view, int texture, int resolution, int submissions) { }
    public record PointMaps(int texture, int columns, int rows, int count) { }
    private record PointSource(double x, double y, double z, float range) { }
    private record PointKey(long world, int resolution, List<PointSource> sources) { }
    private record DirectionalKey(long world, long revision, FrameSnapshot.Vec3d origin, Matrix4f projection,
            boolean separate, int resolution, int actorPolicy) { }
    private record DirectionalCache(DirectionalKey key, int staticTexture, int dynamicTexture, long dynamicFrame,
            int staticDraws, int dynamicDraws) { }
    private static final int MAX_CASCADES = 4;
    private static final String POINT_TARGET = "point-shadow-atlas";
    private static final float POINT_NEAR = 0.72F;
    private static final int[][] POINT_FACES = {
            {1, 0, 0, 0, -1, 0}, {-1, 0, 0, 0, -1, 0},
            {0, 1, 0, 0, 0, 1}, {0, -1, 0, 0, 0, -1},
            {0, 0, 1, 0, -1, 0}, {0, 0, -1, 0, -1, 0}
    };
    private final CascadeController cascades = new CascadeController();
    private final ActorShadowPass actorCasters = new ActorShadowPass();
    private ShaderInstance shader;
    private PointKey pointKey;
    private PointMaps cachedPointMaps;
    private long pointFrame;
    private int actorPolicy;
    private final DirectionalCache[] directional = new DirectionalCache[MAX_CASCADES];

    public List<MapLayer> draw(TerrainBackend terrain, TargetPool targets, FrameSnapshot frame,
            Vector3f towardsSun, int resolution, int count, float distance, float splitLambda,
            boolean cacheStatic, int refreshFrames) throws IOException {
        CascadeController.Cascade[] splits = cascades.splits(0.05F, distance, count, splitLambda, resolution);
        List<MapLayer> result = new ArrayList<>(count);
        releaseFrom(targets, count);
        RenderSystem.disableScissor();
        BufferUploader.reset();
        boolean dynamic = actorCasters.hasActors();
        boolean canCache = cacheStatic && !VegetationMotion.affectsCasters();
        long revision = canCache ? terrain.casterRevision() : 0L;
        long layerBytes = (long) resolution * resolution * 8;
        long extraBudget = targets.limitBytes() / 7;
        Vector3f light = canCache ? stabilizedDirection(towardsSun) : towardsSun;
        for (int index = 0; index < count; index++) {
            CascadeController.LightView view = cascades.fit(splits[index], frame, light, resolution);
            TextureTarget map = target(targets, targetName(index), resolution);
            boolean separate = canCache && dynamic && layerBytes * (index + 1) <= extraBudget
                    && targets.limitBytes() - targets.allocatedBytes() - targets.externalBytes()
                        + targets.allocations().getOrDefault(staticName(index), 0L) >= layerBytes;
            if (!separate) targets.release(staticName(index));
            TextureTarget terrainMap = separate ? target(targets, staticName(index), resolution) : map;
            DirectionalKey key = new DirectionalKey(frame.worldEpoch(), revision, frame.origin(), view.projection(),
                    separate, resolution, actorPolicy);
            DirectionalCache previous = directional[index];
            boolean staticHit = canCache && (separate || !dynamic) && previous != null && previous.key().equals(key)
                    && previous.staticTexture() == terrainMap.getDepthTextureId();
            int terrainDraws = staticHit ? previous.staticDraws() : 0;
            int actorDraws = 0;
            long dynamicFrame = frame.frameIndex();
            try {
                if (!staticHit) {
                    bindDepth(terrainMap, true);
                    terrainDraws = drawCasters(terrain, view.projection(), frame.origin());
                }
                if (dynamic) {
                    boolean dynamicHit = staticHit && separate && previous.dynamicTexture() == map.getDepthTextureId()
                            && frame.frameIndex() - previous.dynamicFrame() < refreshFrames;
                    if (dynamicHit) {
                        dynamicFrame = previous.dynamicFrame();
                        actorDraws = previous.dynamicDraws();
                    } else {
                        if (separate) map.copyDepthFrom(terrainMap);
                        bindDepth(map, false);
                        actorDraws = actorCasters.draw(view.projection(), frame.origin());
                    }
                }
                directional[index] = canCache && (separate || !dynamic) ? new DirectionalCache(key,
                        terrainMap.getDepthTextureId(), map.getDepthTextureId(), dynamicFrame, terrainDraws, actorDraws) : null;
                result.add(new MapLayer(view, map.getDepthTextureId(), resolution, terrainDraws + actorDraws));
            } finally {
                RenderSystem.colorMask(true, true, true, true);
            }
        }
        return List.copyOf(result);
    }

    private static TextureTarget target(TargetPool pool, String name, int resolution) {
        return pool.acquire(new TargetDescriptor(name, resolution, resolution, TargetDescriptor.Format.RGBA8, true, 1, 1, 1));
    }

    private static void bindDepth(TextureTarget target, boolean clear) {
        RenderSystem.depthMask(true);
        if (clear) target.clear(Minecraft.ON_OSX);
        target.bindWrite(true);
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.disableCull();
        RenderSystem.disableBlend();
        RenderSystem.colorMask(false, false, false, false);
    }

    private static Vector3f stabilizedDirection(Vector3f direction) {
        float angle = Math.round(Math.atan2(direction.x, direction.y) * 1024) / 1024F;
        return new Vector3f((float) Math.sin(angle), (float) Math.cos(angle), 0);
    }

    public PointMaps drawPointMaps(TerrainBackend terrain, TargetPool targets, FrameSnapshot frame,
            List<Light> sources, int resolution, int count, int refreshFrames) throws IOException {
        int active = Math.min(count, sources.size());
        if (active == 0) {
            targets.release(POINT_TARGET);
            invalidatePointMaps();
            return null;
        }
        PointKey key = new PointKey(frame.worldEpoch(), resolution, sources.subList(0, active).stream()
                .map(source -> new PointSource(source.x(), source.y(), source.z(), source.range())).toList());
        if (key.equals(pointKey) && frame.frameIndex() - pointFrame < refreshFrames) return cachedPointMaps;
        FrameSnapshot.Vec3d origin = frame.origin();
        int maximumColumns = GL11.glGetInteger(GL11.GL_MAX_TEXTURE_SIZE) / resolution;
        int faceCount = active * POINT_FACES.length;
        int columns = Math.min(maximumColumns, (int) Math.ceil(Math.sqrt(faceCount)));
        if (columns <= 0) throw new IllegalArgumentException("Point shadow resolution exceeds the GPU limit");
        int rows = (faceCount + columns - 1) / columns;
        if (rows > maximumColumns) throw new IllegalArgumentException("Point shadow atlas exceeds the GPU limit");
        TextureTarget atlas = targets.acquire(new TargetDescriptor(POINT_TARGET, columns * resolution, rows * resolution,
                TargetDescriptor.Format.RGBA8, true, 1, 1, 1));
        RenderSystem.disableScissor();
        RenderSystem.depthMask(true);
        atlas.clear(Minecraft.ON_OSX);
        atlas.bindWrite(false);
        BufferUploader.reset();
        RenderSystem.colorMask(false, false, false, false);
        try {
            for (int index = 0; index < active; index++) {
                Light source = sources.get(index);
                Vector3f position = new Vector3f((float) (source.x() - origin.x()),
                        (float) (source.y() - origin.y()), (float) (source.z() - origin.z()));
                for (int face = 0; face < POINT_FACES.length; face++) {
                    int cell = index * POINT_FACES.length + face;
                    RenderSystem.viewport(cell % columns * resolution, cell / columns * resolution, resolution, resolution);
                    RenderSystem.enableDepthTest();
                    RenderSystem.depthFunc(GL11.GL_LEQUAL);
                    RenderSystem.depthMask(true);
                    RenderSystem.disableCull();
                    RenderSystem.disableBlend();
                    int[] axes = POINT_FACES[face];
                    Vector3f target = new Vector3f(position).add(axes[0], axes[1], axes[2]);
                    Matrix4f projection = new Matrix4f().perspective((float) (Math.PI * 0.5), 1, POINT_NEAR, source.range())
                            .lookAt(position, target, new Vector3f(axes[3], axes[4], axes[5]));
                    drawCasters(terrain, projection, origin);
                    actorCasters.draw(projection, origin);
                }
            }
        } finally {
            RenderSystem.colorMask(true, true, true, true);
        }
        pointKey = key;
        pointFrame = frame.frameIndex();
        cachedPointMaps = new PointMaps(atlas.getDepthTextureId(), columns, rows, active);
        return cachedPointMaps;
    }

    public void invalidatePointMaps() {
        pointKey = null;
        cachedPointMaps = null;
    }

    public void prepareActors(FrameSnapshot frame, float partialTick, boolean entities, boolean blockEntities) {
        actorPolicy = (entities ? 1 : 0) | (blockEntities ? 2 : 0);
        actorCasters.prepare(frame, partialTick, entities, blockEntities);
    }

    public boolean hasMovingCasters() { return actorCasters.hasActors(); }

    private int drawCasters(TerrainBackend terrain, Matrix4f projection, FrameSnapshot.Vec3d origin) throws IOException {
        if (terrain instanceof Embeddium031Backend embeddium) return embeddium.drawShadowCasters(projection, origin);
        if (!(terrain instanceof VanillaTerrainBackend vanilla))
            throw new IllegalStateException("Unsupported shadow terrain backend " + terrain.id());
        Minecraft minecraft = Minecraft.getInstance();
        if (shader == null) {
            shader = new ShaderInstance(minecraft.getResourceManager(),
                    new ResourceLocation("taste", "taste_shadow"), DefaultVertexFormat.BLOCK);
        }
        shader.setSampler("BlockAtlas", minecraft.getTextureManager().getTexture(TextureAtlas.LOCATION_BLOCKS).getId());
        requireUniform("LightProjection").set(projection);
        Uniform sectionOrigin = requireUniform("SectionOrigin");
        Uniform cutoff = requireUniform("AlphaCutoff");
        shader.apply();
        VegetationMotion.bind(shader.getId());
        try {
            return vanilla.drawShadowCasters(projection, origin, (mesh, x, y, z, alpha) -> {
                sectionOrigin.set(x, y, z);
                sectionOrigin.upload();
                cutoff.set(alpha);
                cutoff.upload();
                mesh.bind();
                mesh.draw();
            });
        } finally {
            VertexBuffer.unbind();
            shader.clear();
        }
    }

    public void releaseFrom(TargetPool targets, int count) {
        for (int index = count; index < MAX_CASCADES; index++) {
            targets.release(targetName(index));
            targets.release(staticName(index));
            directional[index] = null;
        }
    }

    private static String targetName(int index) { return "sun-shadow-" + index; }
    private static String staticName(int index) { return "sun-shadow-static-" + index; }

    private Uniform requireUniform(String name) {
        Uniform value = shader.getUniform(name);
        if (value == null) throw new IllegalStateException("Missing shadow uniform " + name);
        return value;
    }

    @Override
    public void close() {
        actorCasters.clear();
        invalidatePointMaps();
        java.util.Arrays.fill(directional, null);
        if (shader != null) {
            shader.close();
            shader = null;
        }
    }
}
