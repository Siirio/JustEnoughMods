package dev.taste.engine.config;

import com.google.gson.JsonPrimitive;
import java.util.LinkedHashMap;
import java.util.Map;

public final class DimensionOverrides {
    private final Map<String, Map<String, JsonPrimitive>> values = new LinkedHashMap<>();
    public void put(String dimension, Map<String, JsonPrimitive> overrides) { values.put(dimension, Map.copyOf(overrides)); }
    public Map<String, JsonPrimitive> resolve(String dimension) { return values.getOrDefault(dimension, Map.of()); }
    public void remove(String dimension) { values.remove(dimension); }
    public Map<String, Map<String, JsonPrimitive>> snapshot() { return Map.copyOf(values); }
}
