package dev.taste.engine.environment;

import java.util.LinkedHashMap;
import java.util.Map;

public final class WetnessField {
    private final int capacity;
    private final Map<Long, Float> cells = new LinkedHashMap<>();
    public WetnessField(int capacity) { this.capacity = Math.max(1, capacity); }
    public synchronized float update(long cell, boolean exposed, float rain, float deltaSeconds, float wetSeconds, float drySeconds) {
        float value = cells.getOrDefault(cell, 0.0F);
        if (exposed && rain > 0.0F) value += deltaSeconds / Math.max(0.1F, wetSeconds) * rain;
        else value -= deltaSeconds / Math.max(0.1F, drySeconds);
        value = Math.max(0.0F, Math.min(1.0F, value));
        cells.remove(cell); cells.put(cell, value);
        while (cells.size() > capacity) cells.remove(cells.keySet().iterator().next());
        return value;
    }
    public synchronized void clear() { cells.clear(); }
}
