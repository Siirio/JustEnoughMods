package dev.taste.engine.effects.water;

public final class FoamRenderer {
    public float coverage(float depthDifference, float slope, float turbulence, float width) {
        float shore = 1.0F - smooth(0.0F, Math.max(width, 1.0e-4F), depthDifference);
        return clamp(shore + slope * 0.35F + turbulence * 0.4F);
    }
    private static float smooth(float a, float b, float x) { float t = clamp((x - a) / (b - a)); return t * t * (3 - 2 * t); }
    private static float clamp(float value) { return Math.max(0.0F, Math.min(1.0F, value)); }
}
