package dev.taste.engine.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class SettingRegistry {
    public record Category(String id, Map<String, String> labels) {
        public Category { labels = Map.copyOf(labels); }
        public String label(String language) { return labels.getOrDefault(language, labels.getOrDefault("en_us", id)); }
    }
    public record LessOrEqual(String left, String right, SettingSpec.Requirement when) { }
    private final List<Category> categories;
    private final Map<String, SettingSpec> settings;
    private final List<LessOrEqual> constraints;

    private SettingRegistry(List<Category> categories, Map<String, SettingSpec> settings, List<LessOrEqual> constraints) {
        this.categories = List.copyOf(categories);
        this.settings = Collections.unmodifiableMap(new LinkedHashMap<>(settings));
        this.constraints = List.copyOf(constraints);
    }

    public static SettingRegistry bundled() throws IOException {
        try (Reader reader = resourceReader("registry.json")) { return read(reader); }
    }

    public static Reader resourceReader(String file) throws IOException {
        InputStream stream = SettingRegistry.class.getResourceAsStream("/assets/taste/settings/" + file);
        if (stream == null) throw new IOException("Missing bundled setting resource: " + file);
        return new InputStreamReader(stream, StandardCharsets.UTF_8);
    }

    public static SettingRegistry read(Reader reader) {
        JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
        if (root.get("schemaVersion").getAsInt() != 1) throw new IllegalArgumentException("Unknown registry schema");
        List<Category> categories = new ArrayList<>();
        root.getAsJsonArray("categories").forEach(element -> {
            JsonObject object = element.getAsJsonObject();
            String id = object.get("id").getAsString();
            if (categories.stream().anyMatch(c -> c.id().equals(id))) throw new IllegalArgumentException("Duplicate category " + id);
            categories.add(new Category(id, labels(object.getAsJsonObject("label"))));
        });
        Map<String, SettingSpec> settings = new LinkedHashMap<>();
        for (JsonElement element : root.getAsJsonArray("settings")) {
            JsonObject object = element.getAsJsonObject();
            String id = object.get("id").getAsString();
            String category = object.get("category").getAsString();
            if (categories.stream().noneMatch(c -> c.id().equals(category))) throw new IllegalArgumentException("Unknown category " + category);
            SettingSpec.Type type = SettingSpec.Type.valueOf(object.get("type").getAsString().toUpperCase(Locale.ROOT));
            List<String> options = new ArrayList<>();
            if (object.has("options")) object.getAsJsonArray("options").forEach(v -> options.add(v.getAsString()));
            List<SettingSpec.Requirement> requires = new ArrayList<>();
            object.getAsJsonArray("requires").forEach(v -> {
                JsonObject requirement = v.getAsJsonObject();
                requires.add(readRequirement(requirement));
            });
            EnumSet<SettingSpec.ApplyAction> actions = EnumSet.noneOf(SettingSpec.ApplyAction.class);
            object.getAsJsonArray("apply").forEach(v -> actions.add(SettingSpec.ApplyAction.valueOf(v.getAsString())));
            double min = object.has("minimum") ? object.get("minimum").getAsDouble() : 0;
            double max = object.has("maximum") ? object.get("maximum").getAsDouble() : 1;
            double step = object.has("step") ? object.get("step").getAsDouble() : 1;
            if (!Double.isFinite(min) || !Double.isFinite(max) || !Double.isFinite(step) || max < min || step <= 0)
                throw new IllegalArgumentException("Invalid bounds for " + id);
            SettingSpec spec = new SettingSpec(id, category, type, object.getAsJsonPrimitive("default"),
                    labels(object.getAsJsonObject("label")), object.get("description").getAsString(),
                    object.get("unit").getAsString(), min, max, step, options, requires, actions,
                    object.get("scope").getAsString(), object.get("level").getAsString());
            spec.validate(spec.defaultValue());
            if (settings.putIfAbsent(id, spec) != null) throw new IllegalArgumentException("Duplicate setting " + id);
        }
        List<LessOrEqual> constraints = new ArrayList<>();
        root.getAsJsonArray("crossConstraints").forEach(v -> {
            JsonObject constraint = v.getAsJsonObject();
            var pair = constraint.getAsJsonArray("lessOrEqual");
            constraints.add(new LessOrEqual(pair.get(0).getAsString(), pair.get(1).getAsString(),
                    constraint.has("when") ? readRequirement(constraint.getAsJsonObject("when")) : null));
        });
        SettingRegistry registry = new SettingRegistry(categories, settings, constraints);
        for (SettingSpec spec : settings.values()) for (SettingSpec.Requirement dependency : spec.requires())
            if (dependency.kind() == SettingSpec.Requirement.Kind.SETTING)
                registry.require(dependency.key()).validate(dependency.expected());
        for (LessOrEqual constraint : constraints) {
            if (!registry.require(constraint.left()).numeric() || !registry.require(constraint.right()).numeric())
                throw new IllegalArgumentException("Non-numeric cross constraint");
            if (constraint.when() != null && constraint.when().kind() == SettingSpec.Requirement.Kind.SETTING)
                registry.require(constraint.when().key()).validate(constraint.when().expected());
        }
        registry.validateComplete(registry.defaults());
        return registry;
    }

    private static Map<String, String> labels(JsonObject object) {
        Map<String, String> result = new LinkedHashMap<>();
        object.entrySet().forEach(e -> result.put(e.getKey(), e.getValue().getAsString()));
        return result;
    }

    private static SettingSpec.Requirement readRequirement(JsonObject object) {
        if (object.has("setting") && object.has("equals") && !object.has("capability"))
            return new SettingSpec.Requirement(SettingSpec.Requirement.Kind.SETTING,
                    object.get("setting").getAsString(), object.getAsJsonPrimitive("equals"));
        if (object.has("capability") && !object.has("setting"))
            return new SettingSpec.Requirement(SettingSpec.Requirement.Kind.CAPABILITY,
                    object.get("capability").getAsString(), null);
        throw new IllegalArgumentException("Unknown dependency shape: " + object);
    }

    public List<Category> categories() { return categories; }
    public Map<String, SettingSpec> settings() { return settings; }
    public SettingSpec require(String id) {
        SettingSpec spec = settings.get(id);
        if (spec == null) throw new IllegalArgumentException("Unknown setting: " + id);
        return spec;
    }
    public Map<String, JsonPrimitive> defaults() {
        Map<String, JsonPrimitive> defaults = new LinkedHashMap<>();
        settings.forEach((id, spec) -> defaults.put(id, spec.validate(spec.defaultValue())));
        return defaults;
    }
    public void validateComplete(Map<String, JsonPrimitive> values) {
        if (!values.keySet().equals(settings.keySet())) throw new IllegalArgumentException("Incomplete or unknown setting IDs");
        settings.forEach((id, spec) -> spec.validate(values.get(id)));
        for (LessOrEqual pair : constraints)
            if ((pair.when() == null || pair.when().matches(values, Set.of()))
                    && values.get(pair.left()).getAsDouble() > values.get(pair.right()).getAsDouble())
                throw new IllegalArgumentException(pair.left() + " must be <= " + pair.right());
    }
    public List<String> unmetRequirements(String id, Map<String, JsonPrimitive> values) {
        return unmetRequirements(id, values, Set.of());
    }
    public List<String> unmetRequirements(String id, Map<String, JsonPrimitive> effectiveValues, Set<String> capabilities) {
        return require(id).requires().stream()
                .filter(r -> !r.matches(effectiveValues, capabilities))
                .map(SettingSpec.Requirement::explanation).toList();
    }
}
