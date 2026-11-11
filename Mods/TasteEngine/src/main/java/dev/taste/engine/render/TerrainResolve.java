package dev.taste.engine.render;

import dev.taste.engine.render.backend.TerrainBackend;
import java.util.LinkedHashMap;
import java.util.Map;

/** Keeps the latest non-empty resolve per layer and rejects stale layout generations. */
public final class TerrainResolve {
    private final Map<String, TerrainBackend.LayerStamp> resolved = new LinkedHashMap<>();
    public void accept(TerrainBackend.LayerStamp stamp, long activeGeneration) {
        if (stamp.layoutGeneration() != activeGeneration || stamp.drawCalls() <= 0) return;
        TerrainBackend.LayerStamp prior = resolved.get(stamp.layer());
        if (prior == null || stamp.frame() >= prior.frame()) resolved.put(stamp.layer(), stamp);
    }
    public Map<String, TerrainBackend.LayerStamp> snapshot() { return Map.copyOf(resolved); }
    public void beginFrame() { resolved.clear(); }
}
