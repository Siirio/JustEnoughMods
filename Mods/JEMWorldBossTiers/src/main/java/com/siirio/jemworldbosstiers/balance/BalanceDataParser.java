package com.siirio.jemworldbosstiers.balance;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class BalanceDataParser {
    private BalanceDataParser() {
    }

    public static ParsedBossData parseBosses(JsonObject root) {
        List<BossProfile> profiles = new ArrayList<>();
        ResourceLocation defaultOffering = root.has("default_arena_revival_offering") ? id(root, "default_arena_revival_offering") : null;
        int defaultRevivalXp = GsonHelper.getAsInt(root, "default_arena_revival_xp_levels", 0);
        for (JsonElement element : GsonHelper.getAsJsonArray(root, "bosses")) {
            JsonObject object = element.getAsJsonObject();
            profiles.add(new BossProfile(
                    id(object, "key"),
                    GsonHelper.getAsString(object, "display_name"),
                    ids(GsonHelper.getAsJsonArray(object, "entity_ids")),
                    ids(GsonHelper.getAsJsonArray(object, "victory_advancement_ids", new JsonArray())),
                    GsonHelper.getAsBoolean(object, "scales_with_world_tier", true),
                    GsonHelper.getAsBoolean(object, "counts_toward_world_tier", true),
                    GsonHelper.getAsDouble(object, "native_threat", 0.0D),
                    GsonHelper.getAsString(object, "hp_scaling_safety", "UNKNOWN"),
                    attributeValues(GsonHelper.getAsJsonObject(object, "attributes", new JsonObject())),
                    optionalValues(object, "special_damage_multiplier"),
                    GsonHelper.getAsString(object, "damage_adapter", "standard"),
                    GsonHelper.getAsString(object, "healing_adapter", "none"),
                    GsonHelper.getAsString(object, "stagger_adapter", "none"),
                    GsonHelper.getAsString(object, "arena_strategy", "none"),
                    GsonHelper.getAsString(object, "revival_strategy", "none"),
                    GsonHelper.getAsString(object, "anti_cheese_profile", "native"),
                    GsonHelper.getAsString(object, "destruction_rules", "native"),
                    GsonHelper.getAsInt(object, "arena_radius", 96),
                    object.has("revival_offering") ? id(object, "revival_offering") : defaultOffering,
                    GsonHelper.getAsInt(object, "revival_xp_levels", defaultRevivalXp)
            ));
        }
        List<Integer> thresholds = root.has("tier_thresholds") ? integers(root.getAsJsonArray("tier_thresholds")) : List.of();
        return new ParsedBossData(profiles, thresholds);
    }

    public static List<RewardProfile> parseRewards(JsonObject root) {
        List<RewardProfile> profiles = new ArrayList<>();
        for (JsonElement element : GsonHelper.getAsJsonArray(root, "rewards")) {
            JsonObject object = element.getAsJsonObject();
            profiles.add(new RewardProfile(
                    id(object, "item_id"),
                    id(object, "source_boss"),
                    GsonHelper.getAsString(object, "category")
            ));
        }
        return List.copyOf(profiles);
    }

    private static ResourceLocation id(JsonObject object, String name) {
        ResourceLocation value = ResourceLocation.tryParse(GsonHelper.getAsString(object, name));
        if (value == null) {
            throw new IllegalArgumentException("Invalid resource location for " + name);
        }
        return value;
    }

    private static List<ResourceLocation> ids(JsonArray array) {
        List<ResourceLocation> values = new ArrayList<>();
        array.forEach(element -> {
            ResourceLocation value = ResourceLocation.tryParse(element.getAsString());
            if (value == null) {
                throw new IllegalArgumentException("Invalid resource location " + element.getAsString());
            }
            values.add(value);
        });
        return List.copyOf(values);
    }

    private static List<Integer> integers(JsonArray array) {
        List<Integer> values = new ArrayList<>();
        array.forEach(element -> values.add(element.getAsInt()));
        return List.copyOf(values);
    }

    private static Map<ResourceLocation, TierValues> attributeValues(JsonObject object) {
        Map<ResourceLocation, TierValues> values = new HashMap<>();
        object.entrySet().forEach(entry -> {
            ResourceLocation id = ResourceLocation.tryParse(entry.getKey());
            if (id == null) {
                throw new IllegalArgumentException("Invalid attribute ID " + entry.getKey());
            }
            values.put(id, values(entry.getValue().getAsJsonArray()));
        });
        return Map.copyOf(values);
    }

    private static TierValues optionalValues(JsonObject object, String name) {
        return object.has(name) ? values(object.getAsJsonArray(name)) : null;
    }

    private static TierValues values(JsonArray array) {
        if (array.size() != TierValues.TIER_COUNT) {
            throw new IllegalArgumentException("Tier values must contain exactly five entries");
        }
        return new TierValues(array.get(0).getAsDouble(), array.get(1).getAsDouble(), array.get(2).getAsDouble(), array.get(3).getAsDouble(), array.get(4).getAsDouble());
    }
}
