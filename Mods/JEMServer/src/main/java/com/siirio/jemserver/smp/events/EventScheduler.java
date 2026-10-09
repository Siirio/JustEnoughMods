package com.siirio.jemserver.smp.events;

import com.siirio.jemserver.smp.*;

import net.minecraft.nbt.*;
import net.minecraft.network.chat.*;
import net.minecraft.server.MinecraftServer;

import java.time.Instant;
import java.util.*;

public final class EventScheduler {
    private static final String CHAT_BORDER="━━━━━━━━━━━━━━━━━━━━━━━━━━━━";
    private static final UUID SERVER = new UUID(0, 0);
    private static final Set<String> COMBAT_EVENTS = Set.of("BLOOD_MOON", "BOSS_RAID");
    private static final Set<String> REGION_FREE_EVENTS = Set.of("FISHING", "COOKING_SHOW");
    private static final Set<String> ATTEMPT_FIELDS = Set.of(
            "party", "bossEntity", "combatStarted", "attemptId", "wave", "waveTotal",
            "waveSpawned", "waveKilled", "rewardedWave", "continuedWave", "remaining", "nextWave", "batchSpawned", "batchTarget", "singleBatchWaveThree", "spawnFailures",
            "voting", "pendingWaveRewards", "wave3SetPiece", "wave5SetPiece", "wave3SetPieceSpawned", "wave5SetPieceSpawned", "setPieceMobs", "setPieceBosses", "setPieceDebuffProfileCursor",
            "setPieceAttack", "setPieceImpact", "setPieceAttackStart", "setPieceRecoveryUntil", "setPieceAttackPhase", "setPiecePhaseUntil", "setPieceCooldowns", "setPieceInitialHealth", "setPieceTerrain",
            "setPieceComboQueue", "setPieceComboIndex", "setPieceComboNext", "setPieceComboActive", "setPieceDebuffNextCast",
            "setPieceAttackBlocks", "setPieceAttackTerrain", "setPieceAttackBlocksUntil", "setPieceLastAttack", "setPiecePreviousAttack", "setPieceFinalUsed", "setPieceArenaWave", "setPieceFloorQueue", "setPieceFloorIndex", "bloodMoonCover", "nextBloodSpawn",
            "setPieceOriginX", "setPieceOriginZ", "setPieceSecondX", "setPieceSecondZ", "setPieceTargetX", "setPieceTargetZ",
            "waveStartedTick", "waveDamageStart", "observedPartyDps", "eliteTypes", "specialEquipped", "fullEquipped", "raidReinforcementStage", "raidAlliesSpawned", "raidAllies", "raidBossInitialHealth");
    private static final String PREPARING = "preparing";
    private static final String PREPARATION_QUEUED = "preparationQueued";
    private static long checkedWeek = Long.MIN_VALUE;
    private static final Map<MinecraftServer,ActiveEvents> ACTIVE_CACHE=new WeakHashMap<>();
    private record ActiveEvents(int tick,List<CompoundTag> rows) {}

    public static void reset() {
        checkedWeek = Long.MIN_VALUE;
        ACTIVE_CACHE.clear();
        CookingRecipes.reset();
    }

    public static void scheduleChanged() {
        checkedWeek = Long.MIN_VALUE;
    }

    public static void tick(MinecraftServer server) {
        if (!SmpEnvironment.active(server)) return;
        if (server.getTickCount() % 20 != 0) return;
        var data = SmpData.get(server);
        long now = System.currentTimeMillis();
        prepareNext(server, data, now);
        long week = WeeklySchedule.weekStart(Instant.ofEpochMilli(now));
        if (week != checkedWeek) {
            List<WeeklySchedule.Slot> slots;
            try {
                slots = WeeklySchedule.slots(Instant.ofEpochMilli(now), EventSchedule.definitions(server));
            } catch (RuntimeException exception) {
                slots = List.of();
                org.slf4j.LoggerFactory.getLogger(EventScheduler.class)
                        .error(
                                "Invalid weekly event schedule; existing event state is retained",
                                exception);
            }
            var scheduled = slots.stream().map(WeeklySchedule.Slot::id).collect(java.util.stream.Collectors.toSet());
            for (var row : data.all("events")) {
                if (row.getLong("planned") < week && row.getString("state").equals("PLANNED")) {
                    row.putString("state", "SKIPPED");
                    data.changed(row);
                }
                if (row.getString("state").equals("PLANNED")
                        && !row.getString("slot").startsWith("manual:")
                        && row.getLong("planned") >= week
                        && !scheduled.contains(row.getString("slot"))) {
                    row.putString("state", "SKIPPED");
                    data.changed(row);
                }
            }
            var known = new HashSet<String>();
            data.all("events").forEach(row -> known.add(row.getString("slot")));
            for (var slot : slots)
                if (!known.contains(slot.id())) {
                    var row = data.create("events", SERVER);
                    row.putString("slot", slot.id());
                    row.putString("activity", slot.type());
                    row.putString("title", slot.type());
                    row.putLong("planned", slot.planned());
                    row.putString("state", "PLANNED");
                    data.changed(row);
                }
            checkedWeek = week;
            data.prune("events");
        }
        for (var row : data.all("events")) {
            if (row.getString("state").equals("ACTIVE")) {
                if (now >= row.getLong("ends")) {
                    finish(
                            server,
                            row,
                            COMBAT_EVENTS.contains(row.getString("activity")) ? "FAILED" : "COMPLETED");
                    continue;
                }
                if (row.getBoolean("combatStarted")) new EncounterContext(row).expireDisconnected(server, now);
                if (row.getString("activity").equals("BLOOD_MOON")) BloodMoon.tick(server, row);
                continue;
            }
            if (!row.getString("state").equals("PLANNED") || row.getBoolean(PREPARING) || row.getLong("planned") > now) continue;
            String type = row.getString("activity");
            if (hasActive(data,type,row)) continue;
            long lastEnd =
                    data.all("events").stream()
                            .filter(
                                    other ->
                                            other != row
                                                    && other.getString("activity").equals(type)
                                                    && !other.getString("state").equals("PLANNED"))
                            .mapToLong(other -> other.getLong("ends"))
                            .max()
                            .orElse(0);
            if (now - lastEnd < SmpConfig.EVENT_GAP_HOURS.get() * 3600000L) continue;
            if (!REGION_FREE_EVENTS.contains(type)) {
                if (now < row.getLong("nextRegionAttempt")) continue;
                row.putLong(
                        "nextRegionAttempt",
                        now + SmpConfig.EVENT_REGION_RETRY_SECONDS.get() * 1000L);
                data.changed(row);
            }
            start(server, row, now);
        }
    }

