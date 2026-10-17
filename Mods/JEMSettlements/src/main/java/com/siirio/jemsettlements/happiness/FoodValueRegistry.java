package com.siirio.jemsettlements.happiness;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.food.FoodProperties;
import net.minecraftforge.registries.ForgeRegistries;

public final class FoodValueRegistry extends SimpleJsonResourceReloadListener {
    /** satiety=0 means derive the positive gain from the native FoodProperties. */
    public record Value(int mood, int satiety, String group, String classification) { }
    private static final Gson GSON = new GsonBuilder().create();
    private static volatile Map<ResourceLocation, Value> values = loadBuiltIn();
    public FoodValueRegistry() { super(GSON, "happiness/food_values"); }
    @Override protected void apply(Map<ResourceLocation, JsonElement> resources, ResourceManager manager, ProfilerFiller profiler) {
        Map<ResourceLocation, Value> loaded = new LinkedHashMap<>();
        resources.values().forEach(root -> { if (!root.isJsonArray()) return; root.getAsJsonArray().forEach(element -> { if (!element.isJsonObject()) return; JsonObject row = element.getAsJsonObject(); ResourceLocation id = ResourceLocation.tryParse(row.get("item").getAsString()); if (id == null || !ForgeRegistries.ITEMS.containsKey(id)) return; int mood = Math.max(-8, Math.min(5, row.get("mood").getAsInt())); int satiety = Math.max(-20, Math.min(20, row.has("satiety") ? row.get("satiety").getAsInt() : 0)); String group = row.has("group") ? row.get("group").getAsString() : "basic"; String classification = row.has("classification") ? row.get("classification").getAsString() : group; loaded.put(id, new Value(mood, satiety, group, classification)); }); });
        values = Map.copyOf(loaded);
    }
    public static Optional<Value> classify(ItemStack stack) {
        return classify(stack, null);
    }
    public static Optional<Value> classify(ItemStack stack, net.minecraft.world.entity.LivingEntity consumer) {
        if (stack.isEmpty()) return Optional.empty();
        FoodProperties food;
        try { food = stack.getItem().getFoodProperties(stack, consumer); }
        catch (RuntimeException dynamicFoodFailure) { return Optional.empty(); }
        if (food == null) return Optional.empty();
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem()); if (id == null) return Optional.empty();
        Value reviewed = values.get(id);
        boolean harmful = food.getEffects().stream().anyMatch(effect -> effect.getFirst().getEffect().getCategory() == net.minecraft.world.effect.MobEffectCategory.HARMFUL && effect.getSecond() > 0.0F);
        if (harmful) return Optional.of(new Value(reviewed == null ? (food.getNutrition() <= 2 ? -8 : -6) : Math.min(-1, reviewed.mood()), 1, id.toString(), "unsafe"));
        if (reviewed != null) return Optional.of(new Value(reviewed.mood(), reviewed.mood() < 0 ? 1 : reviewed.satiety(), id.toString(), reviewed.mood() < 0 ? "unsafe" : "modded_dish"));
        int mood = food.getNutrition() >= 8 ? 5 : food.getNutrition() >= 6 ? 3 : food.getNutrition() >= 3 ? 2 : 1;
        return Optional.of(new Value(mood, 0, id.toString(), "modded_dish"));
    }
    public static Map<ResourceLocation, Value> snapshot() { return values; }
    public static void applySynced(Map<ResourceLocation, Value> synced) { values = Map.copyOf(synced); }
    private static Map<ResourceLocation, Value> loadBuiltIn() {
        try (var stream = FoodValueRegistry.class.getResourceAsStream("/data/jem_settlements/happiness/food_values.json")) {
            if (stream == null) return Map.of();
            JsonElement root = com.google.gson.JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
            Map<ResourceLocation, Value> loaded = new LinkedHashMap<>();
            if (root.isJsonArray()) root.getAsJsonArray().forEach(element -> { if (!element.isJsonObject()) return; JsonObject row = element.getAsJsonObject(); ResourceLocation id = ResourceLocation.tryParse(row.get("item").getAsString()); if (id != null) loaded.put(id, new Value(Math.max(-8, Math.min(5, row.get("mood").getAsInt())), Math.max(-20, Math.min(20, row.has("satiety") ? row.get("satiety").getAsInt() : 0)), row.has("group") ? row.get("group").getAsString() : "basic", row.has("classification") ? row.get("classification").getAsString() : "reviewed")); });
            return Map.copyOf(loaded);
        } catch (Exception ignored) { return Map.of(); }
    }
}
