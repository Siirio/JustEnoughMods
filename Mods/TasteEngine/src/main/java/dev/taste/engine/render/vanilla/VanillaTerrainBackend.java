package dev.taste.engine.render.vanilla;

import com.mojang.blaze3d.vertex.VertexBuffer;
import dev.taste.engine.material.MaterialGeneration;
import dev.taste.engine.mixin.client.LevelRendererViewAreaAccessor;
import dev.taste.engine.render.FrameSnapshot;
import dev.taste.engine.render.backend.TerrainBackend;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher.RenderChunk;
import net.minecraft.core.BlockPos;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;

/** Vanilla layer owner with explicit generation stamps. */
public final class VanillaTerrainBackend implements TerrainBackend {
    @FunctionalInterface
    public interface ShadowDraw {
        void draw(VertexBuffer mesh, float x, float y, float z, float alphaCutoff);
    }

    private static final List<RenderType> SHADOW_LAYERS =
            List.of(RenderType.solid(), RenderType.cutoutMipped(), RenderType.cutout());
    private final List<LayerStamp> stamps = new ArrayList<>();
    private MaterialGeneration.Snapshot generation = new MaterialGeneration.Snapshot(0, 0);
    private long frame;
    private String layer;
    public String id() { return "vanilla"; }
    public boolean available() { return true; }
    public void activate(MaterialGeneration.Snapshot next) { generation = next; }
    public void beginFrame(long nextFrame) { frame = nextFrame; stamps.clear(); }
    public void beginLayer(String nextLayer) {
        if (layer != null) throw new IllegalStateException("Nested vanilla terrain layer");
        layer = nextLayer;
    }
    public void endLayer(String endedLayer) {
        if (!endedLayer.equals(layer)) throw new IllegalStateException("Terrain layer mismatch");
        stamps.add(new LayerStamp(layer, frame, generation.layout(), 1));
        layer = null;
    }
    public List<LayerStamp> stamps() { return List.copyOf(stamps); }
    public int drawShadowCasters(Matrix4f lightProjection, FrameSnapshot.Vec3d origin, ShadowDraw draw) {
        ViewArea area = ((LevelRendererViewAreaAccessor) Minecraft.getInstance().levelRenderer).taste$getViewArea();
        if (area == null) return 0;
        FrustumIntersection frustum = new FrustumIntersection(lightProjection);
        int draws = 0;
        for (RenderChunk section : area.chunks) {
            BlockPos position = section.getOrigin();
            float x = (float) (position.getX() - origin.x());
            float y = (float) (position.getY() - origin.y());
            float z = (float) (position.getZ() - origin.z());
            if (!frustum.testAab(x, y, z, x + 16, y + 16, z + 16)) continue;
            for (RenderType layer : SHADOW_LAYERS) {
                if (section.getCompiledChunk().isEmpty(layer)) continue;
                VertexBuffer mesh = section.getBuffer(layer);
                if (mesh == null || mesh.isInvalid()) continue;
                draw.draw(mesh, x, y, z, layer == RenderType.solid() ? 0 : 0.1F);
                draws++;
            }
        }
        return draws;
    }
    public void deactivate() { layer = null; stamps.clear(); }
    public void close() { deactivate(); }
}
