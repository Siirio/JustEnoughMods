package dev.taste.engine.effects.glass;

import java.util.LinkedHashMap;
import java.util.Map;

public final class GlassPatchPool {
    private final int capacity;
    private final Map<GlassPatchKey, DropletSimulation> patches = new LinkedHashMap<>();
    public GlassPatchPool(int capacity) { this.capacity = Math.max(1, capacity); }
    public synchronized DropletSimulation patch(GlassPatchKey key) {
        DropletSimulation simulation = patches.remove(key);
        if (simulation == null) simulation = new DropletSimulation(key.hashCode());
        patches.put(key, simulation);
        while (patches.size() > capacity) patches.remove(patches.keySet().iterator().next());
        return simulation;
    }
    public synchronized void retain(java.util.Set<GlassPatchKey> visible) { patches.keySet().removeIf(key -> !visible.contains(key)); }
}
