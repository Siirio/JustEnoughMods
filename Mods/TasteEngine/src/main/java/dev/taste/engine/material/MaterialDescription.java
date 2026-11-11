package dev.taste.engine.material;

import java.util.Objects;

/** Linear material parameters shared by terrain and forward paths. */
public record MaterialDescription(float roughness, float metallic, float f0, float emission,
        float porosity, float subsurface, float transmission, float ior, int tintRgb, Flags flags) {
    public record Flags(boolean water, boolean glass, boolean ice, boolean lava, boolean foliage, boolean cutout) { }
    public MaterialDescription {
        requireUnit("roughness", roughness);
        requireUnit("metallic", metallic);
        requireUnit("f0", f0);
        if (!Float.isFinite(emission) || emission < 0.0F) throw new IllegalArgumentException("Invalid emission");
        requireUnit("porosity", porosity);
        requireUnit("subsurface", subsurface);
        requireUnit("transmission", transmission);
        if (!Float.isFinite(ior) || ior < 1.0F || ior > 3.0F) throw new IllegalArgumentException("Invalid IOR");
        Objects.requireNonNull(flags);
    }
    public static MaterialDescription fallback() {
        return new MaterialDescription(0.82F, 0.0F, 0.04F, 0.0F, 0.5F, 0.0F, 0.0F, 1.5F,
                0xFFFFFF, new Flags(false, false, false, false, false, false));
    }
    private static void requireUnit(String name, float value) {
        if (!Float.isFinite(value) || value < 0.0F || value > 1.0F)
            throw new IllegalArgumentException("Invalid " + name);
    }
}
