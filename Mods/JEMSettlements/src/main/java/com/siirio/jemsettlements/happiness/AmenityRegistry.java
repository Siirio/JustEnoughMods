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
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;

public final class AmenityRegistry extends SimpleJsonResourceReloadListener {
    public record Amenity(String category, int cap, boolean requiresLight) { }
    private static final Gson GSON = new GsonBuilder().create();
    private static volatile Map<Block, Amenity> values = Map.of();
    public AmenityRegistry() { super(GSON, "happiness/amenities"); }
    @Override protected void apply(Map<ResourceLocation, JsonElement> resources, ResourceManager manager, ProfilerFiller profiler) {
        Map<Block, Amenity> loaded = new LinkedHashMap<>();
        resources.values().forEach(root -> { if (!root.isJsonArray()) return; root.getAsJsonArray().forEach(element -> { if (!element.isJsonObject()) return; JsonObject row = element.getAsJsonObject(); ResourceLocation id = ResourceLocation.tryParse(row.get("block").getAsString()); if (id == null || !ForgeRegistries.BLOCKS.containsKey(id)) return; String category = row.get("category").getAsString(); int cap = row.has("cap") ? Math.max(1, Math.min(3, row.get("cap").getAsInt())) : 3; boolean light = row.has("requires_light") && row.get("requires_light").getAsBoolean(); loaded.put(ForgeRegistries.BLOCKS.getValue(id), new Amenity(category, cap, light)); }); });
        values = Map.copyOf(loaded);
    }
    public static Optional<Amenity> get(Block block) { return Optional.ofNullable(values.get(block)); }
}
