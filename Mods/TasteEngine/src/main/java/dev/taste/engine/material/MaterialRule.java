package dev.taste.engine.material;

import dev.taste.engine.material.MaterialDescription.Family;
import dev.taste.engine.material.MaterialDescription.Flags;
import dev.taste.engine.material.MaterialDescription.LocalLight;
import dev.taste.engine.material.MaterialDescription.Metadata;
import dev.taste.engine.material.MaterialDescription.NormalY;
import dev.taste.engine.material.MaterialDescription.TextureFormat;
import dev.taste.engine.material.MaterialDescription.Wind;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;

record MaterialRule(int priority, Match match, Map<Field, Object> properties) {
    enum Field {
        FAMILY, TEXTURE_FORMAT, NORMAL_Y, ROUGHNESS, DIELECTRIC_F0, METALNESS, METAL_CODE, OPACITY,
        CUTOUT_THRESHOLD, EMISSION_COLOR, EMISSION, POROSITY, THIN_SCATTERING, IOR, THICKNESS,
        ABSORPTION_COLOR, ABSORPTION_DENSITY, WIND, WIND_WEIGHT, WETNESS, SNOW_RETENTION, RAIN_DROPLETS,
        CAST_SHADOW, RECEIVE_SHADOW, CAST_TRANSMISSION, NORMAL_MAP, SPECULAR_MAP, HEIGHT_MAP,
        LIGHT_RANGE, LIGHT_INTENSITY, LIGHT_COLOR
    }

    record Match(ResourceLocation block, ResourceLocation tag, ResourceLocation fluid, ResourceLocation fluidTag,
            ResourceLocation entity, ResourceLocation entityTag, ResourceLocation sprite, MaterialQuery.Layer layer,
            Map<String, String> state, int specificity) {
        boolean matches(MaterialQuery query) {
            return matchesSubject(query) && (sprite == null || sprite.equals(query.sprite()))
                    && (layer == null || layer == query.layer());
        }

        boolean matchesSubject(MaterialQuery query) {
            return (block == null || block.equals(query.block()))
                    && (tag == null || query.tags().contains(tag))
                    && (fluid == null || fluid.equals(query.fluid()))
                    && (fluidTag == null || query.fluidTags().contains(fluidTag))
                    && (entity == null || entity.equals(query.entity()))
                    && (entityTag == null || query.entityTags().contains(entityTag))
                    && query.state().entrySet().containsAll(state.entrySet());
        }
    }

    static MaterialDescription apply(MaterialDescription base, Map<Field, Object> values, boolean cutout) {
        if (values.isEmpty() && (!cutout || base.flags().cutout())) return base;

        Metadata existing = base.metadata();
        Family family = (Family) values.getOrDefault(Field.FAMILY, existing.family());
        Flags flags = values.containsKey(Field.FAMILY)
                ? new Flags(family == Family.WATER, family == Family.GLASS, family == Family.ICE,
                        family == Family.LAVA, family == Family.FOLIAGE, cutout || base.flags().cutout())
                : new Flags(base.flags().water(), base.flags().glass(), base.flags().ice(), base.flags().lava(),
                        base.flags().foliage(), cutout || base.flags().cutout());
        float opacity = (float) values.getOrDefault(Field.OPACITY, existing.opacity());
        LocalLight light = existing.localLight();
        if (values.containsKey(Field.LIGHT_RANGE)) {
            light = new LocalLight((float) values.get(Field.LIGHT_RANGE), (float) values.get(Field.LIGHT_INTENSITY),
                    (Integer) values.getOrDefault(Field.LIGHT_COLOR, light == null ? null : light.colorRgb()));
        }
        Metadata metadata = new Metadata(family,
                (TextureFormat) values.getOrDefault(Field.TEXTURE_FORMAT, existing.textureFormat()),
                (NormalY) values.getOrDefault(Field.NORMAL_Y, existing.normalY()),
                (int) values.getOrDefault(Field.METAL_CODE, existing.metalCode()), opacity,
                (float) values.getOrDefault(Field.CUTOUT_THRESHOLD, existing.cutoutThreshold()),
                (Integer) values.getOrDefault(Field.EMISSION_COLOR, existing.emissionColorRgb()),
                (float) values.getOrDefault(Field.THICKNESS, existing.thickness()),
                (Integer) values.getOrDefault(Field.ABSORPTION_COLOR, existing.absorptionColorRgb()),
                (float) values.getOrDefault(Field.ABSORPTION_DENSITY, existing.absorptionDensity()),
                (Wind) values.getOrDefault(Field.WIND, existing.wind()),
                (float) values.getOrDefault(Field.WIND_WEIGHT, existing.windWeight()),
                (float) values.getOrDefault(Field.WETNESS, existing.wetness()),
                (float) values.getOrDefault(Field.SNOW_RETENTION, existing.snowRetention()),
                (boolean) values.getOrDefault(Field.RAIN_DROPLETS, existing.rainDroplets()),
                (boolean) values.getOrDefault(Field.CAST_SHADOW, existing.castShadow()),
                (boolean) values.getOrDefault(Field.RECEIVE_SHADOW, existing.receiveShadow()),
                (boolean) values.getOrDefault(Field.CAST_TRANSMISSION, existing.castTransmission()),
                (ResourceLocation) values.getOrDefault(Field.NORMAL_MAP, existing.normalMap()),
                (ResourceLocation) values.getOrDefault(Field.SPECULAR_MAP, existing.specularMap()),
                (ResourceLocation) values.getOrDefault(Field.HEIGHT_MAP, existing.heightMap()), light);
        return new MaterialDescription((float) values.getOrDefault(Field.ROUGHNESS, base.roughness()),
                (float) values.getOrDefault(Field.METALNESS, base.metallic()),
                (float) values.getOrDefault(Field.DIELECTRIC_F0, base.f0()),
                (float) values.getOrDefault(Field.EMISSION, base.emission()),
                (float) values.getOrDefault(Field.POROSITY, base.porosity()),
                (float) values.getOrDefault(Field.THIN_SCATTERING, base.subsurface()),
                values.containsKey(Field.OPACITY) ? 1.0F - opacity : base.transmission(),
                (float) values.getOrDefault(Field.IOR, base.ior()), base.tintRgb(), flags, metadata);
    }
}
