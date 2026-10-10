package com.siirio.jemserver.smp;

import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;

public final class SmpViews {
    public static CompoundTag build(ServerPlayer player, SmpQuery query) {
        var data = SmpData.get(player.server);
        var result = new CompoundTag();
        result.putString("tab", query.tab());
        result.putString("filter", query.filter());
        result.putString("querySearch", query.search());
        result.putInt("page", query.page());
        result.putUUID("viewer", player.getUUID());
        int pendingRewards = com.siirio.jemworldbosstiers.encounter.PendingRewardContainer.count(player);
        result.putInt("pendingRewards", pendingRewards);
        if (query.tab().equals("prizes")) {
            int pageSize = SmpConfig.PAGE_SIZE.get();
            var rewards = com.siirio.jemworldbosstiers.encounter.PendingRewardContainer.bundles(player, query.page() * pageSize, pageSize);
            result.put("rows", rewards);
            result.putInt("total", pendingRewards);
            result.putInt("totalItems", com.siirio.jemworldbosstiers.encounter.PendingRewardContainer.totalItems(player));
            result.putInt("pageSize", pageSize);
            return result;
        }
        if (query.tab().equals("shops")) {
            var shops = com.siirio.jemserver.smp.shops.ShopIndex.view(player, query);
            shops.putInt("pendingRewards", pendingRewards);
            return shops;
        }
        if (query.selected() == null) {
        Collection<CompoundTag> candidates;
        if (query.tab().equals("profiles")) candidates = data.all("profiles");
        else candidates = data.all(query.tab());
        String search = query.search().toLowerCase(Locale.ROOT);
        var matches =
                candidates.stream()
                        .filter(row -> visible(player, query, row))
                        .filter(row -> searchable(row).contains(search))
                        .sorted(
                                query.tab().equals("events")
                                        ? Comparator.comparingInt((CompoundTag row) -> row.getString("state").equals("ACTIVE") ? 0 : 1)
                                                .thenComparingLong(row -> row.getLong("planned"))
                                        : Comparator.comparingLong((CompoundTag row) -> row.getLong("created")).reversed())
                        .toList();
        result.putInt("total", matches.size());
        result.putInt("pageSize", SmpConfig.PAGE_SIZE.get());
        var rows = new ListTag();
        int size = SmpConfig.PAGE_SIZE.get();
        matches.stream()
                .skip((long) query.page() * size)
                .limit(size)
                .forEach(
                        row -> {
                            var summary = summary(row);
                            if (query.tab().equals("parties")) organizer(player, row, summary);
                            if (query.tab().equals("events")) eventSummary(row, summary);
                            if (query.tab().equals("profiles"))
                                summary.putString(
                                        "state",
                                        player.server.getPlayerList().getPlayer(row.getUUID("id"))
                                                        == null
                                                ? "OFFLINE"
                                                : "ONLINE");
                            rows.add(summary);
                        });
        result.put("rows", rows);
        }
        if (query.selected() != null) {
            CompoundTag row = data.find(query.tab(), query.selected());
            if (row != null && authorized(player, query.tab(), row)) {
                var detail =
                        query.tab().equals("profiles")
                                ? Profiles.view(player, query.selected())
                                : query.tab().equals("parties")
                                        ? Parties.view(player, row)
                                        : detail(row);
                if (query.tab().equals("parties")) {
                    organizer(player, row, detail);
                    CompoundTag event = row.hasUUID("eventId") ? data.find("events", row.getUUID("eventId")) : null;
                    eventPresentation(player, event == null ? row : event, detail);
                    if (event != null) detail.putString("eventActivity", event.getString("activity"));
                    if (event != null && event.contains("dimension")) {
                        detail.putString("dimension", event.getString("dimension"));
                        detail.putLong("position", event.getLong("position"));
                    }
                    if (!detail.contains("bossType") && row.contains("structureBoss")) detail.putString("bossType", row.getString("structureBoss"));
                    if (!detail.contains("bossType") && row.hasUUID("bossEntity")) {
                        for (var level : player.server.getAllLevels()) {
                            var boss = level.getEntity(row.getUUID("bossEntity"));
                            if (boss != null) { detail.putString("bossType", net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(boss.getType()).toString()); break; }
                        }
                    }
                }
                boolean privateLocation =
                        !query.tab().equals("parties") || Parties.accepted(row, player.getUUID());
                if (!privateLocation) {
                    detail.remove("dimension");
                    detail.remove("position");
                    detail.putBoolean("meaningfulLocation", false);
                }
                detail.remove("deliveries");
                detail.remove("placements");
                if (row.getString("activity").equals("FISHING"))
                    detail.put(
                            "personalScore",
                            row.getCompound("scores")
                                    .getCompound(player.getUUID().toString())
                                    .copy());
                detail.remove("scores");
                detail.remove("mobs");
                if (row.contains("rewardPreview"))
                    detail.put("reward", row.getList("rewardPreview", Tag.TAG_COMPOUND).copy());
                if (query.tab().equals("events")) {
                    eventPresentation(player, row, detail);
                    com.siirio.jemserver.smp.events.CookingShow.view(player, row, detail);
                }
                detail.put("actions", actions(player, query.tab(), row));
                result.put("detail", detail);
            }
        }
        return result;
    }

