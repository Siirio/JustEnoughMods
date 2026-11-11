package dev.taste.engine.light;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ShadowCache {
    public record Key(int cascade, long region) { }
    public record Entry(long geometryGeneration, long transformHash, long lastFrame, int textureLayer) { }
    private final Map<Key, Entry> entries = new LinkedHashMap<>();
    public Entry get(Key key, long geometryGeneration, long transformHash) {
        Entry entry = entries.get(key);
        return entry != null && entry.geometryGeneration() == geometryGeneration && entry.transformHash() == transformHash ? entry : null;
    }
    public void put(Key key, Entry entry) { entries.put(key, entry); }
    public void invalidateRegion(long region) { entries.keySet().removeIf(key -> key.region() == region); }
    public void clear() { entries.clear(); }
}
