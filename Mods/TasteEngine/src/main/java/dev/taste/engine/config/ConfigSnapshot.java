package dev.taste.engine.config;

import com.google.gson.JsonPrimitive;
import java.util.Map;

/** A renderer may read one published snapshot per frame; never read a mutable screen draft. */
public record ConfigSnapshot(long revision, String style, String quality,
        Map<String, JsonPrimitive> userOverrides, Map<String, JsonPrimitive> values) {
    public ConfigSnapshot {
        userOverrides = Map.copyOf(userOverrides);
        values = Map.copyOf(values);
    }
    public JsonPrimitive value(String id) {
        JsonPrimitive value = values.get(id);
        if (value == null) throw new IllegalArgumentException("Unknown setting: " + id);
        return value;
    }
    public boolean requestedEnabled() { return value("general.enabled").getAsBoolean(); }
}
