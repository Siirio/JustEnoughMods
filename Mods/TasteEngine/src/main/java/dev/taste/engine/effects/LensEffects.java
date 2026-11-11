package dev.taste.engine.effects;

public record LensEffects(float flareStrength, float vignette, float chromaticAberration, float grain) {
    public LensEffects { if (flareStrength < 0 || vignette < 0 || chromaticAberration < 0 || grain < 0) throw new IllegalArgumentException("Invalid lens effects"); }
    public float vignette(float normalizedRadiusSquared) { return Math.max(0.0F, 1.0F - vignette * normalizedRadiusSquared); }
}
