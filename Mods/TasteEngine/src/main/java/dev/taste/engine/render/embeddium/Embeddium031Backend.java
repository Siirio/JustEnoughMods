package dev.taste.engine.render.embeddium;

import dev.taste.engine.material.MaterialGeneration;
import dev.taste.engine.render.FrameSnapshot;
import dev.taste.engine.render.backend.TerrainBackend;
import java.util.ArrayList;
import java.util.List;
import org.joml.Matrix4f;

/** Exact-version adapter with no static Embeddium type references. */
public final class Embeddium031Backend implements TerrainBackend {
    private final boolean available;
    private final List<LayerStamp> stamps = new ArrayList<>();
    private MaterialGeneration.Snapshot generation = new MaterialGeneration.Snapshot(0, 0);
    private long frame;
    private String layer;
    private EmbeddiumCaptureRenderer capture;
    public Embeddium031Backend(String installedVersion) { available = installedVersion != null && installedVersion.startsWith("0.3.31"); }
    public String id() { return "embeddium-0.3.31"; }
    public boolean available() { return available; }
    public void activate(MaterialGeneration.Snapshot next) {
        if (!available) throw new IllegalStateException("Embeddium 0.3.31 is not installed");
        generation = next;
    }
    public void beginFrame(long nextFrame) { frame = nextFrame; stamps.clear(); }
    public void beginLayer(String nextLayer) { if (layer != null) throw new IllegalStateException("Nested Embeddium layer"); layer = nextLayer; }
    public void endLayer(String endedLayer) {
        if (!endedLayer.equals(layer)) throw new IllegalStateException("Terrain layer mismatch");
        stamps.add(new LayerStamp(layer, frame, generation.layout(), 1));
        layer = null;
    }
    public List<LayerStamp> stamps() { return List.copyOf(stamps); }
    public int drawShadowCasters(Matrix4f projection, FrameSnapshot.Vec3d origin) {
        if (!available) throw new IllegalStateException("Embeddium 0.3.31 is not installed");
        if (capture == null) capture = new EmbeddiumCaptureRenderer();
        return capture.draw(projection, origin);
    }
    public void deactivate() { layer = null; stamps.clear(); }
    public void close() {
        try {
            if (capture != null) capture.close();
        } finally {
            capture = null;
            deactivate();
        }
    }
}
