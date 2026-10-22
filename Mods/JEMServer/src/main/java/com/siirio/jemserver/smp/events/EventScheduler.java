package com.siirio.jemserver.smp.events;

import com.siirio.jemserver.smp.*;

import net.minecraft.network.chat.*;
import net.minecraft.server.MinecraftServer;

import java.time.Instant;
import java.util.*;

public final class EventScheduler {
    private static final String CHAT_BORDER="━━━━━━━━━━━━━━━━━━━━━━━━━━━━";
    private static final UUID SERVER = new UUID(0, 0);
    private static final Set<String> COMBAT_EVENTS = Set.of("BLOOD_MOON", "BOSS_RAID");
    private static final Set<String> REGION_FREE_EVENTS = Set.of("FISHING", "COOKING_SHOW");
    private static final String PREPARING = "preparing";
    private static final String PREPARATION_QUEUED = "preparationQueued";
    private static long checkedWeek = Long.MIN_VALUE;
    private static final Map<MinecraftServer,ActiveEventSnapshot> ACTIVE_CACHE=new WeakHashMap<>();

    public static void reset() {
        checkedWeek = Long.MIN_VALUE;
        ACTIVE_CACHE.clear();
        CookingRecipes.reset();
    }

    public static void scheduleChanged() {
        checkedWeek = Long.MIN_VALUE;
    }

    public static void tick(MinecraftServer server) {
        var serverForMutation = server;
        if (serverForMutation == null || serverForMutation.overworld() == null) { tickInScope(server); return; }
        var store = com.siirio.jemserver.smp.SmpData.get(serverForMutation);
        store.edit(() -> tickInScope(server));
    }

