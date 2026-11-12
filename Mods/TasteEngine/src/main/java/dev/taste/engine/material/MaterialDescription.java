package dev.taste.engine.material;

import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

/** Linear material parameters shared by terrain and forward paths. */
public record MaterialDescription(float roughness, float metallic, float f0, float emission,
        float porosity, float subsurface, float transmission, float ior, int tintRgb, Flags flags, Metadata metadata) {
    public enum Family {
        NEUTRAL, STONE, WOOD, FOLIAGE, METAL, GLASS, ICE, WATER, LAVA, PORTAL, CLOTH, SKIN, CUSTOM
    }

    public enum TextureFormat {
        PROCEDURAL, LABPBR, OLDPBR
    }

    public enum NormalY {
        OPENGL, DIRECTX
    }

    public enum Wind {
        NONE, GRASS, LEAVES, VINE, CLOTH
    }

    public record Flags(boolean water, boolean glass, boolean ice, boolean lava, boolean foliage, boolean cutout) { }

    public record LocalLight(float range, float intensity, Integer colorRgb) {
        public LocalLight {
            if (!Float.isFinite(range) || range < 0.0F || range > 64.0F) {
                throw new IllegalArgumentException("Invalid local light range");
            }
            if (!Float.isFinite(intensity) || intensity < 0.0F || intensity > 32.0F) {
                throw new IllegalArgumentException("Invalid local light intensity");
            }
            if (colorRgb != null && (colorRgb < 0 || colorRgb > 0xFFFFFF)) {
                throw new IllegalArgumentException("Invalid local light color");
            }
        }
    }

    public record Metadata(Family family, TextureFormat textureFormat, NormalY normalY, int metalCode,
            float opacity, float cutoutThreshold, Integer emissionColorRgb, float thickness, Integer absorptionColorRgb,
            float absorptionDensity, Wind wind, float windWeight, float wetness, float snowRetention,
            boolean rainDroplets, boolean castShadow, boolean receiveShadow, boolean castTransmission,
            ResourceLocation normalMap, ResourceLocation specularMap, ResourceLocation heightMap, LocalLight localLight) {
        public Metadata {
            Objects.requireNonNull(family);
            Objects.requireNonNull(wind);
            requireUnit("opacity", opacity);
            requireUnit("cutout threshold", cutoutThreshold);
        }

        public TextureFormat effectiveTextureFormat(TextureFormat configured, TextureFormat declared) {
            if (textureFormat != null) return textureFormat;
            if (configured != null) return configured;
            return declared != null ? declared : TextureFormat.PROCEDURAL;
        }

        public NormalY effectiveNormalY(TextureFormat format) {
            if (normalY != null) return normalY;
            return format == TextureFormat.LABPBR ? NormalY.DIRECTX : NormalY.OPENGL;
        }

        static Metadata defaults(Flags flags, float transmission) {
            Family family = Family.NEUTRAL;
            if (flags.water()) family = Family.WATER;
            else if (flags.glass()) family = Family.GLASS;
            else if (flags.ice()) family = Family.ICE;
            else if (flags.lava()) family = Family.LAVA;
            else if (flags.foliage()) family = Family.FOLIAGE;
            return new Metadata(family, null, null, -1, 1.0F - transmission, 0.1F, null, 0.0F, null,
                    0.0F, Wind.NONE, 0.0F, 1.0F, 1.0F, false, true, true, false, null, null, null, null);
        }
    }

    private static final MaterialDescription FALLBACK = new MaterialDescription(0.82F, 0.0F, 0.04F,
            0.0F, 0.5F, 0.0F, 0.0F, 1.5F, 0xFFFFFF, new Flags(false, false, false, false, false, false));

    public MaterialDescription(float roughness, float metallic, float f0, float emission,
            float porosity, float subsurface, float transmission, float ior, int tintRgb, Flags flags) {
        this(roughness, metallic, f0, emission, porosity, subsurface, transmission, ior, tintRgb, flags,
                Metadata.defaults(flags, transmission));
    }

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
        Objects.requireNonNull(metadata);
    }

    public static MaterialDescription fallback() {
        return FALLBACK;
    }
    private static void requireUnit(String name, float value) {
        if (!Float.isFinite(value) || value < 0.0F || value > 1.0F)
            throw new IllegalArgumentException("Invalid " + name);
    }
}
