package dev.taste.engine.effects.water;

public final class CausticRenderer {
    public float intensity(float normalY, float depth, float sunVisibility, float configuredStrength) {
        return Math.max(0.0F, normalY) * (float) Math.exp(-Math.max(0.0F, depth) * 0.08F) * sunVisibility * configuredStrength;
    }
}
