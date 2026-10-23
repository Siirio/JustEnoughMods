package com.siirio.jemcompat.feature.food;

import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.registries.ForgeRegistries;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Read-only projection of Catalog/changes/food-quality-assignments.json, packaged by Gradle. */
public final class FoodCatalog {
    public record Effect(ResourceLocation id, int amplifier) {
        public MobEffect effect() { return ForgeRegistries.MOB_EFFECTS.getValue(id); }
    }
    public record Dish(ResourceLocation id, int tier, String family, List<Effect> effects) {
        public int duration() { return switch (tier) { case 1 -> 9600; case 2 -> 18000; case 3 -> 36000; default -> throw new IllegalStateException("Invalid food tier"); }; }
    }
    private static final Map<ResourceLocation, Dish> DISHES = load();
    private FoodCatalog() {}
    public static Dish find(ResourceLocation id) { return DISHES.get(id); }
    private static Map<ResourceLocation, Dish> load() {
        var stream = FoodCatalog.class.getResourceAsStream("/data/jemcompat/food_quality.json");
        if (stream == null) throw new IllegalStateException("Missing canonical food-quality projection");
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            var dishes = new LinkedHashMap<ResourceLocation, Dish>();
            for (var element : JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("assignments")) {
                var row = element.getAsJsonObject();
                var id = new ResourceLocation(row.get("item").getAsString());
                int tier = row.get("tier").getAsInt();
                var effects = row.getAsJsonArray("effects").asList().stream().map(value -> {
                    var effect = value.getAsJsonObject();
                    return new Effect(new ResourceLocation(effect.get("id").getAsString()), effect.get("amplifier").getAsInt());
                }).toList();
                if (tier < 1 || tier > 3 || effects.size() != tier || effects.stream().map(Effect::id).distinct().count() != tier)
                    throw new IllegalStateException("Invalid food assignment: " + id);
                if (dishes.put(id, new Dish(id, tier, row.get("family").getAsString(), effects)) != null)
                    throw new IllegalStateException("Duplicate food assignment: " + id);
            }
            return Map.copyOf(dishes);
        } catch (java.io.IOException e) { throw new IllegalStateException("Cannot read food assignments", e); }
    }
}
