package dev.taste.engine.effects;

public final class StylizedShading {
    public float toon(float luminance, int bands) { if (bands < 2) return luminance; return Math.round(luminance * bands) / (float) bands; }
    public float outline(float centerDepth, float neighborDepth, float strength) { return Math.min(1.0F, Math.abs(centerDepth-neighborDepth)/Math.max(centerDepth,0.05F)*strength); }
    public float wrap(float normalDotLight, float amount) { return Math.max(0.0F, Math.min(1.0F, (normalDotLight + amount) / (1.0F + amount))); }
}
