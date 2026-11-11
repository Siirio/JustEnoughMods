package dev.taste.engine.render;

public final class ReactiveMask {
    public float value(float alphaChange, float luminanceChange, boolean foreignSurface, boolean emission) {
        float value = Math.max(Math.abs(alphaChange), Math.abs(luminanceChange));
        if (foreignSurface) value = Math.max(value, 0.85F);
        if (emission) value = Math.max(value, 0.55F);
        return Math.max(0.0F, Math.min(1.0F, value));
    }
}
