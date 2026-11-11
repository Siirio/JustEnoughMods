package dev.taste.engine.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Immutable UI/validation descriptor. JSON primitives are immutable value objects. */
public record SettingSpec(String id, String category, Type type, JsonPrimitive defaultValue,
        Map<String, String> labels, String description, String unit, double minimum,
        double maximum, double step, List<String> options, List<Requirement> requires,
        Set<ApplyAction> apply, String scope, String level) {
    public enum Type { BOOLEAN, INTEGER, NUMBER, ENUM, COLOR }
    public enum ApplyAction { UNIFORM, SHADERS, TARGETS, CHUNKS, HISTORY, RESOURCES, PROFILE_TRANSACTION }
    public record Requirement(Kind kind, String key, JsonPrimitive expected) {
        public enum Kind { SETTING, CAPABILITY }
        public Requirement {
            if (kind == null || key == null || key.isBlank() || (kind == Kind.SETTING && expected == null))
                throw new IllegalArgumentException("Invalid dependency descriptor");
        }
        public boolean matches(Map<String, JsonPrimitive> effectiveValues, Set<String> capabilities) {
            return kind == Kind.CAPABILITY ? capabilities.contains(key) : expected.equals(effectiveValues.get(key));
        }
        public String explanation() { return kind == Kind.CAPABILITY ? "capability: " + key : key + " = " + expected; }
    }

    public SettingSpec {
        labels = Map.copyOf(labels);
        options = List.copyOf(options);
        requires = List.copyOf(requires);
        apply = Set.copyOf(apply);
    }

    public String label(String language) {
        return labels.getOrDefault(language, labels.getOrDefault("en_us", id));
    }

    /** Strict type and finite-range validation; slider step is a UI increment, not a data restriction. */
    public JsonPrimitive validate(JsonElement input) {
        if (input == null || !input.isJsonPrimitive()) throw bad("a primitive value is required");
        JsonPrimitive value = input.getAsJsonPrimitive();
        return switch (type) {
            case BOOLEAN -> {
                if (!value.isBoolean()) throw bad("a boolean is required");
                yield new JsonPrimitive(value.getAsBoolean());
            }
            case ENUM -> {
                if (!value.isString() || !options.contains(value.getAsString()))
                    throw bad("expected one of " + options);
                yield new JsonPrimitive(value.getAsString());
            }
            case COLOR -> {
                if (!value.isString() || !value.getAsString().matches("#[0-9a-fA-F]{6}"))
                    throw bad("expected #RRGGBB");
                yield new JsonPrimitive(value.getAsString().toUpperCase(Locale.ROOT));
            }
            case NUMBER, INTEGER -> {
                if (!value.isNumber()) throw bad("a JSON number is required");
                double number;
                try { number = value.getAsDouble(); }
                catch (NumberFormatException error) { throw bad("invalid number"); }
                if (!Double.isFinite(number)) throw bad("number must be finite");
                if (number < minimum || number > maximum)
                    throw bad("expected " + minimum + " .. " + maximum);
                if (type == Type.INTEGER) {
                    if (number != Math.rint(number) || number < Long.MIN_VALUE || number >= 0x1.0p63)
                        throw bad("an exactly representable integer in range is required");
                    yield new JsonPrimitive((long) number);
                }
                yield new JsonPrimitive(number);
            }
        };
    }

    public JsonPrimitive parseEditorText(String text) {
        if (type == Type.COLOR || type == Type.ENUM) return validate(new JsonPrimitive(text.trim()));
        if (type == Type.BOOLEAN) {
            if (!text.equals("true") && !text.equals("false")) throw bad("expected true or false");
            return validate(new JsonPrimitive(Boolean.parseBoolean(text)));
        }
        try { return validate(new JsonPrimitive(Double.parseDouble(text.trim()))); }
        catch (NumberFormatException error) { throw bad("invalid number"); }
    }

    public boolean numeric() { return type == Type.INTEGER || type == Type.NUMBER; }
    private IllegalArgumentException bad(String reason) { return new IllegalArgumentException(id + ": " + reason); }
}
