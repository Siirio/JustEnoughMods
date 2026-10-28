package com.siirio.jemanimalhints.compat.jade;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class TamingCatalog {
    public record Entry(List<ResourceLocation> items, boolean mountable) {
        public Entry {
            items = List.copyOf(items);
        }
    }

    private final Map<ResourceLocation, Entry> entries;

    private TamingCatalog(Map<ResourceLocation, Entry> entries) {
        this.entries = Map.copyOf(entries);
    }

    public static TamingCatalog empty() {
        return new TamingCatalog(Map.of());
    }

    public static TamingCatalog parse(JsonObject json) {
        LinkedHashMap<ResourceLocation, Entry> entries = new LinkedHashMap<>();
        for (var element : json.getAsJsonArray("entries")) {
            JsonObject entry = element.getAsJsonObject();
            ResourceLocation entity = parseId(entry.get("entity").getAsString());
            JsonArray items = entry.getAsJsonArray("items");
            List<ResourceLocation> itemIds = items.asList().stream()
                    .map(item -> parseId(item.getAsString()))
                    .toList();
            boolean mountable = entry.has("mountable") && entry.get("mountable").getAsBoolean();
            if (!itemIds.isEmpty() || mountable) {
                entries.put(entity, new Entry(itemIds, mountable));
            }
        }
        return new TamingCatalog(entries);
    }

    public static TamingCatalog merge(Iterable<TamingCatalog> catalogs) {
        LinkedHashMap<ResourceLocation, Entry> entries = new LinkedHashMap<>();
        catalogs.forEach(catalog -> entries.putAll(catalog.entries));
        return new TamingCatalog(entries);
    }

    public Entry entryFor(ResourceLocation entityId) {
        return entries.getOrDefault(entityId, new Entry(List.of(), false));
    }

    private static ResourceLocation parseId(String value) {
        ResourceLocation id = ResourceLocation.tryParse(value);
        if (id == null) {
            throw new JsonParseException("Invalid resource location: " + value);
        }
        return id;
    }
}
