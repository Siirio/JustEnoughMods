package dev.taste.engine.effects.glass;

public record IceMaterial(float roughness, float frost, float absorption, float ior) {
    public IceMaterial { if (roughness < 0 || roughness > 1 || frost < 0 || frost > 1 || absorption < 0 || ior < 1) throw new IllegalArgumentException("Invalid ice material"); }
}
