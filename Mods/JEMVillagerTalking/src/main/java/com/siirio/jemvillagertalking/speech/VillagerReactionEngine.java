package com.siirio.jemvillagertalking.speech;

import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.phys.AABB;
import org.slf4j.Logger;

public final class VillagerReactionEngine {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int WORLD_SPEAKER_RADIUS = 64;
    private static final int MAX_WORLD_SPEAKERS = 64;
    private static final int SEQUENCE_PAUSE_TICKS = 8;
    private static final int ATTENTION_RADIUS = 64;
    private static final int POST_SCENE_SILENCE_TICKS = 100;
    private static final int MAX_OCCURRENCE_MEMORY = 4096;
    private static final String SPEAKER_COOLDOWN_PREFIX = "JemSpeechEventCooldown.";
    private static final String SPEAKER_BUSY_UNTIL_TAG = "JemSpeechBusyUntil";
    private static final String SPEAKER_BUSY_PRIORITY_TAG = "JemSpeechBusyPriority";
    private static final String OCCURRENCE_COUNT_PREFIX = "JemSpeechOccurrenceCount.";
    private static final String LAST_LINE_TAG = "JemSpeechLastLine";
    private static final Map<MinecraftServer, RuntimeState> RUNTIME = new IdentityHashMap<>();

    private VillagerReactionEngine() {
    }

    public static boolean accept(SemanticOccurrence occurrence) {
        RuntimeState runtime = RUNTIME.computeIfAbsent(occurrence.server(), ignored -> new RuntimeState());
        long now = gameTime(occurrence);
        runtime.expire(now);
        String consumedKey = occurrence.event().id() + ":" + occurrence.occurrenceId();
        if (runtime.consumedOccurrences.containsKey(consumedKey)) {
            debug(occurrence, "event not emitted", "reason=DUPLICATE_OCCURRENCE");
            return false;
        }
        runtime.consumedOccurrences.put(consumedKey, now + occurrence.event().cooldowns().sameLineTicks());
        String eventCooldownKey = eventCooldownKey(occurrence);
        long eventReadyAt = runtime.eventCooldowns.getOrDefault(eventCooldownKey, 0L);
        if (eventReadyAt > now) {
            debug(occurrence, "event rejected", "reason=EVENT_COOLDOWN readyAt=" + eventReadyAt);
            return false;
        }
        if (!runtime.attentionAllows(occurrence, now)) {
            debug(occurrence, "event rejected", "reason=ACTIVE_HIGHER_PRIORITY_SCENE");
            return false;
        }
        List<Villager> candidates = candidates(occurrence);
        if (candidates.isEmpty()) {
            debug(occurrence, "event rejected", "perception=FAIL reason=NO_ELIGIBLE_SPEAKER");
            return false;
        }
        debug(occurrence, "perception=PASS", "speakerCandidates=" + candidates.size());
        List<Selection> selections = select(occurrence, candidates, runtime, now);
        if (selections.isEmpty()) {
            debug(occurrence, "event rejected", "reason=NO_REACTION_CANDIDATE");
            return false;
        }
        if (occurrence.event().speakers().sceneMode() == VillagerSpeechCatalog.SceneMode.MULTI_SPEAKER_OR_SEQUENCE
                && occurrence.event().reactions().size() > 1 && selections.size() < 2) {
            debug(occurrence, "event rejected", "reason=SCENE_REQUIRES_MULTIPLE_SPEAKERS");
            return false;
        }
        int delay = 0;
        boolean authoredScene = occurrence.event().speakers().sceneMode()
                == VillagerSpeechCatalog.SceneMode.MULTI_SPEAKER_OR_SEQUENCE;
        for (int index = 0; index < selections.size(); index++) {
            Selection selection = selections.get(index);
            applySelection(occurrence, selection, runtime, now, delay);
            VillagerSpeechNetwork.send(
                    selection.villager(),
                    selection.line(),
                    delay,
                    authoredScene || occurrence.event().activation().mode() != VillagerSpeechCatalog.ActivationMode.SPARSE_AMBIENT,
                    authoredScene,
                    VillagerSpeechCatalog.speakerRole(selection.villager()),
                    selection.villager().hasCustomName() ? selection.villager().getCustomName().getString() : "",
                    index + 1
            );
            debug(occurrence, "selected=" + selection.line().id(),
                    "speaker=" + selection.villager().getUUID() + " consumedOccurrence=true rearm="
                            + occurrence.event().activation().rearmCondition());
            delay += VillagerSpeechCatalog.DISPLAY_TICKS + SEQUENCE_PAUSE_TICKS;
        }
        runtime.eventCooldowns.put(eventCooldownKey,
                now + Math.max(occurrence.event().cooldowns().eventTicks(), delay));
        int finalDelay = Math.max(0, delay - SEQUENCE_PAUSE_TICKS);
        long sceneEnds = now + finalDelay;
        runtime.claimAttention(occurrence, occurrence.event().attentionPriority(), sceneEnds + POST_SCENE_SILENCE_TICKS);
        return true;
    }

