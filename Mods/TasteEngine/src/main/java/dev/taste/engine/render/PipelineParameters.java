package dev.taste.engine.render;

import com.google.gson.JsonPrimitive;
import dev.taste.engine.config.ConfigSnapshot;
import dev.taste.engine.config.ConsumerRegistry;
import dev.taste.engine.config.SettingRegistry;
import java.util.LinkedHashMap;
import java.util.Map;

/** Immutable frame parameter block. Every shipping setting is assigned to its real pipeline phase. */
public final class PipelineParameters {
    private final ConfigSnapshot snapshot;
    private final Map<String, Map<String, JsonPrimitive>> phases;

    private PipelineParameters(ConfigSnapshot snapshot, Map<String, Map<String, JsonPrimitive>> phases) {
        this.snapshot = snapshot;
        this.phases = Map.copyOf(phases);
    }

    public static PipelineParameters resolve(ConfigSnapshot snapshot, SettingRegistry registry,
            ConsumerRegistry consumers) {
        Map<String, Map<String, JsonPrimitive>> grouped = new LinkedHashMap<>();
        registry.settings().keySet().forEach(id -> grouped
                .computeIfAbsent(consumers.consumer(id), ignored -> new LinkedHashMap<>())
                .put(id, snapshot.value(id)));
        Map<String, Map<String, JsonPrimitive>> immutable = new LinkedHashMap<>();
        grouped.forEach((phase, values) -> immutable.put(phase, Map.copyOf(values)));
        return new PipelineParameters(snapshot, immutable);
    }

    public long revision() { return snapshot.revision(); }
    public JsonPrimitive value(String id) { return snapshot.value(id); }
    public Map<String, JsonPrimitive> phase(String phase) { return phases.getOrDefault(phase, Map.of()); }
    public Map<String, Map<String, JsonPrimitive>> phases() { return phases; }
}