    public static boolean start(MinecraftServer server, CompoundTag row, long now) {
        if (!SmpEnvironment.active(server)) return false;
        String type = row.getString("activity");
        var data = SmpData.get(server);
        if (!row.getString("state").equals("PLANNED") || row.getBoolean(PREPARING) || hasActiveOrPreparing(data,type,row)) return false;
        row.putBoolean(PREPARING, true);
        row.putLong(PREPARATION_QUEUED, now);
        data.changed(row);
        return true;
    }

    private static void prepareNext(MinecraftServer server, SmpData data, long now) {
        var row = data.all("events").stream()
                .filter(event -> event.getString("state").equals("PLANNED") && event.getBoolean(PREPARING))
                .min(Comparator.comparingLong(event -> event.getLong(PREPARATION_QUEUED)))
                .orElse(null);
        if (row == null) return;
        String type = row.getString("activity");
        boolean prepared = type.equals("BOSS_RAID") ? RaidEvent.prepare(server, row)
                : REGION_FREE_EVENTS.contains(type) || EventRegions.reserve(server, row);
        if (prepared && type.equals("COOKING_SHOW")) {
            try { CookingShow.prepare(server, row, now); }
            catch (IllegalArgumentException invalid) { prepared = false; }
        }
        row.remove(PREPARING);
        row.remove(PREPARATION_QUEUED);
        if (!prepared) {
            if (row.getString("slot").startsWith("manual:")) row.putString("state", "CANCELLED");
            else row.putLong("nextRegionAttempt", now + SmpConfig.EVENT_REGION_RETRY_SECONDS.get() * 1000L);
            data.changed(row);
            return;
        }
        row.putString("state", "ACTIVE");
        row.putLong("started", now);
        if (!type.equals("COOKING_SHOW")) row.putLong("ends", now + Math.min(type.equals("RESOURCE_RUSH") ? 2 : 3, SmpConfig.EVENT_HOURS.get()) * 3600000L);
        row.putBoolean("catchup", now - row.getLong("planned") > 60000);
        data.changed(row);
        if (type.equals("BLOOD_MOON") && row.getString("slot").startsWith("manual:") && !BloodMoon.startManual(server, row)) {
            finish(server, row, "CANCELLED");
            return;
        }
        com.siirio.jemserver.LandmarkNetwork.syncAll(server);
        announce(server,row,true,"");
    }

