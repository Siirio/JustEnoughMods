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
    private static volatile Map<net.minecraft.tags.TagKey<Block>, Amenity> tags = Map.of();
    public AmenityRegistry() { super(GSON, "happiness/amenities"); }
    @Override protected void apply(Map<ResourceLocation, JsonElement> resources, ResourceManager manager, ProfilerFiller profiler) {
        Map<Block, Amenity> loaded = new LinkedHashMap<>();
        Map<net.minecraft.tags.TagKey<Block>, Amenity> loadedTags = new LinkedHashMap<>();
        resources.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(resource -> {
            JsonElement root = resource.getValue(); if (!root.isJsonArray()) return;
            root.getAsJsonArray().forEach(element -> {
                if (!element.isJsonObject()) return;
                JsonObject row = element.getAsJsonObject();
                String field = row.has("block") ? "block" : row.has("tag") ? "tag" : "";
                if (field.isEmpty() || !row.has("category")) return;
                ResourceLocation id = ResourceLocation.tryParse(row.get(field).getAsString()); if (id == null) return;
                String category = row.get("category").getAsString();
                int cap = row.has("cap") ? Math.max(1, Math.min(3, row.get("cap").getAsInt())) : 3;
                var amenity = new Amenity(category, cap, row.has("requires_light") && row.get("requires_light").getAsBoolean());
                if (field.equals("tag")) loadedTags.put(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BLOCK, id), amenity);
                else if (ForgeRegistries.BLOCKS.containsKey(id)) loaded.put(ForgeRegistries.BLOCKS.getValue(id), amenity);
            });
        });
        values = Map.copyOf(loaded); tags = java.util.Collections.unmodifiableMap(loadedTags);
        CityComfortService.clear();
    }
    public static Optional<Amenity> get(net.minecraft.world.level.block.state.BlockState state) {
        var exact = get(state.getBlock()); if (exact.isPresent()) return exact;
        for (var entry : tags.entrySet()) if (state.is(entry.getKey())) return Optional.of(entry.getValue());
        Block block = state.getBlock();
        if (block instanceof net.minecraft.world.level.block.CarpetBlock) return Optional.of(new Amenity("comfort", 3, false));
        if (block instanceof net.minecraft.world.level.block.FlowerPotBlock pot && pot.getContent() != net.minecraft.world.level.block.Blocks.AIR)
            return Optional.of(new Amenity("greenery", 3, false));
        if (block instanceof net.minecraft.world.level.block.CandleBlock || block instanceof net.minecraft.world.level.block.CandleCakeBlock)
            return Optional.of(new Amenity("warmth_light", 3, true));
        return Optional.empty();
    }

    public static boolean active(net.minecraft.server.level.ServerLevel level, net.minecraft.core.BlockPos pos,
                                 net.minecraft.world.level.block.state.BlockState state, Amenity amenity) {
        var id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        if (id != null && id.toString().equals("fairylights:fastener"))
            return net.minecraftforge.fml.ModList.get().isLoaded("fairylights") && com.siirio.jemsettlements.compat.FairyLightsEvidence.decorated(level,pos);
        return !amenity.requiresLight() || state.getLightEmission(level,pos) > 0;
    }
    public static Optional<Amenity> get(Block block) { return Optional.ofNullable(values.get(block)); }
}
