package dev.taste.engine.render.backend;

import dev.taste.engine.material.MaterialGeneration;
import java.util.List;

/** Backend boundary. Optional backend classes never enter common signatures. */
public interface TerrainBackend extends AutoCloseable {
    record LayerStamp(String layer, long frame, long layoutGeneration, int drawCalls) { }
    String id();
    boolean available();
    void activate(MaterialGeneration.Snapshot generation);
    void beginFrame(long frame);
    void beginLayer(String layer);
    void endLayer(String layer);
    List<LayerStamp> stamps();
    void deactivate();
    @Override void close();
}
