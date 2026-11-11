package dev.taste.engine.effects;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public final class ParticleManager {
    public record Particle(long id, String type, double x, double y, double z, float velocityX, float velocityY,
            float velocityZ, float age, float lifetime, float size, int color, float emission) { }
    private final List<Particle> particles = new ArrayList<>();
    private long nextId;
    public void spawn(String type, double x, double y, double z, float vx, float vy, float vz,
            float lifetime, float size, int color, float emission, int totalBudget) {
        if (particles.size() >= totalBudget) particles.remove(0);
        particles.add(new Particle(++nextId, type, x, y, z, vx, vy, vz, 0, lifetime, size, color, emission));
    }
    public void tick(float deltaSeconds) {
        List<Particle> next = new ArrayList<>(particles.size());
        for (Particle p : particles) if (p.age() + deltaSeconds < p.lifetime()) {
            next.add(new Particle(p.id(), p.type(), p.x() + p.velocityX() * deltaSeconds,
                    p.y() + p.velocityY() * deltaSeconds, p.z() + p.velocityZ() * deltaSeconds,
                    p.velocityX(), p.velocityY(), p.velocityZ(), p.age() + deltaSeconds,
                    p.lifetime(), p.size(), p.color(), p.emission()));
        }
        particles.clear(); particles.addAll(next);
    }
    public List<Particle> visible(double x, double y, double z, double radius, int limit) {
        double radiusSquared = radius * radius;
        return particles.stream().filter(p -> squared(p, x, y, z) <= radiusSquared)
                .sorted(Comparator.comparingDouble(p -> squared(p, x, y, z))).limit(limit).toList();
    }
    private static double squared(Particle p, double x, double y, double z) { double dx=p.x()-x,dy=p.y()-y,dz=p.z()-z; return dx*dx+dy*dy+dz*dz; }
}
