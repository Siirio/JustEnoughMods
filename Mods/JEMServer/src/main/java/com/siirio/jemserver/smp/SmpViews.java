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
        Collection<? extends SmpRecord> candidates = switch (SmpTable.fromKey(query.tab())) {
            case PARTIES -> data.parties().snapshots();
            case EVENTS -> data.events().snapshots();
            default -> List.of();
        };
        String search = query.search().toLowerCase(Locale.ROOT);
        var matches =
                candidates.stream().map(source -> new Projection(source, encode(source)))
                        .filter(row -> visible(player, query, row.source(), row.wire()))
                        .filter(row -> searchable(row.wire()).contains(search))
                        .sorted(
                                query.tab().equals("events")
                                        ? Comparator.comparingInt((Projection row) -> row.source().state().equals("ACTIVE") ? 0 : 1)
                                                .thenComparingLong(row -> row.wire().getLong("planned"))
                                        : Comparator.comparingLong((Projection row) -> row.source().created()).reversed())
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
                            var summary = summary(row.wire());
                            if (query.tab().equals("parties")) organizer(player, row.source(), summary);
                            if (query.tab().equals("events")) eventSummary(row.source(), summary);
                            rows.add(summary);
                        });
        result.put("rows", rows);
        }
        if (query.selected() != null) {
            SmpRecord source = data.view(SmpTable.fromKey(query.tab()), query.selected());
            CompoundTag row = source == null ? null : encode(source);
            if (row != null && authorized(player, source)) {
                var detail =
                        query.tab().equals("parties")
                                        ? partyView(player, (PartyState) source)
                                        : detail(row);
                if (query.tab().equals("parties")) {
                    organizer(player, source, detail);
                    EventState event = row.hasUUID("eventId") ? data.events().snapshot(row.getUUID("eventId")) : null;
                    eventPresentation(player, event == null ? source : event, detail);
                    if (event != null) detail.putString("eventActivity", event.activity());
                    if (event != null && event.hasDimension()) {
                        detail.putString("dimension", event.dimension());
                        detail.putLong("position", event.position());
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
                        !query.tab().equals("parties") || Parties.accepted((PartyState) source, player.getUUID());
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
                    eventPresentation(player, source, detail);
                    com.siirio.jemserver.smp.events.CookingShow.view(player, (EventState) source, detail);
                }
                detail.put("actions", actions(player, source));
                result.put("detail", detail);
            }
        }
        return result;
    }

    private static void organizer(ServerPlayer player, SmpRecord row, CompoundTag result) {
        if (row.owner() == null) return;
        String name = Profiles.snapshot(player.server, row.owner()).name();
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

    private static boolean authorized(ServerPlayer player, SmpRecord source) {
        if (source instanceof PartyState party && (party.state().equals("DRAFT") || party.solo()))
            return player.getUUID().equals(party.owner()) || party.members().containsKey(player.getUUID());
        return true;
    }

    private static boolean visible(ServerPlayer player, SmpQuery query, SmpRecord source, CompoundTag wire) {
        boolean owner = player.getUUID().equals(source.owner());
        boolean member = source instanceof PartyState party ? party.members().containsKey(player.getUUID())
                : source instanceof EventState event && event.members().containsKey(player.getUUID());
        if (!authorized(player, source)) return false;
        if (query.filter().startsWith("owner:"))
            return source.owner() != null && source.owner().toString().equals(query.filter().substring(6));
        return switch (query.filter()) {
            case "mine" -> owner;
            case "accepted" -> member;
            case "history", "past" -> source.closed();
            case "active" -> source.state().equals("ACTIVE");
            case "upcoming" -> source.state().equals("PLANNED");
            default -> !source.closed();
        };
    }

    private static ListTag actions(ServerPlayer player, SmpRecord source) {
        var result = new ListTag();
        if (source instanceof PartyState party) {
            PartyAdmission.actions(player, party).forEach(action -> result.add(StringTag.valueOf(action)));
            var event = party.hasEventId() ? SmpData.get(player.server).events().snapshot(party.eventId()) : null;
            if (SmpActions.canNavigate(player, party, event) && meaningfulLocation(event == null ? party : event)) add(result, "navigate");
        } else if (source instanceof EventState event) {
            EventAdmission.actions(player, event).forEach(action -> result.add(StringTag.valueOf(action)));
            if (SmpActions.canTeleportEvent(event) && meaningfulLocation(event)
                    && Set.of("BLOOD_MOON", "BOSS_RAID", "RESOURCE_RUSH").contains(event.activity())) add(result, "teleport_event");
            if (SmpActions.canNavigate(event) && meaningfulLocation(event)) add(result, "navigate");
        }
        return result;
    }

    private static String activity(SmpRecord source) {
        return source instanceof PartyState party ? party.activity() : ((EventState) source).activity();
    }

    private static boolean meaningfulLocation(SmpRecord source) {
        if (source instanceof PartyState party) return party.hasDimension() && party.hasPosition()
                && !Set.of("FISHING", "COOKING_SHOW").contains(party.activity());
        if (source instanceof EventState event) return event.hasDimension() && event.hasPosition()
                && !Set.of("FISHING", "COOKING_SHOW").contains(event.activity());
        return false;
    }

    private static void eventSummary(SmpRecord source, CompoundTag detail) {
        if (source instanceof PartyState party) {
            EventDescriptions.add(party, detail);
            if (party.hasBossType()) detail.putString("bossType", party.bossType());
            detail.putString("lifecycle", party.state());
        } else if (source instanceof EventState event) {
            EventDescriptions.add(event, detail);
            if (event.hasBossType()) detail.putString("bossType", event.bossType());
            detail.putString("lifecycle", EventAdmission.lifecycle(event));
        }
        detail.putBoolean("meaningfulLocation", meaningfulLocation(source));
    }

    private static void eventPresentation(ServerPlayer player, SmpRecord source, CompoundTag detail) {
        eventSummary(source, detail);
        String activity = activity(source);
        boolean combat = source instanceof PartyState ? activity.equals("BOSS") : EventAdmission.combat((EventState) source);
        var rules = new ListTag();
        if (combat) {
            if (activity.equals("BLOOD_MOON")) {
                rule(rules, "waves", com.siirio.jemserver.smp.events.BloodMoonWaves.WAVES);
                rule(rules, "safe_deaths", com.siirio.jemserver.smp.events.EventRules.SAFE_DEATHS.get());
            } else rule(rules, "start");
            rule(rules, "no_late_join");
            rule(rules, "no_friendly_fire");
            rule(rules, "eligible_rewards");
        } else if (activity.equals("RESOURCE_RUSH")) {
            rule(rules, "resource_rush");
            rule(rules, "rush_natural");
            rule(rules, "rush_silk_touch");
            rule(rules, "rush_animals");
            rule(rules, "rush_animal_boundary");
            if (((EventState) source).rushTrackingFull()) rule(rules, "rush_tracking_full");
        } else if (activity.equals("FISHING")) EventDescriptions.fishingRules(rules);
        else if (activity.equals("COOKING_SHOW")) rule(rules, "cooking");
        detail.put("rules", rules);
        if (source instanceof PartyState party) EventRewardPreview.add(player, party, detail);
        else EventRewardPreview.add(player, (EventState) source, detail);
    }

    private record Projection(SmpRecord source, CompoundTag wire) {}
    private static CompoundTag encode(SmpRecord source) {
        if (source instanceof PartyState party) return PartyStateNbtCodec.encode(party);
        if (source instanceof EventState event) return EventNbtCodec.encode(event);
        throw new IllegalArgumentException("Not an SMP view route");
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

    private static CompoundTag partyView(ServerPlayer player, PartyState row) {
        var copy = PartyStateNbtCodec.encode(row);
        copy.remove("invitations");
        int present = 0, ready = 0;
        var members = copy.getCompound("members");
        for (String key : members.getAllKeys()) {
            var member = members.getCompound(key);
            var online = player.server.getPlayerList().getPlayer(UUID.fromString(key));
            boolean simulated=member.getBoolean("simulated");
            member.putBoolean("online", online != null || simulated);
            boolean here =
                    member.getBoolean("accepted")
                            && (simulated || online != null && SmpRecords.present(row, online));
            member.putBoolean("present", here);
            UUID memberId=UUID.fromString(key);
            boolean leader=row.owner().equals(player.getUUID());
            member.putBoolean("canApprove",leader&&!row.state().equals("ACTIVE")&&!member.getBoolean("accepted"));
            member.putBoolean("canRemove",leader&&!row.state().equals("ACTIVE")&&!memberId.equals(player.getUUID()));
            member.putBoolean("canTransfer",leader&&!row.state().equals("ACTIVE")&&member.getBoolean("accepted")&&!memberId.equals(player.getUUID()));
            if (here) present++;
            if (online == null && !simulated) member.putBoolean("ready", false);
            if (member.getBoolean("ready") && member.getBoolean("accepted")) ready++;
        }
        copy.putInt("worldTier", com.siirio.jemworldbosstiers.api.WorldTierApi.currentTier(player.server));
        if (row.hasBossEntity()) {
            var level =
                    player.server.getLevel(
                            net.minecraft.resources.ResourceKey.create(
                                    net.minecraft.core.registries.Registries.DIMENSION,
                                    new net.minecraft.resources.ResourceLocation(
                                            row.dimension())));
            if (level != null
                    && level.getEntity(row.bossEntity()) instanceof net.minecraft.world.entity.LivingEntity boss)
                for (var status :
                        com.siirio.jemworldbosstiers.api.HostedEncounterApi.status(boss)) {
                    var member = members.getCompound(status.playerId().toString());
                    member.putString("combatState", status.state());
                    member.putBoolean("reviveAvailable", status.reviveAvailable());
                    member.putLong("remainingTicks", status.remainingTicks());
                }
        }
        if (row.activity().equals("BOSS") && !row.state().equals("ACTIVE")) {
            var scaling = com.siirio.jemworldbosstiers.encounter.HostedScaling.calculate(Math.max(1, present), false, 1, 1);
            copy.putInt("partyBonusPercent", (int) Math.round((scaling.health() - 1) * 100));
        }
        copy.putInt("present", present);
        copy.putInt("readyCount", ready);
        copy.putBoolean("leader", row.owner().equals(player.getUUID()));
        String reason = HostedParties.startReason(player, row);
        copy.putBoolean("canStart", reason.isEmpty());
        copy.putString("startReason", reason);
        copy.putBoolean("closeLobby", row.closed());
        if (row.closed()) copy.putString("lobbyMessage", "party_dissolved");
        var candidates = new net.minecraft.nbt.ListTag();
        if (copy.getBoolean("leader") && !row.solo() && !row.state().equals("ACTIVE") && !row.closed()) {
            for (var target : player.server.getPlayerList().getPlayers()) {
                if (SmpRecords.member(row, target.getUUID()) || target.isSpectator()
                        || SmpData.get(player.server).parties().snapshots().stream().anyMatch(other -> !other.closed() && Parties.accepted(other, target.getUUID()))) continue;
                var candidate = new CompoundTag();
                candidate.putUUID("id", target.getUUID());
                candidate.putString("name", target.getGameProfile().getName());
                candidate.putBoolean("pending", row.invitations().containsKey(target.getUUID()));
                candidates.add(candidate);
            }
        }
        copy.put("inviteCandidates", candidates);
        return copy;
    }

    public static CompoundTag profile(ServerPlayer viewer, UUID id) {
        var state = SmpData.get(viewer.server).profiles().snapshot(id);
        var source = state == null ? null : ProfileStateNbtCodec.encode(state);
        var row = new CompoundTag();
        if (source == null) return row;
        for (String key :
                java.util.List.of(
                        "id",
                        "owner",
                        "revision",
                        "name",
                        "firstJoin",
                        "playTicks",
                        "partiesHosted",
                        "partiesJoined",
                        "bossesHosted",
                        "bossAssists",
                        "raidsCompleted",
                        "bloodMoonClears",
                        "fishingEvents",
                        "creaturesTamed",
                        "landmarksCreated",
                        "biomesExplored",
                        "animalsTamed",
                        "tameSpecies",
                        "fishingMilestones",
                        "rewardAvailability")) {
            if (source.contains(key)) row.put(key, source.get(key).copy());
        }
        row.putInt("discoveredBiomes", source.getCompound("discoveredBiomes").size());
        var statistics = new net.minecraft.nbt.ListTag();
        var eventStats = source.getCompound("eventStats");
        for (String type : eventStats.getAllKeys()) {
            var statistic = new CompoundTag();
            statistic.putString("type", type);
            statistic.putLong("count", eventStats.getLong(type));
            statistics.add(statistic);
        }
        statistics.sort(java.util.Comparator.comparing(value -> ((CompoundTag) value).getString("type")));
        row.put("eventStats", statistics);
        var player = viewer.server.getPlayerList().getPlayer(id);
        row.putBoolean("online", player != null);
        if (player != null)
            row.putInt(
                    "playTicks",
                    player.getStats()
                            .getValue(
                                    net.minecraft.stats.Stats.CUSTOM.get(
                                            net.minecraft.stats.Stats.PLAY_TIME)));
        if (player != null)
            row.putInt(
                    "mobsKilled",
                    player.getStats().getValue(net.minecraft.stats.Stats.CUSTOM.get(net.minecraft.stats.Stats.MOB_KILLS)));
        else if (source.contains("mobsKilled")) row.putInt("mobsKilled", source.getInt("mobsKilled"));
        if (DomainFeatures.loaded("jem_roles")) RacesAdapter.project(viewer, id, row);
        var professions = new net.minecraft.nbt.ListTag();
        if (net.minecraftforge.fml.ModList.get().isLoaded("jem_professions")) {
            professions = source.getList("professions", net.minecraft.nbt.Tag.TAG_COMPOUND).copy();
            if (player != null) {
                professions = new ListTag();
                for (var value : com.siirio.jemserver.compat.professions.ProfessionHostAdapter.snapshot(player)) {
                    var profession = new CompoundTag();
                    profession.putString("id", value.id());
                    profession.putLong("xp", value.xp());
                    profession.putInt("level", value.level());
                    professions.add(profession);
                }
            }
        }
        row.put("professions", professions);
        if (DomainFeatures.loaded("jem_settlements")) com.siirio.jemserver.compat.settlements.SettlementHostAdapter.profile(viewer, id, row);
        return row;
    }

    private static final class RacesAdapter {
        static void project(ServerPlayer viewer, UUID id, CompoundTag row) {
            com.siirio.jemroles.api.RaceApi.get(viewer.server, id).ifPresent(race -> row.putString("race", race.race().name()));
        }
    }


    private SmpViews() {}
}