    public static void clear(MinecraftServer server) {
        RUNTIME.remove(server);
    }

    private static List<Villager> candidates(SemanticOccurrence occurrence) {
        List<Villager> candidates = new ArrayList<>();
        if (occurrence.event().perception().scope() == VillagerSpeechCatalog.Scope.WORLD) {
            Set<UUID> seen = new LinkedHashSet<>();
            for (ServerLevel level : occurrence.server().getAllLevels()) {
                for (ServerPlayer player : level.players()) {
                    for (Villager villager : level.getEntitiesOfClass(
                            Villager.class,
                            player.getBoundingBox().inflate(WORLD_SPEAKER_RADIUS),
                            candidate -> worldEligible(occurrence, candidate)
                    )) {
                        if (seen.add(villager.getUUID())) {
                            candidates.add(villager);
                            if (candidates.size() >= MAX_WORLD_SPEAKERS) {
                                return candidates;
                            }
                        }
                    }
                }
            }
            return candidates;
        }
        if (occurrence.level() == null) {
            return List.of();
        }
        BlockPos origin = origin(occurrence);
        int radius = occurrence.event().perception().distanceOr(WORLD_SPEAKER_RADIUS);
        candidates.addAll(occurrence.level().getEntitiesOfClass(
                Villager.class,
                new AABB(origin).inflate(radius),
                villager -> eligibleSpeaker(occurrence, villager, origin, radius)
        ));
        candidates.sort(Comparator.comparingDouble(villager -> villager.distanceToSqr(
                origin.getX() + 0.5D,
                origin.getY() + 0.5D,
                origin.getZ() + 0.5D
        )));
        return candidates;
    }

    private static boolean eligibleSpeaker(SemanticOccurrence occurrence, Villager villager, BlockPos origin, int radius) {
        if (occurrence.event().speakers().alive() && !villager.isAlive()) {
            return false;
        }
        if (occurrence.event().speakers().awake() && villager.isSleeping()) {
            return false;
        }
        if (villager.distanceToSqr(origin.getX() + 0.5D, origin.getY() + 0.5D, origin.getZ() + 0.5D) > radius * radius) {
            return false;
        }
        Entity subject = occurrence.subject();
        if (occurrence.event().perception().requiresLineOfSight()) {
            return subject != null && subject.level() == villager.level()
                    && (subject == villager || villager.hasLineOfSight(subject));
        }
        return true;
    }

    private static boolean worldEligible(SemanticOccurrence occurrence, Villager villager) {
        return (!occurrence.event().speakers().alive() || villager.isAlive())
                && (!occurrence.event().speakers().awake() || !villager.isSleeping());
    }

    private static List<Selection> select(
            SemanticOccurrence occurrence,
            List<Villager> candidates,
            RuntimeState runtime,
            long now
    ) {
        boolean scene = occurrence.event().speakers().sceneMode()
                == VillagerSpeechCatalog.SceneMode.MULTI_SPEAKER_OR_SEQUENCE;
        int limit = scene
                ? Math.min(occurrence.event().activation().maxReactionsPerOccurrence(), occurrence.event().reactions().size())
                : 1;
        List<Selection> selections = new ArrayList<>();
        Set<UUID> usedSpeakers = new LinkedHashSet<>();
        Set<String> usedLines = new LinkedHashSet<>();
        if (scene) {
            for (VillagerSpeechCatalog.Line authoredLine : occurrence.event().reactions()) {
                Selection selection = candidates.stream()
                        .filter(villager -> !usedSpeakers.contains(villager.getUUID()))
                        .filter(authoredLine::matches)
                        .map(villager -> eligibleSelection(occurrence, villager, authoredLine, runtime, now))
                        .filter(java.util.Objects::nonNull)
                        .findFirst()
                        .orElse(null);
                if (selection != null) {
                    debug(occurrence, "candidate=" + authoredLine.id(),
                            "ACCEPT speaker=" + selection.villager().getUUID());
                    selections.add(selection);
                    usedSpeakers.add(selection.villager().getUUID());
                    usedLines.add(selection.line().id());
                    if (selections.size() >= limit) {
                        break;
                    }
                } else {
                    debug(occurrence, "candidate=" + authoredLine.id(),
                            "REJECT reason=NO_DISTINCT_ELIGIBLE_SPEAKER");
                }
            }
            return selections;
        }
        for (Villager villager : candidates) {
            int occurrenceCount = occurrenceCount(villager, occurrence.event().id());
            String context = contextState(occurrence, occurrenceCount);
            List<VillagerSpeechCatalog.Line> lines = occurrence.event().lines(villager, context);
            int specificity = lines.stream().mapToInt(VillagerSpeechCatalog.Line::specificity).max().orElse(-1);
            int priority = lines.stream()
                    .filter(line -> line.specificity() == specificity)
                    .mapToInt(VillagerSpeechCatalog.Line::priority)
                    .max()
                    .orElse(-1);
            List<VillagerSpeechCatalog.Line> eligible = lines.stream()
                    .filter(line -> line.specificity() == specificity && line.priority() == priority)
                    .filter(line -> eligibleSelection(occurrence, villager, line, runtime, now) != null)
                    .toList();
            for (VillagerSpeechCatalog.Line line : occurrence.event().reactions()) {
                String reason = rejectionReason(
                        occurrence,
                        villager,
                        line,
                        lines,
                        specificity,
                        priority,
                        runtime,
                        now
                );
                debug(occurrence, "candidate=" + line.id(), reason);
            }
            if (!eligible.isEmpty()) {
                String recent = runtime.recentLines.getOrDefault(occurrence.event().id(), "");
                String previous = villager.getPersistentData().getString(LAST_LINE_TAG);
                List<VillagerSpeechCatalog.Line> fresh = eligible.stream()
                        .filter(line -> !line.id().equals(recent) && !line.id().equals(previous))
                        .toList();
                List<VillagerSpeechCatalog.Line> pool = fresh.isEmpty() ? eligible : fresh;
                VillagerSpeechCatalog.Line line = pool.get(villager.getRandom().nextInt(pool.size()));
                selections.add(new Selection(villager, line));
                break;
            }
        }
        return selections;
    }

