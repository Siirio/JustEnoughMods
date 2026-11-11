package dev.taste.engine.effects;

public record MotionBlur(float shutter, int samples, boolean handAffected) {
    public MotionBlur { if (shutter < 0 || samples < 1) throw new IllegalArgumentException("Invalid motion blur"); }
    public float scale(float velocity) { return velocity * shutter; }
}
