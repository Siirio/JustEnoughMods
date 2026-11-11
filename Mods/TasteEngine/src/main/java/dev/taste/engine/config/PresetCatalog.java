package dev.taste.engine.config;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import java.io.IOException;
import java.io.Reader;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Independent partial overlays. Every composition starts from defaults, so old presets cannot leak. */
public final class PresetCatalog {
    private final Map<String, Map<String, JsonPrimitive>> styles;
    private final Map<String, Map<String, JsonPrimitive>> qualities;
    private PresetCatalog(Map<String, Map<String, JsonPrimitive>> styles, Map<String, Map<String, JsonPrimitive>> qualities) {
        this.styles = Map.copyOf(styles);
        this.qualities = Map.copyOf(qualities);
    }
    public static PresetCatalog bundled(SettingRegistry registry) throws IOException {
        try (Reader style = SettingRegistry.resourceReader("style-presets.json");
             Reader quality = SettingRegistry.resourceReader("quality-presets.json")) {
            return read(registry, style, quality);
        }
    }
    public static PresetCatalog read(SettingRegistry registry, Reader style, Reader quality) {
        return new PresetCatalog(readGroup(registry, style, "general.style"), readGroup(registry, quality, "general.quality"));
    }
    private static Map<String, Map<String, JsonPrimitive>> readGroup(SettingRegistry registry, Reader reader, String selector) {
        JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
        if (root.get("schemaVersion").getAsInt() != 1) throw new IllegalArgumentException("Unknown preset schema");
        Map<String, Map<String, JsonPrimitive>> result = new LinkedHashMap<>();
        root.getAsJsonObject("presets").entrySet().forEach(entry -> {
            registry.require(selector).validate(new JsonPrimitive(entry.getKey()));
            Map<String, JsonPrimitive> overlay = new LinkedHashMap<>();
            entry.getValue().getAsJsonObject().entrySet().forEach(value -> {
                if (value.getKey().equals("general.style") || value.getKey().equals("general.quality"))
                    throw new IllegalArgumentException("Preset cannot rewrite its selectors");
                overlay.put(value.getKey(), registry.require(value.getKey()).validate(value.getValue()));
            });
            result.put(entry.getKey(), Map.copyOf(overlay));
        });
        result.put("custom", Map.of());
        return result;
    }
    public Map<String, JsonPrimitive> style(String name) { return require(styles, name); }
    public Map<String, JsonPrimitive> quality(String name) { return require(qualities, name); }
    public Set<String> styleNames() { return styles.keySet(); }
    public Set<String> qualityNames() { return qualities.keySet(); }
    private static Map<String, JsonPrimitive> require(Map<String, Map<String, JsonPrimitive>> groups, String name) {
        Map<String, JsonPrimitive> result = groups.get(name);
        if (result == null) throw new IllegalArgumentException("Unknown preset: " + name);
        return result;
    }
}
