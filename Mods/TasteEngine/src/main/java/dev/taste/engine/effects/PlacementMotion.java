package dev.taste.engine.effects;

public final class PlacementMotion {
    public float scale(float elapsedSeconds, float durationSeconds) {
        float t = Math.max(0.0F, Math.min(1.0F, elapsedSeconds / Math.max(durationSeconds, 1.0e-4F)));
        float overshoot = 1.70158F;
        float x = t - 1.0F;
        return 1.0F + (overshoot + 1.0F) * x * x * x + overshoot * x * x;
    }
}
