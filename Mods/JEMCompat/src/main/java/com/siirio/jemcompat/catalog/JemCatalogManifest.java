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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Loads the reviewed, exact JEI item-to-leaf-category assignments. */
final class JemCatalogManifest {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String RESOURCE = "/assets/jemcompat/jei_catalog.json";
    private static final int SCHEMA_VERSION = 1;
    private static final int UNKNOWN_LOG_LIMIT = 25;
    private static final Set<JemCatalogCategories.Kind> PARENTS = Set.of(
            JemCatalogCategories.Kind.WEAPONS, JemCatalogCategories.Kind.FOOD,
            JemCatalogCategories.Kind.BUILDING);
    private static final Map<ResourceLocation, JemCatalogCategories.Kind> ASSIGNMENTS = load();
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

    private static Map<ResourceLocation, JemCatalogCategories.Kind> load() {
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
            LOGGER.info("Loaded {} reviewed JEI catalog assignments", result.size());
            return Map.copyOf(result);
        } catch (RuntimeException exception) {
            throw new IllegalStateException("Cannot load JEI catalog manifest", exception);
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot read JEI catalog manifest", exception);
        }
    }
}
