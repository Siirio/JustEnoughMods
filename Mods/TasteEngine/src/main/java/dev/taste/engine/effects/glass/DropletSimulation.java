package dev.taste.engine.effects.glass;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class DropletSimulation {
    public record Drop(float u, float v, float radius, float speed, float age) { }
    private final List<Drop> drops = new ArrayList<>();
    private long randomState;
    public DropletSimulation(long seed) { randomState = seed; }
    public void step(float deltaSeconds, float rain, float density, float drySeconds, int maximumDrops) {
        List<Drop> next = new ArrayList<>(drops.size());
        for (Drop drop : drops) {
            float radius = Math.max(0.0F, drop.radius() - deltaSeconds / Math.max(1.0F, drySeconds) * (1.0F - rain) * 0.02F);
            float v = drop.v() - drop.speed() * deltaSeconds;
            if (radius > 0.002F && v > -0.2F) next.add(new Drop(drop.u(), v, radius, drop.speed(), drop.age() + deltaSeconds));
        }
        drops.clear(); drops.addAll(next);
        int additions = Math.min(maximumDrops - drops.size(), (int) Math.floor(rain * density * deltaSeconds * 12.0F));
        for (int i = 0; i < additions; i++) drops.add(new Drop(random(), 1.0F + random() * 0.1F, 0.006F + random() * 0.025F, 0.025F + random() * 0.08F, 0.0F));
    }
    public List<Drop> snapshot() { return List.copyOf(drops); }
    private float random() { randomState ^= randomState << 13; randomState ^= randomState >>> 7; randomState ^= randomState << 17; return (randomState >>> 40 & 0xFFFFFF) / 16777215.0F; }
}