    private static void announce(MinecraftServer server, CompoundTag row, boolean started, String state) {
        var border=Component.literal(CHAT_BORDER).withStyle(net.minecraft.ChatFormatting.DARK_GRAY);
        var title=Component.literal("  JEM SMP  ").withStyle(net.minecraft.ChatFormatting.GOLD,net.minecraft.ChatFormatting.BOLD)
                .append(Component.translatable("jem.smp."+row.getString("activity")).withStyle(row.getString("activity").equals("BLOOD_MOON")?net.minecraft.ChatFormatting.RED:net.minecraft.ChatFormatting.GOLD));
        var status=started?Component.translatable("jem.smp.chat.event_started").withStyle(net.minecraft.ChatFormatting.WHITE)
                :Component.translatable("jem.smp.chat.event_finished",Component.translatable("jem.smp."+state)).withStyle(state.equals("COMPLETED")?net.minecraft.ChatFormatting.GREEN:net.minecraft.ChatFormatting.RED);
        var action=Component.translatable(started?"jem.smp.chat.event_open":"jem.smp.chat.event_results")
                .withStyle(style->style.withColor(net.minecraft.ChatFormatting.YELLOW).withUnderlined(true).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,"/smp events "+row.getUUID("id"))));
        for(Component line:List.of(border,title,status,action,border)) server.getPlayerList().broadcastSystemMessage(line,false);
    }

    public static CompoundTag active(MinecraftServer server, String type) {
        return active(server).stream().filter(e->e.getString("activity").equals(type)).findFirst().orElse(null);
    }

    public static List<CompoundTag> active(MinecraftServer server) {
        if (!SmpEnvironment.active(server)) return List.of();
        int tick=server.getTickCount();
        ActiveEvents cached=ACTIVE_CACHE.get(server);
        if(cached!=null&&cached.tick()==tick) return cached.rows();
        List<CompoundTag> rows=SmpData.get(server).all("events").stream().filter(e->e.getString("state").equals("ACTIVE")).toList();
        ACTIVE_CACHE.put(server,new ActiveEvents(tick,rows));
        return rows;
    }

    public static void finish(MinecraftServer server, CompoundTag row, String state) {
        announce(server,row,false,state);
        if(row.getBoolean("combatStarted")) new EncounterContext(row).complete(server,state.equals("COMPLETED"));
        row.putString("state", state);
        if(row.getString("activity").equals("BLOOD_MOON") && !state.equals("COMPLETED")) {
            cleanupFailedBloodMoon(server,row);
        }
        setPartyState(server,row,state);
        finishPartyTeleport(server,row);
        row.putLong("ends", System.currentTimeMillis());
        row.remove("placements");
        BloodMoon.cleanup(server, row);
        RaidEvent.cleanup(server, row);
        SmpData.get(server).changed(row);
        com.siirio.jemserver.LandmarkNetwork.syncAll(server);
    }

    public static void failAttempt(MinecraftServer server, CompoundTag row) {
        if (!row.getString("state").equals("ACTIVE") || !COMBAT_EVENTS.contains(row.getString("activity"))) return;
        announce(server,row,false,"FAILED");
        var encounter=new EncounterContext(row);
        encounter.complete(server,false);
        var data=SmpData.get(server);
        if(row.getString("activity").equals("BLOOD_MOON")) {
            cleanupFailedBloodMoon(server,row);
            BloodMoon.cleanup(server,row);
        }
        RaidEvent.resetAttempt(server,row);
        setPartyState(server,row,"FAILED");
        finishPartyTeleport(server,row);
        var members=SmpRecords.members(row);
        for(String id:members.getAllKeys()) {
            var member=members.getCompound(id);
            member.remove("accepted");
            member.remove("ready");
            member.remove("actualDeaths");
            member.remove("awaitingReturn");
            member.remove("contributed");
            member.remove("kills");
            member.remove("damage");
        }
        encounter.resetAttempt();
        ATTEMPT_FIELDS.forEach(row::remove);
        data.changed(row);
    }

    private static void setPartyState(MinecraftServer server, CompoundTag event, String state) {
        if (!event.hasUUID("party")) return;
        var data=SmpData.get(server);
        var party=data.find("parties",event.getUUID("party"));
        if(party!=null && !SmpData.closed(party)) {
            party.putString("state",state);
            data.changed(party);
        }
    }

    private static void finishPartyTeleport(MinecraftServer server,CompoundTag event) {
        if(!event.hasUUID("party")) return;
        CompoundTag party=SmpData.get(server).find("parties",event.getUUID("party"));
        if(party==null) return;
        if(event.getString("activity").equals("BOSS_RAID")) StructureStaging.finish(server,party);
        else PartyTeleportFlow.finish(server,party);
    }

    private static boolean hasActive(SmpData data, String activity, CompoundTag excluded) {
        return data.all("events").stream().anyMatch(event -> event != excluded
                && event.getString("state").equals("ACTIVE")
                && event.getString("activity").equals(activity));
    }

    private static boolean hasActiveOrPreparing(SmpData data, String activity, CompoundTag excluded) {
        return data.all("events").stream().anyMatch(event -> event != excluded
                && event.getString("activity").equals(activity)
                && (event.getString("state").equals("ACTIVE") || event.getBoolean(PREPARING)));
    }

    public static void cancelPreparation(MinecraftServer server, CompoundTag row) {
        row.remove(PREPARING);
        row.remove(PREPARATION_QUEUED);
        row.putString("state", "CANCELLED");
        row.putLong("ends", System.currentTimeMillis());
        if(row.getString("activity").equals("BOSS_RAID")) RaidEvent.cleanup(server,row);
        SmpData.get(server).changed(row);
    }

    private static void cleanupFailedBloodMoon(MinecraftServer server, CompoundTag event) {
        BloodMoonRewards.commitConsolation(server,event);
        ReviveSupport.finish(server,event);
        BloodMoonLoot.discard(server,event);
        CombatNetwork.failedBloodMoon(server,event);
    }

    private EventScheduler() {}
}
