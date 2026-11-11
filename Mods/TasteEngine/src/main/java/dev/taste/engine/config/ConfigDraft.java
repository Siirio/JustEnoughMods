package dev.taste.engine.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import java.util.LinkedHashMap;
import java.util.Map;

/** Screen-owned transaction. Discarding a draft is Cancel; it has no global side effects. */
public final class ConfigDraft {
    private final SettingRegistry registry;
    private final PresetCatalog presets;
    private final ConfigSnapshot original;
    private final Map<String, JsonPrimitive> overrides = new LinkedHashMap<>();
    private String style;
    private String quality;

    public ConfigDraft(SettingRegistry registry, PresetCatalog presets, ConfigSnapshot original) {
        this.registry = registry;
        this.presets = presets;
        this.original = original;
        style = original.style();
        quality = original.quality();
        overrides.putAll(original.userOverrides());
    }
    public static ConfigSnapshot defaults(SettingRegistry registry, PresetCatalog presets) {
        String style = registry.require("general.style").defaultValue().getAsString();
        String quality = registry.require("general.quality").defaultValue().getAsString();
        ConfigSnapshot base = new ConfigSnapshot(0, style, quality, Map.of(), registry.defaults());
        return new ConfigDraft(registry, presets, base).snapshot(0);
    }
    public long baseRevision() { return original.revision(); }
    public String style() { return style; }
    public String quality() { return quality; }
    public boolean hasOverride(String id) { return overrides.containsKey(id); }
    public boolean dirty() {
        return !style.equals(original.style()) || !quality.equals(original.quality()) || !overrides.equals(original.userOverrides());
    }
    public void set(String id, JsonElement value) {
        JsonPrimitive validated = registry.require(id).validate(value);
        switch (id) {
            case "general.style" -> { presets.style(validated.getAsString()); style = validated.getAsString(); }
            case "general.quality" -> { presets.quality(validated.getAsString()); quality = validated.getAsString(); }
            default -> overrides.put(id, validated);
        }
    }
    public void reset(String id) {
        if (id.equals("general.style")) style = registry.require(id).defaultValue().getAsString();
        else if (id.equals("general.quality")) quality = registry.require(id).defaultValue().getAsString();
        else { registry.require(id); overrides.remove(id); }
    }
    public void resetCategory(String category) {
        registry.settings().values().stream().filter(s -> s.category().equals(category)).forEach(s -> reset(s.id()));
    }
    public void resetAll() {
        overrides.clear();
        style = registry.require("general.style").defaultValue().getAsString();
        quality = registry.require("general.quality").defaultValue().getAsString();
    }
    public Map<String, JsonPrimitive> effective() {
        Map<String, JsonPrimitive> values = registry.defaults();
        values.putAll(presets.style(style));
        values.putAll(presets.quality(quality));
        values.put("general.style", new JsonPrimitive(style));
        values.put("general.quality", new JsonPrimitive(quality));
        values.putAll(overrides);
        return values;
    }
    public JsonPrimitive value(String id) { registry.require(id); return effective().get(id); }
    public ConfigSnapshot snapshot(long revision) {
        Map<String, JsonPrimitive> values = effective();
        registry.validateComplete(values);
        return new ConfigSnapshot(revision, style, quality, overrides, values);
    }
}
