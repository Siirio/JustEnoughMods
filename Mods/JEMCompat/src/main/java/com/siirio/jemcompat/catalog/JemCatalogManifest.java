package com.siirio.jemcompat.catalog;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Loads the reviewed, exact JEI item-to-leaf-category assignments. */
final class JemCatalogManifest {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String RESOURCE = "/assets/jemcompat/jei_catalog.json";
    private static final int SCHEMA_VERSION = 2;
    private static final int UNKNOWN_LOG_LIMIT = 25;
    private static final Set<JemCatalogCategories.Kind> PARENTS = Set.of(
            JemCatalogCategories.Kind.WEAPONS, JemCatalogCategories.Kind.FOOD,
            JemCatalogCategories.Kind.WEARABLES, JemCatalogCategories.Kind.BUILDING);
    private static final LoadedManifest MANIFEST = load();
    private static final Map<ResourceLocation, JemCatalogCategories.Kind> ASSIGNMENTS = MANIFEST.assignments();
    private static final Map<ResourceLocation, Set<JemCatalogCategories.Kind>> ADDITIONAL_VIEWS = MANIFEST.additionalViews();
    private static final Set<ResourceLocation> REPORTED_UNKNOWN = new HashSet<>();

    private JemCatalogManifest() {
    }

    static JemCatalogCategories.Kind category(ItemStack stack) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (id == null) {
            return JemCatalogCategories.Kind.OTHER;
        }
        JemCatalogCategories.Kind category = ASSIGNMENTS.get(id);
        if (category != null) {
            return category;
        }
        synchronized (REPORTED_UNKNOWN) {
            if (REPORTED_UNKNOWN.add(id) && REPORTED_UNKNOWN.size() <= UNKNOWN_LOG_LIMIT) {
                LOGGER.warn("JEI catalog has no reviewed assignment for {}; using OTHER", id);
            }
        }
        return JemCatalogCategories.Kind.OTHER;
    }

    static Set<JemCatalogCategories.Kind> additionalCategories(ItemStack stack) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return id == null ? Set.of() : ADDITIONAL_VIEWS.getOrDefault(id, Set.of());
    }

    private static LoadedManifest load() {
        InputStream stream = JemCatalogManifest.class.getResourceAsStream(RESOURCE);
        if (stream == null) {
            throw new IllegalStateException("Missing JEI catalog manifest " + RESOURCE);
        }
        try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            if (root.get("schema_version").getAsInt() != SCHEMA_VERSION) {
                throw new IllegalStateException("Unsupported JEI catalog schema");
            }
            JsonObject categories = root.getAsJsonObject("assignments");
            EnumMap<JemCatalogCategories.Kind, Boolean> present = new EnumMap<>(JemCatalogCategories.Kind.class);
            Map<ResourceLocation, JemCatalogCategories.Kind> result = new HashMap<>();
            for (Map.Entry<String, JsonElement> entry : categories.entrySet()) {
                JemCatalogCategories.Kind category = JemCatalogCategories.Kind.valueOf(entry.getKey());
                if (PARENTS.contains(category)) {
                    throw new IllegalStateException("JEI parent category cannot own items: " + category);
                }
                present.put(category, true);
                for (JsonElement value : entry.getValue().getAsJsonArray()) {
                    ResourceLocation id = ResourceLocation.tryParse(value.getAsString());
                    if (id == null) {
                        throw new IllegalStateException("Invalid JEI catalog item ID " + value);
                    }
                    JemCatalogCategories.Kind previous = result.putIfAbsent(id, category);
                    if (previous != null) {
                        throw new IllegalStateException("Duplicate JEI catalog item " + id + " in " + previous + " and " + category);
                    }
                }
            }
            for (JemCatalogCategories.Kind category : JemCatalogCategories.Kind.values()) {
                if (!PARENTS.contains(category) && !present.containsKey(category)) {
                    throw new IllegalStateException("Missing JEI leaf category " + category);
                }
            }
            if (root.get("item_count").getAsInt() != result.size()) {
                throw new IllegalStateException("JEI catalog item_count does not match its assignments");
            }
            Map<ResourceLocation, EnumSet<JemCatalogCategories.Kind>> additional = new HashMap<>();
            JsonObject additionalViews = root.getAsJsonObject("additional_views");
            if (additionalViews == null) {
                throw new IllegalStateException("Missing JEI additional_views");
            }
            for (Map.Entry<String, JsonElement> entry : additionalViews.entrySet()) {
                JemCatalogCategories.Kind view = JemCatalogCategories.Kind.valueOf(entry.getKey());
                if (PARENTS.contains(view)) {
                    throw new IllegalStateException("JEI parent category cannot be an additional view: " + view);
                }
                Set<ResourceLocation> seen = new HashSet<>();
                for (JsonElement value : entry.getValue().getAsJsonArray()) {
                    ResourceLocation id = ResourceLocation.tryParse(value.getAsString());
                    if (id == null || !result.containsKey(id)) {
                        throw new IllegalStateException("Invalid JEI additional-view item " + value);
                    }
                    if (!seen.add(id)) {
                        throw new IllegalStateException("Duplicate JEI additional-view item " + id + " in " + view);
                    }
                    if (result.get(id) == view) {
                        throw new IllegalStateException("Redundant JEI additional-view item " + id + " in " + view);
                    }
                    additional.computeIfAbsent(id, ignored -> EnumSet.noneOf(JemCatalogCategories.Kind.class)).add(view);
                }
            }
            Map<ResourceLocation, Set<JemCatalogCategories.Kind>> immutableAdditional = new HashMap<>();
            additional.forEach((id, views) -> immutableAdditional.put(id, Set.copyOf(views)));
            LOGGER.info("Loaded {} reviewed JEI catalog assignments and {} additional-view entries",
                    result.size(), immutableAdditional.size());
            return new LoadedManifest(Map.copyOf(result), Map.copyOf(immutableAdditional));
        } catch (RuntimeException exception) {
            throw new IllegalStateException("Cannot load JEI catalog manifest", exception);
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot read JEI catalog manifest", exception);
        }
    }

    private record LoadedManifest(Map<ResourceLocation, JemCatalogCategories.Kind> assignments,
                                  Map<ResourceLocation, Set<JemCatalogCategories.Kind>> additionalViews) {
    }
}
