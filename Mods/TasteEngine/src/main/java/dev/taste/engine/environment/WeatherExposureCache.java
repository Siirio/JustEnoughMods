package dev.taste.engine.environment;

import java.util.LinkedHashMap;
import java.util.Map;

/** Roof and shape exposure cache with explicit expiry. */
public final class WeatherExposureCache {
    public record Entry(boolean exposed, long checkedTick) { }
    private final int capacity;
    private final Map<Long, Entry> entries = new LinkedHashMap<>();
    public WeatherExposureCache(int capacity) { this.capacity = Math.max(1, capacity); }
    public synchronized Entry get(long column, long currentTick, long maximumAge) {
        Entry entry = entries.get(column);
        return entry != null && currentTick - entry.checkedTick() <= maximumAge ? entry : null;
    }
    public synchronized void put(long column, Entry entry) {
        entries.remove(column); entries.put(column, entry);
        while (entries.size() > capacity) entries.remove(entries.keySet().iterator().next());
    }
    public synchronized void invalidate(long column) { entries.remove(column); }
    public synchronized void clear() { entries.clear(); }
}
