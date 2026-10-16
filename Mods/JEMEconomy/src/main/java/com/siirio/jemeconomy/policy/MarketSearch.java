package com.siirio.jemeconomy.policy;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import net.minecraftforge.registries.ForgeRegistries;

/** Packaged native/overlay aliases make search independent of the server's language. */
public final class MarketSearch {
    private static final Map<String, String> ITEMS;
    private static final Map<String, String> PAINTINGS;
    static {
        try (var stream = MarketSearch.class.getResourceAsStream("/data/jem_economy/economy/market_search_terms.json")) {
            if (stream == null) throw new IllegalStateException("Missing market search terms");
            var definition = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            ITEMS = terms(definition.getAsJsonObject("items"));
            PAINTINGS = terms(definition.getAsJsonObject("paintings"));
        } catch (java.io.IOException failure) { throw new IllegalStateException("Cannot load market search terms", failure); }
    }
    private MarketSearch() { }

    public static boolean matches(MarketSupply.Listing listing, String search) {
        if (search.isBlank() || listing.id().toString().contains(search)) return true;
        var item = ForgeRegistries.ITEMS.getKey(listing.stack().getItem());
        if (item != null && ITEMS.getOrDefault(item.toString(), "").contains(search)) return true;
        var entityTag = listing.stack().getTagElement("EntityTag");
        if (entityTag == null || !entityTag.contains("variant")) return false;
        var variant = net.minecraft.resources.ResourceLocation.tryParse(entityTag.getString("variant"));
        if (variant == null) return false;
        String key = "painting." + variant.getNamespace() + "." + variant.getPath() + ".title";
        return PAINTINGS.getOrDefault(key, "").contains(search);
    }
    private static Map<String, String> terms(JsonObject section) {
        Map<String, String> result = new HashMap<>();
        for (var entry : section.entrySet()) {
            var values = entry.getValue().getAsJsonArray();
            if (values.size() > 32) throw new IllegalStateException("Too many market search aliases for " + entry.getKey());
            StringBuilder joined = new StringBuilder();
            for (var value : values) {
                String text = value.getAsString();
                if (text.length() > 1024) throw new IllegalStateException("Oversized market search alias for " + entry.getKey());
                joined.append(text.toLowerCase(Locale.ROOT)).append('\n');
            }
            result.put(entry.getKey(), joined.toString());
        }
        return Map.copyOf(result);
    }
}
