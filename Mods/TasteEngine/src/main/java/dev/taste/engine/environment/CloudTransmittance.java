package dev.taste.engine.environment;

public final class CloudTransmittance {
    public float transmittance(float density, float distance) { return (float) Math.exp(-Math.max(0.0F, density) * Math.max(0.0F, distance)); }
    public float composite(float front, float back) { return clamp(front * back); }
    private static float clamp(float value) { return Math.max(0.0F, Math.min(1.0F, value)); }
}
