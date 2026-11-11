package dev.taste.engine.render.reflection;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ProbeCache {
    public record Probe(long key, double x, double y, double z, long geometryGeneration, long updatedFrame, int cubeTexture) { }
    private final int capacity;
    private final Map<Long, Probe> probes = new LinkedHashMap<>();
    public ProbeCache(int capacity) { this.capacity = Math.max(1, capacity); }
    public synchronized void put(Probe probe) { probes.remove(probe.key()); probes.put(probe.key(), probe); while (probes.size() > capacity) probes.remove(probes.keySet().iterator().next()); }
    public synchronized Probe get(long key, long generation) { Probe probe = probes.get(key); return probe != null && probe.geometryGeneration() == generation ? probe : null; }
    public synchronized void invalidateGeneration(long generation) { probes.values().removeIf(probe -> probe.geometryGeneration() != generation); }
    public synchronized Map<Long, Probe> snapshot() { return Map.copyOf(probes); }
}
