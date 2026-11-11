package dev.taste.engine.effects.glass;

public record GlassRenderer(float ior, float thickness, boolean stainedTransmission, boolean refraction,
        GlassPatchPool patches, IceMaterial ice) {
    public GlassRenderer { if (ior < 1 || thickness < 0) throw new IllegalArgumentException("Invalid glass material"); }
}
