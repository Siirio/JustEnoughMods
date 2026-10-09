package com.siirio.jemvillagertalking.speech;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

public final class VillagerReactionRegistry {
    private static final Map<String, Registration> REGISTRATIONS = registrations();

    private VillagerReactionRegistry() {
    }

    public static void bootstrap() {
        if (REGISTRATIONS.size() != VillagerSpeechCatalog.eventCount()) {
            throw new IllegalStateException("Villager detector coverage mismatch");
        }
    }

    public static boolean emitLocal(
            String eventId,
            String occurrenceId,
            ServerLevel level,
            BlockPos origin,
            Entity subject,
            Entity actor,
            String contextState
    ) {
        Registration registration = requireRegistration(eventId);
        return VillagerReactionEngine.accept(SemanticOccurrence.local(
                registration.event(), requireOccurrenceId(occurrenceId), level, origin, subject, actor, contextState
        ));
    }

    public static boolean emitWorld(
            String eventId,
            String occurrenceId,
            MinecraftServer server,
            Entity subject,
            Entity actor,
            String contextState
    ) {
        Registration registration = requireRegistration(eventId);
        return VillagerReactionEngine.accept(SemanticOccurrence.world(
                registration.event(), requireOccurrenceId(occurrenceId), server, subject, actor, contextState
        ));
    }

    public static Registration requireRegistration(String eventId) {
        Registration registration = REGISTRATIONS.get(normalize(eventId));
        if (registration == null) {
            throw new IllegalArgumentException("Unregistered villager semantic event " + eventId);
        }
        return registration;
    }

    public static int registeredCount() {
        return REGISTRATIONS.size();
    }

    public static Set<String> registeredEventIds() {
        return REGISTRATIONS.keySet();
    }

    public static Set<String> catalogEventIds() {
        Set<String> ids = new LinkedHashSet<>();
        VillagerSpeechCatalog.events().forEach(event -> ids.add(event.id()));
        return Collections.unmodifiableSet(ids);
    }

    public static void clearRuntimeState(MinecraftServer server) {
        VillagerReactionEngine.clear(server);
        VillagerReactionTracker.clear(server);
    }

    private static Map<String, Registration> registrations() {
        Map<String, Registration> registrations = new LinkedHashMap<>();
        for (VillagerSpeechCatalog.EventDefinition event : VillagerSpeechCatalog.events()) {
            DetectorAdapter adapter = DetectorAdapter.from(event.handlerStrategy());
            Registration previous = registrations.put(event.id(), new Registration(event, adapter));
            if (previous != null) {
                throw new IllegalStateException("Duplicate villager detector registration " + event.id());
            }
        }
        Set<String> catalog = new LinkedHashSet<>();
        VillagerSpeechCatalog.events().forEach(event -> catalog.add(event.id()));
        if (!registrations.keySet().equals(catalog)) {
            throw new IllegalStateException("Villager detector coverage mismatch");
        }
        return Collections.unmodifiableMap(registrations);
    }

    private static String requireOccurrenceId(String occurrenceId) {
        if (occurrenceId == null || occurrenceId.isBlank()) {
            throw new IllegalArgumentException("Villager semantic occurrence id is required");
        }
        return occurrenceId;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    public record Registration(VillagerSpeechCatalog.EventDefinition event, DetectorAdapter adapter) {
    }

    public enum DetectorAdapter {
        ENTITY_COMBAT_STATE_TRANSITION,
        EXISTING_OR_DERIVED_FORGE_HOOK,
        FORGE_BLOCK_EVENT,
        FORGE_BLOCK_OR_EXPLOSION_EVENT,
        FORGE_CONTAINER_EVENT,
        FORGE_ITEM_TOSS_EVENT,
        FORGE_LIVING_HURT_EVENT,
        FORGE_MOUNT_EVENT,
        FORGE_PLAYER_INTERACTION_EVENT,
        FORGE_PLAYER_LIFECYCLE_EVENT,
        FORGE_SLEEP_OR_BLOCK_INTERACTION_EVENT,
        FORGE_SOUND_EVENT,
        FORGE_STATE_EVENT_OR_TRANSITION_TRACKER,
        FORGE_TRADE_EVENT,
        MOD_ADAPTER_SEMANTIC_HOOK,
        SEMANTIC_DETECTOR_OR_FORGE_EVENT,
        TRANSITION_POLL_OR_ENTITY_OBSERVATION,
        TRANSITION_TRACKER,
        VILLAGER_TRADE_LIFECYCLE_HOOK,
        WORLD_STATE_TRANSITION_TRACKER;

        private static DetectorAdapter from(String strategy) {
            try {
                return valueOf(strategy.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                throw new IllegalStateException("Unknown villager detector strategy " + strategy, exception);
            }
        }
    }
}
