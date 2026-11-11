package dev.taste.engine.effects;

public final class PortalSurfaceRenderer {
    public record Sample(float red, float green, float blue, float emission, float distortion) { }
    public Sample sample(float u, float v, float time, int seed, float emission, float distortion) {
        float angle = (float) Math.atan2(v - 0.5F, u - 0.5F);
        float radius = (float) Math.hypot(u - 0.5F, v - 0.5F);
        float swirl = (float) Math.sin(angle * 5.0F - radius * 24.0F + time * 1.6F + seed * 0.01F) * 0.5F + 0.5F;
        return new Sample(0.18F + swirl * 0.34F, 0.04F + swirl * 0.12F, 0.38F + swirl * 0.52F, emission, distortion * swirl);
    }
}
