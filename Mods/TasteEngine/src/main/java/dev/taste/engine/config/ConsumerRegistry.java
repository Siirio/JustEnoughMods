package dev.taste.engine.config;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** Maps every shipped control to the renderer phase that consumes it. */
public final class ConsumerRegistry {
    private final Map<String, String> consumers = new LinkedHashMap<>();
    public void register(String phase, String... ids) {
        for (String id : ids) {
            String previous = consumers.putIfAbsent(id, phase);
            if (previous != null && !previous.equals(phase))
                throw new IllegalArgumentException(id + " already belongs to " + previous);
        }
    }
    public String consumer(String id) { return consumers.getOrDefault(id, "unassigned"); }
    public Set<String> unassigned(SettingRegistry registry) {
        Set<String> missing = new LinkedHashSet<>(registry.settings().keySet());
        missing.removeAll(consumers.keySet());
        return Set.copyOf(missing);
    }
    public Map<String, String> snapshot() { return Map.copyOf(consumers); }

    public static ConsumerRegistry complete(SettingRegistry registry) {
        ConsumerRegistry result = new ConsumerRegistry();
        Map<String, String> phases = Map.ofEntries(
                Map.entry("general", "frame"), Map.entry("lighting", "lighting"), Map.entry("shadows", "shadow"),
                Map.entry("gi", "gi"), Map.entry("ao", "ao"), Map.entry("sky", "sky"), Map.entry("clouds", "cloud"),
                Map.entry("cloudLayer1", "cloud"), Map.entry("cloudLayer2", "cloud"), Map.entry("cloudLayer3", "cloud"),
                Map.entry("fog", "volume"), Map.entry("weather", "weather"), Map.entry("wetness", "weather"),
                Map.entry("water", "water"), Map.entry("underwater", "water"), Map.entry("reflections", "reflection"),
                Map.entry("glass", "glass"), Map.entry("materials", "material"), Map.entry("emission", "material"),
                Map.entry("bloom", "bloom"), Map.entry("vegetation", "vegetation"), Map.entry("particles", "particle"),
                Map.entry("entities", "forward"), Map.entry("hand", "hand"), Map.entry("color", "color"),
                Map.entry("camera", "camera"), Map.entry("antialiasing", "temporal"), Map.entry("style", "style"),
                Map.entry("dimensions", "environment"), Map.entry("accessibility", "accessibility"),
                Map.entry("performance", "performance"), Map.entry("photo", "photo"), Map.entry("server", "environment"),
                Map.entry("lava", "lava"), Map.entry("portals", "portal"), Map.entry("interaction", "interaction"));
        registry.settings().values().forEach(spec -> result.register(phases.getOrDefault(spec.category(), "unassigned"), spec.id()));
        return result;
    }
}
