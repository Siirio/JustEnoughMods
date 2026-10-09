package com.siirio.jemcompat.compat.jade;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Map;

public final class TamingDataLoader extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().create();
    private static final TamingDataLoader INSTANCE = new TamingDataLoader();
    private static volatile TamingCatalog catalog = TamingCatalog.empty();

    private TamingDataLoader() {
        super(GSON, "jemcompat/taming");
    }

    public static TamingDataLoader instance() {
        return INSTANCE;
    }

    public static TamingCatalog catalog() {
        return catalog;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> resources, ResourceManager manager, ProfilerFiller profiler) {
        BreedingFoodCatalog.clear();
        catalog = TamingCatalog.merge(resources.values().stream()
                .map(JsonElement::getAsJsonObject)
                .map(TamingCatalog::parse)
                .toList());
    }
}
