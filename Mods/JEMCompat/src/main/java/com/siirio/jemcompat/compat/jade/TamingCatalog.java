package com.siirio.jemcompat.compat.jade;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class TamingCatalog {
    private final Map<ResourceLocation, List<ResourceLocation>> entries;

    private TamingCatalog(Map<ResourceLocation, List<ResourceLocation>> entries) {
        this.entries = Map.copyOf(entries);
    }

    public static TamingCatalog empty() {
        return new TamingCatalog(Map.of());
    }

    public static TamingCatalog parse(JsonObject json) {
        LinkedHashMap<ResourceLocation, List<ResourceLocation>> entries = new LinkedHashMap<>();
        for (var element : json.getAsJsonArray("entries")) {
            JsonObject entry = element.getAsJsonObject();
            ResourceLocation entity = parseId(entry.get("entity").getAsString());
            JsonArray items = entry.getAsJsonArray("items");
            List<ResourceLocation> itemIds = items.asList().stream()
                    .map(item -> parseId(item.getAsString()))
                    .toList();
            if (!itemIds.isEmpty()) {
                entries.put(entity, itemIds);
            }
        }
        return new TamingCatalog(entries);
    }

    public static TamingCatalog merge(Iterable<TamingCatalog> catalogs) {
        LinkedHashMap<ResourceLocation, List<ResourceLocation>> entries = new LinkedHashMap<>();
        catalogs.forEach(catalog -> entries.putAll(catalog.entries));
        return new TamingCatalog(entries);
    }

    public List<ResourceLocation> itemsFor(ResourceLocation entityId) {
        return entries.getOrDefault(entityId, List.of());
    }

    private static ResourceLocation parseId(String value) {
        ResourceLocation id = ResourceLocation.tryParse(value);
        if (id == null) {
            throw new JsonParseException("Invalid resource location: " + value);
        }
        return id;
    }
}