    private static void organizer(ServerPlayer player, CompoundTag row, CompoundTag result) {
        if (!row.hasUUID("owner")) return;
        String name = Profiles.get(player.server, row.getUUID("owner")).getString("name");
        if (!name.isBlank()) result.putString("organizerName", name);
    }

    private static CompoundTag detail(CompoundTag row) {
        var result = new CompoundTag();
        for (String key :
                List.of(
                        "id",
                        "owner",
                        "revision",
                        "created",
                        "title",
                        "description",
                        "category",
                        "activity",
                        "name",
                        "state",
                        "language",
                        "scheduled",
                        "planned",
                        "started",
                        "ends",
                        "deadline",
                        "dimension",
                        "position",
                        "radius",
                        "members",
                        "limit",
                        "solo",
                        "approval",
                        "wave",
                        "party",
                        "bossEntity",
                        "bossType",
                        "combatStarted",
                        "eventId",
                        "bloodMoon",
                        "structureBoss",
                        "raid",
                        "cookingRequirements",
                        "trackerFull")) if (row.contains(key)) result.put(key, row.get(key).copy());
        return result;
    }

    private static CompoundTag summary(CompoundTag row) {
        var copy = new CompoundTag();
        for (String key :
                List.of(
                        "id",
                        "owner",
                        "title",
                        "name",
                        "state",
                        "revision",
                        "activity",
                        "category",
                        "planned",
                        "started",
                        "ends",
                        "dimension",
                        "position",
                        "bossType",
                        "created",
                        "limit")) if (row.contains(key)) copy.put(key, row.get(key).copy());
        copy.putInt("joined", row.getCompound("members").size());
        return copy;
    }

    private static String searchable(CompoundTag row) {
        return (row.getString("title")
                        + " "
                        + row.getString("name")
                        + " "
                        + row.getString("category")
                        + " "
                        + row.getString("activity")
                        + " "
                        + row.getString("description"))
                .toLowerCase(Locale.ROOT);
    }

    private static boolean authorized(ServerPlayer player, String table, CompoundTag row) {
        if (table.equals("parties")
                && (row.getString("state").equals("DRAFT") || row.getBoolean("solo")))
            return row.getUUID("owner").equals(player.getUUID())
                    || SmpRecords.member(row, player.getUUID());
        return true;
    }

    private static boolean visible(ServerPlayer player, SmpQuery query, CompoundTag row) {
        boolean owner = row.hasUUID("owner") && row.getUUID("owner").equals(player.getUUID());
        boolean member = SmpRecords.member(row, player.getUUID());
        if (query.tab().equals("parties")
                && (row.getString("state").equals("DRAFT") || row.getBoolean("solo"))
                && !owner
                && !member) return false;
        if (query.filter().startsWith("owner:"))
            return row.hasUUID("owner")
                    && row.getUUID("owner").toString().equals(query.filter().substring(6));
        return switch (query.filter()) {
            case "mine" -> owner;
            case "accepted" -> member;
            case "history", "past" -> SmpData.closed(row);
            case "active" -> row.getString("state").equals("ACTIVE");
            case "upcoming" -> row.getString("state").equals("PLANNED");
            default -> !SmpData.closed(row);
        };
    }

