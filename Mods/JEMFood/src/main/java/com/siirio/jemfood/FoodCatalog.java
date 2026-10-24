package com.siirio.jemfood;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** The JAR contains a build projection of the Atlas authority, never another assignment table. */
public final class FoodCatalog {
    public record Dish(ResourceLocation id, int tier, String policy, boolean variety, List<String> traits) {
        public int duration() { return tier == 0 ? 0 : 1200 * (tier + 1); }
    }
    public record Trait(String id, String backend, String units, ResourceLocation preferred, ResourceLocation fallback,
                        double first, double second, double third) {
        public double value(int tier) { return tier == 1 ? first : tier == 2 ? second : third; }
        public boolean bridge() { return backend.equals("bridge"); }
    }
    private static final Map<ResourceLocation, Dish> DISHES = new LinkedHashMap<>();
    private static final Map<String, Trait> TRAITS = new LinkedHashMap<>();
    static {
        var stream = FoodCatalog.class.getResourceAsStream("/data/jemfood/food_quality.json");
        if (stream == null) throw new IllegalStateException("Missing Atlas food projection");
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            for (var element : root.getAsJsonArray("traits")) {
                var row = element.getAsJsonObject();
                var values = row.getAsJsonObject("mechanic").getAsJsonObject("values_by_tier");
                String id = row.get("id").getAsString();
                var trait = new Trait(id, row.get("backend").getAsString(), row.getAsJsonObject("mechanic").get("units").getAsString(), location(row, "preferred_effect_id"),
                        location(row, "fallback_vanilla_effect_id"), values.get("1").getAsDouble(),
                        values.get("2").getAsDouble(), values.get("3").getAsDouble());
                if (TRAITS.put(id, trait) != null) throw new IllegalStateException("Duplicate food trait " + id);
            }
            for (var element : root.getAsJsonArray("assignments")) {
                var row = element.getAsJsonObject();
                var id = new ResourceLocation(row.get("id").getAsString());
                int tier = row.get("quality_tier").getAsInt();
                var traits = row.getAsJsonArray("blessing_traits").asList().stream().map(v -> v.getAsString()).toList();
                String policy = row.get("bonus_policy").getAsString();
                if (tier < 0 || tier > 3 || traits.stream().distinct().count() != traits.size()
                        || traits.stream().anyMatch(t -> !TRAITS.containsKey(t))
                        || traits.size() != (policy.equals("food_blessing") ? tier : 0))
                    throw new IllegalStateException("Invalid food assignment " + id);
                if (DISHES.put(id, new Dish(id, tier, policy, row.get("variety_eligible").getAsBoolean(), traits)) != null)
                    throw new IllegalStateException("Duplicate food " + id);
            }
        } catch (java.io.IOException e) { throw new IllegalStateException("Cannot load food projection", e); }
    }
    private FoodCatalog() {}
    private static ResourceLocation location(JsonObject row, String key) {
        return row.get(key).isJsonNull() ? null : new ResourceLocation(row.get(key).getAsString());
    }
    public static Dish find(ResourceLocation id) { return DISHES.get(id); }
    public static Trait trait(String id) { return TRAITS.get(id); }
    public static List<Trait> traits() { return List.copyOf(TRAITS.values()); }
}
