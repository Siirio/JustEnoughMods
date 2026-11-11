package dev.taste.engine.environment;

import java.util.LinkedHashMap;
import java.util.Map;

public final class DimensionProfiles {
    public record Profile(boolean sky, float red, float green, float blue, float fogDensity,
            float cloudBase, float cloudTop, float starStrength) { }
    private final Map<String, Profile> profiles = new LinkedHashMap<>();
    private final Profile fallback = new Profile(false, 0.08F, 0.08F, 0.10F, 0.02F, 128.0F, 192.0F, 0.4F);
    public DimensionProfiles() {
        profiles.put("minecraft:overworld", new Profile(true, 0.48F, 0.68F, 1.0F, 0.006F, 112.0F, 196.0F, 1.0F));
        profiles.put("minecraft:the_nether", new Profile(false, 0.45F, 0.08F, 0.03F, 0.035F, 0.0F, 0.0F, 0.0F));
        profiles.put("minecraft:the_end", new Profile(false, 0.16F, 0.10F, 0.28F, 0.012F, 0.0F, 0.0F, 1.5F));
    }
    public void register(String dimension, Profile profile) { profiles.put(dimension, profile); }
    public Profile resolve(String dimension) { return profiles.getOrDefault(dimension, fallback); }
}
