package com.siirio.jemsettlements.happiness;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.food.FoodProperties;
import net.minecraftforge.registries.ForgeRegistries;

public final class FoodValueRegistry extends SimpleJsonResourceReloadListener {
    public record Value(int mood, String group, String classification) { }
    private static final Gson GSON = new GsonBuilder().create();
    private static volatile Map<ResourceLocation, Value> values = Map.of();
    public FoodValueRegistry() { super(GSON, "happiness/food_values"); }
    @Override protected void apply(Map<ResourceLocation, JsonElement> resources, ResourceManager manager, ProfilerFiller profiler) {
        Map<ResourceLocation, Value> loaded = new LinkedHashMap<>();
        resources.values().forEach(root -> { if (!root.isJsonArray()) return; root.getAsJsonArray().forEach(element -> { if (!element.isJsonObject()) return; JsonObject row = element.getAsJsonObject(); ResourceLocation id = ResourceLocation.tryParse(row.get("item").getAsString()); if (id == null || !ForgeRegistries.ITEMS.containsKey(id)) return; int mood = Math.max(-8, Math.min(5, row.get("mood").getAsInt())); String group = row.has("group") ? row.get("group").getAsString() : "basic"; String classification = row.has("classification") ? row.get("classification").getAsString() : group; loaded.put(id, new Value(mood, group, classification)); }); });
        values = Map.copyOf(loaded);
    }
    public static Optional<Value> classify(ItemStack stack) {
        if (stack.isEmpty() || stack.hasTag()) return Optional.empty();
        FoodProperties food = stack.getItem().getFoodProperties(stack, null); if (food == null) return Optional.empty();
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem()); if (id == null) return Optional.empty();
        return Optional.ofNullable(values.get(id)).or(() -> Optional.of(new Value(1, "basic", "safe_unknown_edible")));
    }
}
