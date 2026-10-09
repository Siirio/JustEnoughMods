package com.siirio.jemserver.smp;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.siirio.jemworldbosstiers.api.WorldTierApi;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.loot.Deserializers;
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.Map;
import java.util.WeakHashMap;

public final class EventRewardPreview {
    private static final Map<LootTable, ListTag> PREVIEWS = new WeakHashMap<>();

    public static void add(ServerPlayer player, CompoundTag event, CompoundTag detail) {
        String type = event.getString("activity");
        String table =
                switch (type) {
                    case "COOKING_SHOW" -> event.getString("cookingReward");


                    default -> "";
                };
        detail.putString(
                "rewardMode",
                type.equals("RESOURCE_RUSH") ? "DOUBLE_ELIGIBLE_DROPS" : "POSSIBLE_LOOT");
        if(type.equals("BLOOD_MOON")) {
            detail.putString("rewardMode", "BLOOD_MOON_REWARDS");
            detail.put("reward", com.siirio.jemserver.smp.events.BloodMoonRewards.preview());
            return;
        }
        if(type.equals("FISHING")) { detail.putString("rewardMode","FISHING_XP"); return; }
        if (type.equals("BOSS_RAID")) {
            int tier = WorldTierApi.currentTier(player.server);
            detail.putInt("worldTier", tier);
            ListTag rewards = new ListTag();
            for (var stack : com.siirio.jemworldbosstiers.encounter.RaidRewardPool.preview(tier)) {
                CompoundTag item = new CompoundTag();
                item.putString("item", net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
                item.putInt("count", stack.getCount());
                item.putInt("minCount", stack.getCount());
                item.putInt("maxCount", stack.getCount());
                rewards.add(item);
            }
            detail.put("reward", rewards);
            detail.putString("personalRewardMode", "TWO_DISTINCT_RESOURCES_AND_BOOK");
            return;
        }
        if (type.equals("BOSS") && event.hasUUID("bossEntity")) {
            for (var level : player.server.getAllLevels()) {
                if (level.getEntity(event.getUUID("bossEntity")) instanceof net.minecraft.world.entity.Mob boss) {
                    table = boss.getLootTable().toString();
                    break;
                }
            }
        }
        if (!table.isEmpty()) detail.put("reward", preview(player, table));
    }

    private static ListTag preview(ServerPlayer player, String id) {
        var table = player.server.getLootData().getLootTable(new ResourceLocation(id));
        return PREVIEWS.computeIfAbsent(table, EventRewardPreview::read).copy();
    }

    private static ListTag read(LootTable table) {
        var result = new ListTag();
        var root =
                Deserializers.createLootTableSerializer()
                        .create()
                        .toJsonTree(table, LootTable.class)
                        .getAsJsonObject();
        if (!root.has("pools")) return result;
        for (var value : root.getAsJsonArray("pools")) {
            var pool = value.getAsJsonObject();
            if (!pool.has("entries")) continue;
            var alternatives = new java.util.LinkedHashMap<String, CompoundTag>();
            for (var entryValue : pool.getAsJsonArray("entries")) {
                var entry = entryValue.getAsJsonObject();
                if (!entry.has("type")
                        || !entry.get("type").getAsString().equals("minecraft:item")
                        || !entry.has("name")) continue;
                if (result.size() >= SmpConfig.PAGE_SIZE.get()) return result;
                var item = new CompoundTag();
                String itemId = entry.get("name").getAsString();
                if (itemId.equals("minecraft:book") && entry.has("functions")) {
                    for (var function : entry.getAsJsonArray("functions"))
                        if (function.getAsJsonObject().get("function").getAsString().equals("minecraft:enchant_with_levels")) itemId = "minecraft:enchanted_book";
                }
                item.putString("item", itemId);
                item.putInt("count", 1);
                item.putInt("minCount", 1);
                item.putInt("maxCount", 1);
                counts(item, entry.getAsJsonArray("functions"));
                counts(item, pool.getAsJsonArray("functions"));
                var prior = alternatives.putIfAbsent(itemId, item);
                if (prior != null) {
                    prior.putInt("minCount", Math.min(prior.getInt("minCount"), item.getInt("minCount")));
                    prior.putInt("maxCount", Math.max(prior.getInt("maxCount"), item.getInt("maxCount")));
                }
            }
            alternatives.values().forEach(result::add);
        }
        return result;
    }

    private static void counts(CompoundTag item, JsonArray functions) {
        if (functions == null) return;
        for (var value : functions) {
            JsonObject function = value.getAsJsonObject();
            if (!function.has("function")
                    || !function.get("function").getAsString().equals("minecraft:set_count")
                    || !function.has("count")) continue;
            var count = function.get("count");
            if (count.isJsonPrimitive()) {
                item.putInt("minCount", count.getAsInt());
                item.putInt("maxCount", count.getAsInt());
            } else if (count.isJsonObject()
                    && count.getAsJsonObject().has("min")
                    && count.getAsJsonObject().get("min").isJsonPrimitive()
                    && count.getAsJsonObject().has("max")
                    && count.getAsJsonObject().get("max").isJsonPrimitive()) {
                item.putInt("minCount", count.getAsJsonObject().get("min").getAsInt());
                item.putInt("maxCount", count.getAsJsonObject().get("max").getAsInt());
            }
            item.putInt("count", item.getInt("minCount"));
        }
    }

    private EventRewardPreview() {}
}
