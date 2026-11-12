package dev.taste.engine.light;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexBuffer;
import dev.taste.engine.api.LightProvider.Light;
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
    private static final int MAX_CASCADES = 4;
    private static final String POINT_TARGET = "point-shadow-atlas";
    private static final float POINT_NEAR = 0.72F;
    private static final int[][] POINT_FACES = {
            {1, 0, 0, 0, -1, 0}, {-1, 0, 0, 0, -1, 0},
            {0, 1, 0, 0, 0, 1}, {0, -1, 0, 0, 0, -1},
            {0, 0, 1, 0, -1, 0}, {0, 0, -1, 0, -1, 0}
    };
    private final CascadeController cascades = new CascadeController();
    private ShaderInstance shader;
    private PointKey pointKey;
    private PointMaps cachedPointMaps;
    private long pointFrame;

    public List<MapLayer> draw(TerrainBackend terrain, TargetPool targets, FrameSnapshot frame,
            Vector3f towardsSun, int resolution, int count, float distance, float splitLambda) throws IOException {
        CascadeController.Cascade[] splits = cascades.splits(0.05F, distance, count, splitLambda, resolution);
        List<MapLayer> result = new ArrayList<>(count);
        releaseFrom(targets, count);
        RenderSystem.disableScissor();
        BufferUploader.reset();
        for (int index = 0; index < count; index++) {
            CascadeController.LightView view = cascades.fit(splits[index], frame, towardsSun, resolution);
            TextureTarget map = targets.acquire(new TargetDescriptor(targetName(index), resolution,
                    resolution, TargetDescriptor.Format.RGBA8, true, 1, 1, 1));
            RenderSystem.depthMask(true);
            map.clear(Minecraft.ON_OSX);
            map.bindWrite(true);
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(GL11.GL_LEQUAL);
            RenderSystem.disableCull();
            RenderSystem.disableBlend();
            RenderSystem.colorMask(false, false, false, false);
            try {
                int submissions = drawCasters(terrain, view.projection(), frame.origin());
                result.add(new MapLayer(view, map.getDepthTextureId(), resolution, submissions));
            } finally {
                RenderSystem.colorMask(true, true, true, true);
            }
        }
        return List.copyOf(result);
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
        for (int index = count; index < MAX_CASCADES; index++) targets.release(targetName(index));
    }

    private static String targetName(int index) { return "sun-shadow-" + index; }

    private Uniform requireUniform(String name) {
        Uniform value = shader.getUniform(name);
        if (value == null) throw new IllegalStateException("Missing shadow uniform " + name);
        return value;
    }

    @Override
    public void close() {
        invalidatePointMaps();
        if (shader != null) {
            shader.close();
            shader = null;
        }
    }
}