    private static String rejectionReason(
            SemanticOccurrence occurrence,
            Villager villager,
            VillagerSpeechCatalog.Line line,
            List<VillagerSpeechCatalog.Line> contextualLines,
            int specificity,
            int priority,
            RuntimeState runtime,
            long now
    ) {
        if (!line.matches(villager)) {
            return "REJECT reason=SPEAKER_INELIGIBLE";
        }
        if (!contextualLines.contains(line)) {
            return "REJECT reason=CONTEXT_MISMATCH";
        }
        if (line.specificity() < specificity) {
            return "REJECT reason=LESS_SPECIFIC";
        }
        if (line.priority() < priority) {
            return "REJECT reason=LOWER_PRIORITY";
        }
        if (speakerBusy(villager, occurrence.event().attentionPriority(), now)) {
            return "REJECT reason=SPEAKER_BUSY";
        }
        if (villager.getPersistentData().getLong(SPEAKER_COOLDOWN_PREFIX + occurrence.event().id()) > now) {
            return "REJECT reason=SPEAKER_COOLDOWN";
        }
        if (runtime.lineCooldowns.getOrDefault(villager.getUUID() + ":" + line.id(), 0L) > now) {
            return "REJECT reason=SAME_LINE_COOLDOWN";
        }
        return "ACCEPT priority=" + line.priority();
    }

    private static Selection eligibleSelection(
            SemanticOccurrence occurrence,
            Villager villager,
            VillagerSpeechCatalog.Line line,
            RuntimeState runtime,
            long now
    ) {
        if (speakerBusy(villager, occurrence.event().attentionPriority(), now)) {
            return null;
        }
        if (villager.getPersistentData().getLong(SPEAKER_COOLDOWN_PREFIX + occurrence.event().id()) > now) {
            return null;
        }
        String lineKey = villager.getUUID() + ":" + line.id();
        if (runtime.lineCooldowns.getOrDefault(lineKey, 0L) > now) {
            return null;
        }
        return new Selection(villager, line);
    }

    private static boolean speakerBusy(Villager villager, int priority, long now) {
        return villager.getPersistentData().getLong(SPEAKER_BUSY_UNTIL_TAG) > now
                && villager.getPersistentData().getInt(SPEAKER_BUSY_PRIORITY_TAG) >= priority;
    }

    private static void applySelection(
            SemanticOccurrence occurrence,
            Selection selection,
            RuntimeState runtime,
            long now,
            int delay
    ) {
        Villager villager = selection.villager();
        villager.getPersistentData().putLong(
                SPEAKER_COOLDOWN_PREFIX + occurrence.event().id(),
                now + delay + occurrence.event().cooldowns().speakerTicks()
        );
        villager.getPersistentData().putLong(
                SPEAKER_BUSY_UNTIL_TAG,
                now + delay + VillagerSpeechCatalog.DISPLAY_TICKS
        );
        villager.getPersistentData().putInt(
                SPEAKER_BUSY_PRIORITY_TAG,
                occurrence.event().attentionPriority()
        );
        villager.getPersistentData().putInt(
                OCCURRENCE_COUNT_PREFIX + occurrence.event().id(),
                occurrenceCount(villager, occurrence.event().id()) + 1
        );
        villager.getPersistentData().putString(LAST_LINE_TAG, selection.line().id());
        runtime.lineCooldowns.put(
                villager.getUUID() + ":" + selection.line().id(),
                now + delay + occurrence.event().cooldowns().sameLineTicks()
        );
        runtime.recentLines.put(occurrence.event().id(), selection.line().id());
    }

