package dev.taste.engine.light;

import java.util.LinkedHashMap;
import java.util.Map;

/** Stable shadow slots with least-recently-used replacement. */
public final class LocalShadowAtlas {
    public record Slot(int index, long owner, long lastUsedFrame, int faces) { }
    private final int capacity;
    private final Map<Long, Slot> owners = new LinkedHashMap<>();
    public LocalShadowAtlas(int capacity) { this.capacity = Math.max(1, capacity); }
    public synchronized Slot acquire(long owner, long frame, int faces) {
        Slot existing = owners.get(owner);
        if (existing != null) {
            Slot refreshed = new Slot(existing.index(), owner, frame, faces);
            owners.put(owner, refreshed);
            return refreshed;
        }
        int index;
        if (owners.size() < capacity) index = firstFree();
        else {
            Slot oldest = owners.values().stream().min(java.util.Comparator.comparingLong(Slot::lastUsedFrame)).orElseThrow();
            owners.remove(oldest.owner());
            index = oldest.index();
        }
        Slot slot = new Slot(index, owner, frame, faces);
        owners.put(owner, slot);
        return slot;
    }
    public synchronized void remove(long owner) { owners.remove(owner); }
    public synchronized Map<Long, Slot> snapshot() { return Map.copyOf(owners); }
    private int firstFree() { for (int i = 0; i < capacity; i++) { final int candidate = i; if (owners.values().stream().noneMatch(slot -> slot.index() == candidate)) return i; } throw new IllegalStateException(); }
}
