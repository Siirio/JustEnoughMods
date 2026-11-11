package dev.taste.engine.effects;

public final class ImpactRenderer {
    public record Impact(float flash, float ringRadius, float smoke, float debris) { }
    public Impact evaluate(float age, float duration, float strength) {
        float t = Math.max(0.0F, Math.min(1.0F, age / Math.max(duration, 1.0e-4F)));
        return new Impact((1.0F - t) * strength, t * t * strength * 6.0F,
                (float) Math.sin(t * Math.PI) * strength, (1.0F - t) * (1.0F - t) * strength);
    }
}
