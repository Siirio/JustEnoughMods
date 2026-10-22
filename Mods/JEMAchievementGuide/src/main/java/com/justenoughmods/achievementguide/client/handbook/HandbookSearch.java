package com.justenoughmods.achievementguide.client.handbook;

import com.google.gson.JsonParser;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;

/** Search-only bilingual aliases; gameplay facts continue to belong to their domains. */
public final class HandbookSearch extends SimplePreparableReloadListener<Map<ResourceLocation, String>> {
    private static volatile Map<ResourceLocation, String> aliases = Map.of();

    static void reloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new HandbookSearch());
    }

    public static boolean matches(ResourceLocation entry, String query) {
        String needle = query.toLowerCase(Locale.ROOT).strip();
        return entry.toString().contains(needle) || aliases.getOrDefault(entry, "").contains(needle);
    }

    @Override
    protected Map<ResourceLocation, String> prepare(ResourceManager resources, ProfilerFiller profiler) {
        Map<ResourceLocation, String> result = new HashMap<>();
        String base = "patchouli_books/just_enough_guide/";
        for (String language : List.of("en_us", "ru_ru")) {
            Map<ResourceLocation, JsonObject> categories = new HashMap<>();
            String categoryPrefix = base + language + "/categories/";
            resources.listResources(categoryPrefix, id -> "jem_advancements".equals(id.getNamespace()) && id.getPath().endsWith(".json"))
                    .forEach((id, resource) -> {
                        try (var reader = resource.openAsReader()) {
                            String path = id.getPath().substring(categoryPrefix.length(), id.getPath().length() - 5);
                            categories.put(new ResourceLocation("jem_advancements", path), JsonParser.parseReader(reader).getAsJsonObject());
                        } catch (IOException | RuntimeException exception) {
                            throw new IllegalStateException("Invalid handbook search category: " + id, exception);
                        }
                    });
            String prefix = base + language + "/entries/";
            resources.listResources(prefix, id -> "jem_advancements".equals(id.getNamespace()) && id.getPath().endsWith(".json"))
                    .forEach((id, resource) -> {
                        try (var reader = resource.openAsReader()) {
                            var json = JsonParser.parseReader(reader).getAsJsonObject();
                            String path = id.getPath().substring(prefix.length(), id.getPath().length() - 5);
                            ResourceLocation key = new ResourceLocation("jem_advancements", path);
                            String text = json.has("name") ? json.get("name").getAsString() : "";
                            if (json.has("jem_aliases")) for (var alias : json.getAsJsonArray("jem_aliases")) text += " " + alias.getAsString();
                            var visited = new HashSet<ResourceLocation>();
                            ResourceLocation categoryId = json.has("category") ? new ResourceLocation(json.get("category").getAsString()) : null;
                            while (categoryId != null && visited.add(categoryId)) {
                                JsonObject category = categories.get(categoryId);
                                if (category == null) break;
                                text += " " + categoryId;
                                if (category.has("name")) text += " " + category.get("name").getAsString();
                                categoryId = category.has("parent") ? new ResourceLocation(category.get("parent").getAsString()) : null;
                            }
                            result.merge(key, text.toLowerCase(Locale.ROOT), (a, b) -> a + " " + b);
                        } catch (IOException | RuntimeException exception) {
                            throw new IllegalStateException("Invalid handbook search source: " + id, exception);
                        }
                    });
        }
        return Map.copyOf(result);
    }

    @Override
    protected void apply(Map<ResourceLocation, String> prepared, ResourceManager resources, ProfilerFiller profiler) {
        aliases = prepared;
    }
}
