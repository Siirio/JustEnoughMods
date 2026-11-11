package dev.taste.engine.render.vanilla;

import dev.taste.engine.material.MaterialGeneration;
import dev.taste.engine.render.backend.TerrainBackend;
import java.util.ArrayList;
import java.util.List;

/** Vanilla layer owner with explicit generation stamps. */
public final class VanillaTerrainBackend implements TerrainBackend {
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
    public void deactivate() { layer = null; stamps.clear(); }
    public void close() { deactivate(); }
}