    private static void tickInScope(MinecraftServer server) {
        if (!SmpEnvironment.active(server)) return;
        if (server.getTickCount() % 20 != 0) return;
        var data = SmpData.get(server);
        long now = System.currentTimeMillis();
        if (!EventRuntime.enabled(server)) {
            for (var row : List.copyOf(data.events().currentRows())) {
                if (!EventRuntime.ordinaryRaid(row) || !row.state().equals("ACTIVE")) continue;
                if (now >= row.ends()) finish(server, row, "FAILED");
                else if (row.combatStarted()) new EncounterContext(row).expireDisconnected(server, now);
            }
            return;
        }
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
            for (var row : data.events().currentRows()) {
                if (row.planned() < week && row.state().equals("PLANNED")) {
                    row.state( "SKIPPED");

                }
                if (row.state().equals("PLANNED")
                        && !row.slot().startsWith("manual:")
                        && row.planned() >= week
                        && !scheduled.contains(row.slot())) {
                    row.state( "SKIPPED");

                }
            }
            var known = new HashSet<String>();
            data.events().currentRows().forEach(row -> known.add(row.slot()));
            for (var slot : slots)
                if (!known.contains(slot.id())) {
                    var row = data.events().create(SERVER);
                    row.slot( slot.id());
                    row.activity( slot.type());
                    row.title( slot.type());
                    row.planned( slot.planned());
                    row.state( "PLANNED");

                }
            checkedWeek = week;
            data.events().prune();
        }
        for (var row : data.events().currentRows()) {
            if (row.state().equals("ACTIVE")) {
                if (now >= row.ends()) {
                    finish(
                            server,
                            row,
                            COMBAT_EVENTS.contains(row.activity()) ? "FAILED" : "COMPLETED");
                    continue;
                }
                if (row.combatStarted()) new EncounterContext(row).expireDisconnected(server, now);
                if (row.activity().equals("BLOOD_MOON")) BloodMoon.tick(server, row);
                continue;
            }
            if (!row.state().equals("PLANNED") || row.preparing() || row.planned() > now) continue;
            String type = row.activity();
            if (hasActive(data,type,row)) continue;
            long lastEnd =
                    data.events().currentRows().stream()
                            .filter(
                                    other ->
                                            other != row
                                                    && other.activity().equals(type)
                                                    && !other.state().equals("PLANNED"))
                            .mapToLong(other -> other.ends())
                            .max()
                            .orElse(0);
            if (now - lastEnd < SmpConfig.EVENT_GAP_HOURS.get() * 3600000L) continue;
            if (!REGION_FREE_EVENTS.contains(type)) {
                if (now < row.nextRegionAttempt()) continue;
                row.nextRegionAttempt(
                        now + SmpConfig.EVENT_REGION_RETRY_SECONDS.get() * 1000L);

            }
            start(server, row, now);
        }
    }

    public static boolean start(MinecraftServer server, EventState row, long now) {
        var serverForMutation = server;
        if (serverForMutation == null || serverForMutation.overworld() == null) { return startInScope(server, row, now); }
        var store = com.siirio.jemserver.smp.SmpData.get(serverForMutation);
        return store.edit(() -> startInScope(server, store.events().bind(row), now));
    }

    private static boolean startInScope(MinecraftServer server, EventState row, long now) {
        if (!EventRuntime.enabled(server)) return false;
        String type = row.activity();
        var data = SmpData.get(server);
        if (!row.state().equals("PLANNED") || row.preparing() || hasActiveOrPreparing(data,type,row)) return false;
        row.preparing( true);
        row.preparationQueued( now);

        return true;
    }

    private static void prepareNext(MinecraftServer server, SmpData data, long now) {
        var serverForMutation = server;
        if (serverForMutation == null || serverForMutation.overworld() == null) { prepareNextInScope(server, data, now); return; }
        var store = com.siirio.jemserver.smp.SmpData.get(serverForMutation);
        store.edit(() -> prepareNextInScope(server, data, now));
    }

    private static void prepareNextInScope(MinecraftServer server, SmpData data, long now) {
        var row = data.events().currentRows().stream()
                .filter(event -> event.state().equals("PLANNED") && event.preparing())
                .min(Comparator.comparingLong(event -> event.preparationQueued()))
                .orElse(null);
        if (row == null) return;
        String type = row.activity();
        boolean prepared = type.equals("BOSS_RAID") ? RaidEvent.prepare(server, row)
                : REGION_FREE_EVENTS.contains(type) || EventRegions.reserve(server, row);
        if (prepared && type.equals("COOKING_SHOW")) {
            try { CookingShow.prepare(server, row, now); }
            catch (IllegalArgumentException invalid) { prepared = false; }
        }
        row.clearPreparing();
        row.clearPreparationQueued();
        if (!prepared) {
            if (row.slot().startsWith("manual:")) row.state( "CANCELLED");
            else row.nextRegionAttempt( now + SmpConfig.EVENT_REGION_RETRY_SECONDS.get() * 1000L);

            return;
        }
        row.state( "ACTIVE");
        ACTIVE_CACHE.remove(server);
        row.started( now);
        if (!type.equals("COOKING_SHOW")) row.ends( now + Math.min(type.equals("RESOURCE_RUSH") ? 2 : 3, SmpConfig.EVENT_HOURS.get()) * 3600000L);
        row.catchup( now - row.planned() > 60000);

        if (type.equals("BLOOD_MOON") && row.slot().startsWith("manual:") && !BloodMoon.startManual(server, row)) {
            finish(server, row, "CANCELLED");
            return;
        }
        announce(server,row,true,"");
    }

    private static void announce(MinecraftServer server, EventState row, boolean started, String state) {
        if (EventRuntime.ordinaryRaid(row)) return;
        var border=Component.literal(CHAT_BORDER).withStyle(net.minecraft.ChatFormatting.DARK_GRAY);
        var title=Component.literal("  JEM SMP  ").withStyle(net.minecraft.ChatFormatting.GOLD,net.minecraft.ChatFormatting.BOLD)
                .append(Component.translatable("jem.smp."+row.activity()).withStyle(row.activity().equals("BLOOD_MOON")?net.minecraft.ChatFormatting.RED:net.minecraft.ChatFormatting.GOLD));
        var status=started?Component.translatable("jem.smp.chat.event_started").withStyle(net.minecraft.ChatFormatting.WHITE)
                :Component.translatable("jem.smp.chat.event_finished",Component.translatable("jem.smp."+state)).withStyle(state.equals("COMPLETED")?net.minecraft.ChatFormatting.GREEN:net.minecraft.ChatFormatting.RED);
        var action=Component.translatable(started?"jem.smp.chat.event_open":"jem.smp.chat.event_results")
                .withStyle(style->style.withColor(net.minecraft.ChatFormatting.YELLOW).withUnderlined(true).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,"/smp events "+row.id())));
        for(Component line:List.of(border,title,status,action,border)) server.getPlayerList().broadcastSystemMessage(line,false);
    }

    public static EventState active(MinecraftServer server, String type) {
        return active(server).stream().filter(e->e.activity().equals(type)).findFirst().orElse(null);
    }

    public static List<EventState> active(MinecraftServer server) {
        if (!SmpEnvironment.active(server)) return List.of();
        var data = SmpData.get(server);
        var records = data.events();
        int tick = server.getTickCount();
        var cached = ACTIVE_CACHE.get(server);
        if (cached != null && cached.tick() == tick) {
            return cached.ids().stream().map(id -> data.inCommand() ? records.current(id) : records.snapshot(id))
                    .filter(Objects::nonNull).toList();
        }
        var candidates = data.inCommand() ? records.currentRows() : records.snapshots();
        var rows = candidates.stream().filter(event -> EventRuntime.available(server, event)
                && event.state().equals("ACTIVE")).toList();
        ACTIVE_CACHE.put(server, new ActiveEventSnapshot(tick, rows.stream().map(EventState::id).toList()));
        return rows;
    }

    public static void finish(MinecraftServer server, EventState row, String state) {
        var serverForMutation = server;
        if (serverForMutation == null || serverForMutation.overworld() == null) { finishInScope(server, row, state); return; }
        var store = com.siirio.jemserver.smp.SmpData.get(serverForMutation);
        store.edit(() -> finishInScope(server, store.events().bind(row), state));
    }

    private static void finishInScope(MinecraftServer server, EventState row, String state) {
        announce(server,row,false,state);
        if(row.combatStarted()) new EncounterContext(row).complete(server,state.equals("COMPLETED"));
        row.state( state);
        ACTIVE_CACHE.remove(server);
        if(row.activity().equals("BLOOD_MOON")) EventAreaHooks.releaseBloodMoon(server,row.id(),false);
        if(row.activity().equals("BLOOD_MOON") && !state.equals("COMPLETED")) {
            cleanupFailedBloodMoon(server,row);
        }
        setPartyState(server,row,state);
        finishPartyTeleport(server,row);
        row.ends( System.currentTimeMillis());
        row.clearPlacements();
        BloodMoon.cleanup(server, row);
        RaidEvent.cleanup(server, row);

    }

    public static void failAttempt(MinecraftServer server, EventState row) {
        var serverForMutation = server;
        if (serverForMutation == null || serverForMutation.overworld() == null) { failAttemptInScope(server, row); return; }
        var store = com.siirio.jemserver.smp.SmpData.get(serverForMutation);
        store.edit(() -> failAttemptInScope(server, store.events().bind(row)));
    }

    private static void failAttemptInScope(MinecraftServer server, EventState row) {
        if (!row.state().equals("ACTIVE") || !COMBAT_EVENTS.contains(row.activity())) return;
        announce(server,row,false,"FAILED");
        var encounter=new EncounterContext(row);
        encounter.complete(server,false);
        var data=SmpData.get(server);
        if(row.activity().equals("BLOOD_MOON")) {
            cleanupFailedBloodMoon(server,row);
            BloodMoon.cleanup(server,row);
        }
        RaidEvent.resetAttempt(server,row);
        setPartyState(server,row,"FAILED");
        finishPartyTeleport(server,row);
        for (var member : row.members().values()) {
            member.clearAccepted();
            member.clearReady();
            member.clearActualDeaths();
            member.clearAwaitingReturn();
            member.clearContributed();
            member.clearKills();
            member.clearDamage();
        }
        encounter.resetAttempt();
        resetAttemptFields(row);
        if(row.activity().equals("BLOOD_MOON")) EventAreaHooks.releaseBloodMoon(server,row.id(),true);

    }

    public static boolean cancelAttempt(MinecraftServer server, EventState row, UUID actionToken) {
        if (!row.state().equals("ACTIVE") || !COMBAT_EVENTS.contains(row.activity())) return false;
        UUID current = row.hasAttemptId() ? row.attemptId() : row.id();
        if (!current.equals(actionToken)) return false;
        failAttempt(server, row);
        return true;
    }

    private static void setPartyState(MinecraftServer server, EventState event, String state) {
        var serverForMutation = server;
        if (serverForMutation == null || serverForMutation.overworld() == null) { setPartyStateInScope(server, event, state); return; }
        var store = com.siirio.jemserver.smp.SmpData.get(serverForMutation);
        store.edit(() -> setPartyStateInScope(server, store.events().bind(event), state));
    }

    private static void setPartyStateInScope(MinecraftServer server, EventState event, String state) {
        if (!event.hasParty()) return;
        var data=SmpData.get(server);
        var party=data.parties().current(event.party());
        if(party!=null && !SmpData.closed(party)) {
            party.state(state);
            HostedBoundary.clearParty(party);

        }
    }

    private static void finishPartyTeleport(MinecraftServer server,EventState event) {
        var serverForMutation = server;
        if (serverForMutation == null || serverForMutation.overworld() == null) { finishPartyTeleportInScope(server, event); return; }
        var store = com.siirio.jemserver.smp.SmpData.get(serverForMutation);
        store.edit(() -> finishPartyTeleportInScope(server, store.events().bind(event)));
    }

    private static void finishPartyTeleportInScope(MinecraftServer server,EventState event) {
        if(!event.hasParty()) return;
        PartyState party=SmpData.get(server).parties().current(event.party());
        if(party==null) return;
        if(event.activity().equals("BOSS_RAID")) StructureStaging.finish(server,party);
        else PartyTeleportFlow.finish(server,party);
    }

    private static boolean hasActive(SmpData data, String activity, EventState excluded) {
        var store = data;
        return store.edit(() -> hasActiveInScope(data, activity, store.events().bind(excluded)));
    }

    private static boolean hasActiveInScope(SmpData data, String activity, EventState excluded) {
        return data.events().currentRows().stream().anyMatch(event -> event != excluded
                && event.state().equals("ACTIVE")
                && event.activity().equals(activity));
    }

    private static boolean hasActiveOrPreparing(SmpData data, String activity, EventState excluded) {
        var store = data;
        return store.edit(() -> hasActiveOrPreparingInScope(data, activity, store.events().bind(excluded)));
    }

    private static boolean hasActiveOrPreparingInScope(SmpData data, String activity, EventState excluded) {
        return data.events().currentRows().stream().anyMatch(event -> event != excluded
                && event.activity().equals(activity)
                && (event.state().equals("ACTIVE") || event.preparing()));
    }

    public static void cancelPreparation(MinecraftServer server, EventState row) {
        var serverForMutation = server;
        if (serverForMutation == null || serverForMutation.overworld() == null) { cancelPreparationInScope(server, row); return; }
        var store = com.siirio.jemserver.smp.SmpData.get(serverForMutation);
        store.edit(() -> cancelPreparationInScope(server, store.events().bind(row)));
    }

    private static void cancelPreparationInScope(MinecraftServer server, EventState row) {
        row.clearPreparing();
        row.clearPreparationQueued();
        row.state( "CANCELLED");
        row.ends( System.currentTimeMillis());
        if(row.activity().equals("BOSS_RAID")) RaidEvent.cleanup(server,row);

    }

    private static void cleanupFailedBloodMoon(MinecraftServer server, EventState event) {
        BloodMoonRewards.commitConsolation(server,event);
        ReviveSupport.finish(server,event);
        BloodMoonLoot.discard(server,event);
        CombatNetwork.failedBloodMoon(server,event);
    }

    private static void resetAttemptFields(EventState row) {
        var setPiece = row.setPiece().copy();
        row.clearParty();
        row.clearBossEntity();
        row.clearCombatStarted();
        row.clearAttemptId();
        row.clearWave();
        row.clearWaveTotal();
        row.clearWaveSpawned();
        row.clearWaveKilled();
        row.clearRewardedWave();
        row.clearContinuedWave();
        row.clearRemaining();
        row.clearNextWave();
        row.clearBatchSpawned();
        row.clearBatchTarget();
        row.clearSingleBatchWaveThree();
        row.clearSpawnFailures();
        row.clearVoting();
        row.clearPendingWaveRewards();
        row.clearWave3SetPieceSpawned();
        row.clearWave5SetPieceSpawned();
        setPiece.clearMobs();
        setPiece.clearBosses();
        setPiece.clearDebuffProfileCursor();
        setPiece.clearAttack();
        setPiece.clearImpact();
        setPiece.clearAttackStart();
        setPiece.clearRecoveryUntil();
        setPiece.clearAttackPhase();
        setPiece.clearPhaseUntil();
        setPiece.clearCooldowns();
        setPiece.clearInitialHealth();
        setPiece.clearComboQueue();
        setPiece.clearComboIndex();
        setPiece.clearComboNext();
        setPiece.clearComboActive();
        setPiece.clearDebuffNextCast();
        setPiece.clearLastAttack();
        setPiece.clearPreviousAttack();
        setPiece.clearFinalUsed();
        setPiece.clearFloorQueue();
        setPiece.clearFloorIndex();
        row.clearBloodMoonCover();
        row.clearNextBloodSpawn();
        setPiece.clearOriginX();
        setPiece.clearOriginZ();
        setPiece.clearSecondX();
        setPiece.clearSecondZ();
        setPiece.clearTargetX();
        setPiece.clearTargetZ();
        row.clearWaveStartedTick();
        row.clearWaveDamageStart();
        row.clearObservedPartyDps();
        row.clearEliteTypes();
        row.clearSpecialEquipped();
        row.clearFullEquipped();
        row.clearRaidReinforcementStage();
        row.clearRaidAlliesSpawned();
        row.clearRaidAllies();
        row.clearRaidBossInitialHealth();
        row.setPiece(setPiece);
    }

    private EventScheduler() {}
}