    private static String contextState(SemanticOccurrence occurrence, int occurrenceCount) {
        if (occurrence.contextState() != null && !occurrence.contextState().isBlank()) {
            return occurrence.contextState();
        }
        if (occurrenceCount == 0 && occurrence.event().hasContext("first")) {
            return "first";
        }
        if (occurrenceCount > 0 && occurrence.event().hasContext("repeat")) {
            return "repeat";
        }
        if (occurrence.event().hasContext("remembered")) {
            return "remembered";
        }
        return "";
    }

    private static int occurrenceCount(Villager villager, String eventId) {
        return villager.getPersistentData().getInt(OCCURRENCE_COUNT_PREFIX + eventId);
    }

    private static BlockPos origin(SemanticOccurrence occurrence) {
        if (occurrence.origin() != null) {
            return occurrence.origin();
        }
        if (occurrence.subject() != null) {
            return occurrence.subject().blockPosition();
        }
        if (occurrence.actor() != null) {
            return occurrence.actor().blockPosition();
        }
        return BlockPos.ZERO;
    }

    private static long gameTime(SemanticOccurrence occurrence) {
        return occurrence.level() == null
                ? occurrence.server().overworld().getGameTime()
                : occurrence.level().getGameTime();
    }

    private static String eventCooldownKey(SemanticOccurrence occurrence) {
        String dimension = occurrence.level() == null ? "world" : occurrence.level().dimension().location().toString();
        return dimension + ":" + occurrence.event().id();
    }

    private static void debug(SemanticOccurrence occurrence, String first, String second) {
        LOGGER.debug(
                "[REACTION] event={} occurrence={} activation={} {} {}",
                occurrence.event().id(),
                occurrence.occurrenceId(),
                occurrence.event().activation().mode(),
                first,
                second
        );
    }

    private record Selection(Villager villager, VillagerSpeechCatalog.Line line) {
    }

    private static final class RuntimeState {
        private final Map<String, Long> consumedOccurrences = new HashMap<>();
        private final Map<String, Long> eventCooldowns = new HashMap<>();
        private final Map<String, Long> lineCooldowns = new HashMap<>();
        private final Map<String, String> recentLines = new HashMap<>();
        private final List<Attention> attention = new ArrayList<>();

        private void expire(long now) {
            if (consumedOccurrences.size() >= MAX_OCCURRENCE_MEMORY) {
                removeExpired(consumedOccurrences, now);
            }
            if (lineCooldowns.size() >= MAX_OCCURRENCE_MEMORY) {
                removeExpired(lineCooldowns, now);
            }
            removeExpired(eventCooldowns, now);
            attention.removeIf(value -> value.readyAt() <= now);
        }

        private boolean attentionAllows(SemanticOccurrence occurrence, long now) {
            int priority = occurrence.event().attentionPriority();
            return attention.stream()
                    .filter(value -> value.readyAt() > now && value.overlaps(occurrence))
                    .allMatch(value -> priority > value.priority());
        }

        private void claimAttention(SemanticOccurrence occurrence, int priority, long readyAt) {
            attention.removeIf(value -> value.overlaps(occurrence));
            attention.add(Attention.from(occurrence, priority, readyAt));
        }

        private static void removeExpired(Map<String, Long> values, long now) {
            Iterator<Map.Entry<String, Long>> iterator = values.entrySet().iterator();
            while (iterator.hasNext()) {
                if (iterator.next().getValue() <= now) {
                    iterator.remove();
                }
            }
        }
    }

    private record Attention(String dimension, BlockPos origin, boolean world, int priority, long readyAt) {
        private static Attention from(SemanticOccurrence occurrence, int priority, long readyAt) {
            return new Attention(
                    occurrence.level() == null ? "" : occurrence.level().dimension().location().toString(),
                    VillagerReactionEngine.origin(occurrence),
                    occurrence.level() == null,
                    priority,
                    readyAt
            );
        }

        private boolean overlaps(SemanticOccurrence occurrence) {
            if (world || occurrence.level() == null) {
                return true;
            }
            if (!dimension.equals(occurrence.level().dimension().location().toString())) {
                return false;
            }
            BlockPos other = VillagerReactionEngine.origin(occurrence);
            long x = origin.getX() - other.getX();
            long y = origin.getY() - other.getY();
            long z = origin.getZ() - other.getZ();
            return x * x + y * y + z * z <= (long) ATTENTION_RADIUS * ATTENTION_RADIUS;
        }
    }
}