    private static ListTag actions(ServerPlayer player, String table, CompoundTag row) {
        var result = new ListTag();
        boolean owner = row.getUUID("owner").equals(player.getUUID()),
                member = SmpRecords.member(row, player.getUUID());
        boolean closed = SmpData.closed(row);
        if (table.equals("profiles")) {
            add(result, "message", "invite", "view_shops", "territory_rights");
            return result;
        }
        if (!closed && table.equals("parties") && !row.getString("state").equals("ACTIVE")) {
            if (owner) {
                if(row.getString("activity").equals("BOSS")) add(result, "start");
                add(result, "dissolve");
                if (!row.getBoolean("solo")) add(result, "invite");
            } else if (!member && !row.getBoolean("solo") && row.getString("state").equals("PUBLISHED")) add(result, "join");
            else if (member) add(result, "leave");
            if (row.getString("activity").equals("BOSS")&&Parties.accepted(row, player.getUUID())) add(result, "ready");
            if (Parties.accepted(row, player.getUUID())) add(result, "arrive");
        }
        if (table.equals("events") && !closed && Set.of("BOSS_RAID", "BLOOD_MOON").contains(row.getString("activity"))
                && !row.getBoolean("combatStarted") && !row.hasUUID("bossEntity")) {
            var party = row.hasUUID("party") ? SmpData.get(player.server).find("parties", row.getUUID("party")) : null;
            if (party == null || SmpData.closed(party) || !party.getBoolean("solo") || Parties.accepted(party, player.getUUID())) {
                add(result, "create_party");
                if (party == null || SmpData.closed(party) || party.getUUID("owner").equals(player.getUUID()) && SmpRecords.members(party).size() == 1) add(result, "solo");
            }
        }
        boolean authorized = table.equals("events") || owner || table.equals("parties") && Parties.accepted(row, player.getUUID());
        var location = row.hasUUID("eventId") ? SmpData.get(player.server).find("events", row.getUUID("eventId")) : row;
        if(table.equals("events")&&!closed&&meaningfulLocation(row)&&Set.of("BLOOD_MOON","BOSS_RAID","RESOURCE_RUSH").contains(row.getString("activity"))) add(result,"teleport_event");
        if (authorized && location != null && meaningfulLocation(location) && !closed) add(result, "navigate");
        return result;
    }

    private static boolean meaningfulLocation(CompoundTag row) {
        return row.contains("dimension") && row.contains("position")
                && !Set.of("FISHING", "COOKING_SHOW").contains(row.getString("activity"));
    }

    private static void eventSummary(CompoundTag row, CompoundTag detail) {
        EventDescriptions.add(row, detail);
        detail.putBoolean("meaningfulLocation", meaningfulLocation(row));
        if (row.contains("bossType")) detail.putString("bossType", row.getString("bossType"));
        String state = row.getString("state");
        boolean combat = Set.of("BOSS_RAID", "BLOOD_MOON", "BOSS").contains(row.getString("activity"));
        detail.putString("lifecycle", state.equals("PLANNED") ? "SCHEDULED"
                : state.equals("ACTIVE") && combat && !row.getBoolean("combatStarted") && !row.hasUUID("bossEntity") && !row.getString("activity").equals("BOSS") ? "STAGING" : state);
    }

    private static void eventPresentation(ServerPlayer player, CompoundTag row, CompoundTag detail) {
        eventSummary(row, detail);
        boolean combat = Set.of("BOSS_RAID", "BLOOD_MOON", "BOSS").contains(row.getString("activity"));
        var rules = new ListTag();
        if (combat) {
            if (row.getString("activity").equals("BLOOD_MOON")) {
                rule(rules, "waves", com.siirio.jemserver.smp.events.BloodMoonWaves.WAVES);
                rule(rules, "safe_deaths", com.siirio.jemserver.smp.events.EventRules.SAFE_DEATHS.get());
            } else rule(rules, "start");
            rule(rules, "no_late_join");
            rule(rules, "no_friendly_fire");
            rule(rules, "eligible_rewards");
        } else if (row.getString("activity").equals("RESOURCE_RUSH")) {
            rule(rules, "resource_rush");
            rule(rules, "rush_natural");
            rule(rules, "rush_silk_touch");
            rule(rules, "rush_animals");
            rule(rules, "rush_animal_boundary");
            if (row.getBoolean("rushTrackingFull")) rule(rules, "rush_tracking_full");
        }
        else if (row.getString("activity").equals("FISHING")) EventDescriptions.fishingRules(rules);
        else if (row.getString("activity").equals("COOKING_SHOW")) rule(rules, "cooking");
        detail.put("rules", rules);
        EventRewardPreview.add(player, row, detail);
    }

    private static void rule(ListTag rules, String key, Object... values) {
        var rule = new CompoundTag();
        rule.putString("key", "jem.smp.rule." + key);
        var args = new ListTag();
        for (Object value : values) args.add(StringTag.valueOf(value.toString()));
        rule.put("args", args);
        rules.add(rule);
    }

    private static void add(ListTag list, String... actions) {
        for (String action : actions) list.add(StringTag.valueOf(action));
    }

    private SmpViews() {}
}
