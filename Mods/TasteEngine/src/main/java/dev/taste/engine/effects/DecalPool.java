package dev.taste.engine.effects;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class DecalPool {
    public record Decal(long id, double x, double y, double z, float normalX, float normalY, float normalZ,
            float size, int color, long createdTick, long lifetimeTicks) { }
    private final int capacity;
    private final List<Decal> decals = new ArrayList<>();
    public DecalPool(int capacity) { this.capacity = Math.max(1, capacity); }
    public void add(Decal decal) { if (decals.size() >= capacity) decals.remove(0); decals.add(decal); }
    public List<Decal> visible(long tick) { decals.removeIf(decal -> tick - decal.createdTick() >= decal.lifetimeTicks()); return List.copyOf(decals); }
    public void clear() { decals.clear(); }
}
