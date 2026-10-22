package com.siirio.jemserver.smp;

import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;

public final class SmpViews {
    public static CompoundTag build(ServerPlayer player, SmpQuery query) {
        SmpRecords.require(SmpEnvironment.active(player) && SmpTable.viewable(query.tab()), "unavailable");
        CompoundTag result = buildView(player, query);
        result.put("features", DomainFeatures.snapshot(player.server));
        return result;
    }

    private static CompoundTag buildView(ServerPlayer player, SmpQuery query) {
        var data = SmpData.get(player.server);
        var result = new CompoundTag();
        result.putString("tab", query.tab());
        result.putString("filter", query.filter());
        result.putString("querySearch", query.search());
        result.putInt("page", query.page());
        result.putUUID("viewer", player.getUUID());
        if (query.tab().equals("settlements")) return com.siirio.jemserver.smp.settlements.SettlementProjection.view(player, query.selected());
        if (query.tab().equals("recruits")) return com.siirio.jemserver.smp.recruits.RecruitProjection.view(player);
        if (query.tab().equals("shops")) return com.siirio.jemserver.smp.shops.ShopIndex.view(player, query);
        if (query.selected() == null) {
        Collection<CompoundTag> candidates = data.views(SmpTable.fromKey(query.tab()));
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
                            rows.add(summary);
                        });
        result.put("rows", rows);
        }
        if (query.selected() != null) {
            CompoundTag row = data.view(SmpTable.fromKey(query.tab()), query.selected());
            if (row != null && authorized(player, query.tab(), row)) {
                var detail =
                        query.tab().equals("parties")
                                        ? Parties.view(player, row)
                                        : detail(row);
                if (query.tab().equals("parties")) {
                    organizer(player, row, detail);
                    CompoundTag event = row.hasUUID("eventId") ? data.events().snapshot(row.getUUID("eventId")) : null;
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
        String name = Profiles.snapshot(player.server, row.getUUID("owner")).getString("name");
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
        if (table.equals("parties")) result.addAll(PartyAdmission.actions(player, row));
        if (table.equals("events")) result.addAll(EventAdmission.actions(player, row));
        var location = row.hasUUID("eventId") ? SmpData.get(player.server).events().snapshot(row.getUUID("eventId")) : row;
        if(SmpActions.canTeleportEvent(table, row)&&meaningfulLocation(row)&&Set.of("BLOOD_MOON","BOSS_RAID","RESOURCE_RUSH").contains(row.getString("activity"))) add(result,"teleport_event");
        if (SmpActions.canNavigate(player, table, row, location) && meaningfulLocation(location)) add(result, "navigate");
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
        detail.putString("lifecycle", EventAdmission.lifecycle(row));
    }

    private static void eventPresentation(ServerPlayer player, CompoundTag row, CompoundTag detail) {
        eventSummary(row, detail);
        boolean combat = EventAdmission.combat(row);
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

    private static final class RewardsAdapter {
        static int count(ServerPlayer player) { return DomainFeatures.loaded("jem_world_boss_tiers") ? NativeRewards.count(player) : 0; }
        static int totalItems(ServerPlayer player) { return DomainFeatures.loaded("jem_world_boss_tiers") ? NativeRewards.totalItems(player) : 0; }
        static ListTag bundles(ServerPlayer player, int start, int size) { return DomainFeatures.loaded("jem_world_boss_tiers") ? NativeRewards.bundles(player, start, size) : new ListTag(); }
    }
    private static final class NativeRewards {
        static int count(ServerPlayer player) { return com.siirio.jemworldbosstiers.encounter.PendingRewardContainer.count(player); }
        static int totalItems(ServerPlayer player) { return com.siirio.jemworldbosstiers.encounter.PendingRewardContainer.totalItems(player); }
        static ListTag bundles(ServerPlayer player, int start, int size) { return com.siirio.jemworldbosstiers.encounter.PendingRewardContainer.bundles(player, start, size); }
    }

    private SmpViews() {}
}
