package com.siirio.jemvillagertalking.speech;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

public final class VillagerSpeechCatalog {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String RESOURCE = "/assets/jem_villager_talking/villager_speech_master_v2.json";
    private static final Set<String> SPEAKER_ROLES = Set.of(
            "adult", "any", "armorer", "butcher", "cartographer", "child", "cleric", "farmer",
            "fisherman", "fletcher", "leatherworker", "librarian", "mason", "nitwit", "shepherd",
            "toolsmith", "unemployed", "weaponsmith"
    );
    public static final int DISPLAY_TICKS = 60;
    private static final Map<String, EventDefinition> EVENTS;
    private static final Map<String, Line> LINES;

    static {
        Map<String, EventDefinition> events = new LinkedHashMap<>();
        Map<String, Line> lines = new LinkedHashMap<>();
        try (var stream = VillagerSpeechCatalog.class.getResourceAsStream(RESOURCE)) {
            if (stream == null) {
                throw new IllegalStateException("Missing villager speech V2 catalog");
            }
            try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                if (!"2.0".equals(root.get("schema_version").getAsString())) {
                    throw new IllegalStateException("Unsupported villager speech catalog schema");
                }
                for (var value : root.getAsJsonArray("events")) {
                    EventDefinition definition = parseEvent(value.getAsJsonObject(), lines);
                    if (events.put(definition.id(), definition) != null) {
                        throw new IllegalStateException("Duplicate villager speech event " + definition.id());
                    }
                }
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot load villager speech V2 catalog", exception);
        }
        if (events.isEmpty() || lines.isEmpty()) {
            throw new IllegalStateException("Empty villager speech V2 catalog");
        }
        EVENTS = Collections.unmodifiableMap(events);
        LINES = Collections.unmodifiableMap(lines);
    }

    private VillagerSpeechCatalog() {
    }

    public static EventDefinition event(String id) {
        return EVENTS.get(normalize(id));
    }

    public static EventDefinition requireEvent(String id) {
        EventDefinition definition = event(id);
        if (definition == null) {
            throw new IllegalArgumentException("Unknown villager semantic event " + id);
        }
        return definition;
    }

    public static Collection<EventDefinition> events() {
        return EVENTS.values();
    }

    public static Line line(String id) {
        return LINES.get(id);
    }

    public static int eventCount() {
        return EVENTS.size();
    }

    public static int lineCount() {
        return LINES.size();
    }

    public static String speakerRole(Villager villager) {
        if (villager.isBaby()) {
            return "child";
        }
        VillagerProfession profession = villager.getVillagerData().getProfession();
        if (profession == VillagerProfession.NONE) {
            return "unemployed";
        }
        if (profession == VillagerProfession.NITWIT) {
            return "nitwit";
        }
        var id = ForgeRegistries.VILLAGER_PROFESSIONS.getKey(profession);
        return id == null ? "villager" : id.getPath();
    }

    private static EventDefinition parseEvent(JsonObject object, Map<String, Line> lines) {
        String id = normalize(requiredString(object, "event_id"));
        JsonObject activationObject = object.getAsJsonObject("activation");
        Activation activation = new Activation(
                enumValue(ActivationMode.class, requiredString(activationObject, "mode")),
                requiredString(activationObject, "rearm_condition"),
                activationObject.get("max_reactions_per_occurrence").getAsInt()
        );
        JsonObject perceptionObject = object.getAsJsonObject("perception");
        Integer distance = perceptionObject.get("max_distance_blocks").isJsonNull()
                ? null
                : perceptionObject.get("max_distance_blocks").getAsInt();
        Perception perception = new Perception(
                enumValue(Scope.class, requiredString(perceptionObject, "scope")),
                enumValue(Channel.class, requiredString(perceptionObject, "channel")),
                distance,
                perceptionObject.get("requires_line_of_sight").getAsBoolean(),
                perceptionObject.get("same_dimension_required").getAsBoolean(),
                enumValue(KnowledgeType.class, requiredString(perceptionObject, "knowledge_type"))
        );
        JsonObject speakerObject = object.getAsJsonObject("speaker_requirements");
        SpeakerRequirements speakers = new SpeakerRequirements(
                speakerObject.get("alive").getAsBoolean(),
                speakerObject.get("awake").getAsBoolean(),
                splitHints(requiredString(speakerObject, "current_speaker_hints")),
                enumValue(SceneMode.class, requiredString(speakerObject, "scene_mode"))
        );
        JsonObject cooldownObject = object.getAsJsonObject("cooldowns");
        Cooldowns cooldowns = new Cooldowns(
                cooldownObject.get("event_ticks").getAsInt(),
                cooldownObject.get("speaker_ticks").getAsInt(),
                cooldownObject.get("same_line_ticks").getAsInt()
        );
        JsonObject handlerObject = object.getAsJsonObject("handler");
        if (!handlerObject.get("must_implement").getAsBoolean()) {
            throw new IllegalStateException("Non-production catalog event " + id);
        }
        List<Line> reactions = new ArrayList<>();
        for (var value : object.getAsJsonArray("reactions")) {
            JsonObject reactionObject = value.getAsJsonObject();
            String lineId = requiredString(reactionObject, "line_id");
            Map<String, String> text = new LinkedHashMap<>();
            reactionObject.getAsJsonObject("text").entrySet()
                    .forEach(entry -> text.put(normalize(entry.getKey()), entry.getValue().getAsString()));
            Line line = new Line(
                    lineId,
                    id,
                    normalize(requiredString(reactionObject, "speaker")),
                    optionalString(reactionObject, "context_state"),
                    normalize(requiredString(reactionObject, "tone")),
                    reactionObject.get("priority").getAsInt(),
                    Map.copyOf(text)
            );
            if (!SPEAKER_ROLES.contains(line.speaker())) {
                throw new IllegalStateException("Unknown villager speaker role " + line.speaker());
            }
            if (!line.text().containsKey("en_us") || !line.text().containsKey("ru_ru")
                    || line.text().get("en_us").isBlank() || line.text().get("ru_ru").isBlank()) {
                throw new IllegalStateException("Missing localized villager speech line " + lineId);
            }
            if (lines.put(lineId, line) != null) {
                throw new IllegalStateException("Duplicate villager speech line " + lineId);
            }
            reactions.add(line);
        }
        if (reactions.isEmpty()) {
            throw new IllegalStateException("Villager speech event has no reactions " + id);
        }
        EventDefinition definition = new EventDefinition(
                id,
                requiredString(object, "label"),
                activation,
                perception,
                speakers,
                cooldowns,
                normalize(requiredString(handlerObject, "strategy")),
                List.copyOf(reactions)
        );
        validateEvent(definition);
        reportShadowedReactions(definition);
        return definition;
    }

    private static void validateEvent(EventDefinition event) {
        if (event.speakers().sceneMode() == SceneMode.SINGLE_SPEAKER
                && event.activation().maxReactionsPerOccurrence() != 1) {
            throw new IllegalStateException("Single-speaker event allows multiple reactions " + event.id());
        }
        if (event.perception().scope() == Scope.WORLD
                && (event.perception().knowledgeType() != KnowledgeType.WORLD_AWARENESS
                || event.perception().requiresLineOfSight()
                || event.perception().maxDistanceBlocks() != null)) {
            throw new IllegalStateException("Impossible world-awareness conditions " + event.id());
        }
    }

    private static void reportShadowedReactions(EventDefinition event) {
        for (Line line : event.reactions()) {
            int strongestPriority = event.reactions().stream()
                    .filter(candidate -> candidate.speaker().equals(line.speaker()))
                    .filter(candidate -> candidate.contextState().equals(line.contextState()))
                    .mapToInt(Line::priority)
                    .max()
                    .orElse(line.priority());
            if (line.priority() < strongestPriority) {
                LOGGER.warn(
                        "Villager reaction {} is shadowed in event {} by priority {}",
                        line.id(),
                        event.id(),
                        strongestPriority
                );
            }
        }
    }

    private static List<String> splitHints(String value) {
        return java.util.Arrays.stream(value.split(","))
                .map(VillagerSpeechCatalog::normalize)
                .filter(entry -> !entry.isBlank())
                .toList();
    }

    private static String requiredString(JsonObject object, String name) {
        if (!object.has(name) || object.get(name).isJsonNull()) {
            throw new IllegalStateException("Missing villager speech field " + name);
        }
        return object.get(name).getAsString();
    }

    private static String optionalString(JsonObject object, String name) {
        return object.has(name) && !object.get(name).isJsonNull() ? normalize(object.get(name).getAsString()) : "";
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static <T extends Enum<T>> T enumValue(Class<T> type, String value) {
        return Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT));
    }

    public record EventDefinition(
            String id,
            String label,
            Activation activation,
            Perception perception,
            SpeakerRequirements speakers,
            Cooldowns cooldowns,
            String handlerStrategy,
            List<Line> reactions
    ) {
        public List<Line> lines(Villager villager, String contextState) {
            List<Line> eligible = reactions.stream().filter(line -> line.matches(villager)).toList();
            if (contextState == null || contextState.isBlank()) {
                List<Line> withoutContext = eligible.stream().filter(line -> line.contextState().isBlank()).toList();
                return withoutContext.isEmpty() ? eligible : withoutContext;
            }
            List<Line> exact = eligible.stream().filter(line -> line.contextState().equals(contextState)).toList();
            if (!exact.isEmpty()) {
                return exact;
            }
            List<Line> withoutContext = eligible.stream().filter(line -> line.contextState().isBlank()).toList();
            return withoutContext.isEmpty() ? eligible : withoutContext;
        }

        public boolean hasContext(String contextState) {
            return reactions.stream().anyMatch(line -> line.contextState().equals(contextState));
        }

        public int attentionPriority() {
            return reactions.stream().mapToInt(Line::priority).max().orElse(0);
        }
    }

    public record Activation(ActivationMode mode, String rearmCondition, int maxReactionsPerOccurrence) {
    }

    public record Perception(
            Scope scope,
            Channel channel,
            Integer maxDistanceBlocks,
            boolean requiresLineOfSight,
            boolean sameDimensionRequired,
            KnowledgeType knowledgeType
    ) {
        public int distanceOr(int fallback) {
            return maxDistanceBlocks == null ? fallback : maxDistanceBlocks;
        }
    }

    public record SpeakerRequirements(boolean alive, boolean awake, List<String> hints, SceneMode sceneMode) {
    }

    public record Cooldowns(int eventTicks, int speakerTicks, int sameLineTicks) {
    }

    public record Line(
            String id,
            String eventId,
            String speaker,
            String contextState,
            String tone,
            int priority,
            Map<String, String> text
    ) {
        public boolean matches(Villager villager) {
            if (villager.isBaby()) {
                return speaker.equals("child") || speaker.equals("any");
            }
            if (speaker.equals("child")) {
                return false;
            }
            if (speaker.equals("any") || speaker.equals("adult")) {
                return true;
            }
            VillagerProfession profession = villager.getVillagerData().getProfession();
            if (speaker.equals("unemployed")) {
                return profession == VillagerProfession.NONE;
            }
            if (speaker.equals("nitwit")) {
                return profession == VillagerProfession.NITWIT;
            }
            var id = ForgeRegistries.VILLAGER_PROFESSIONS.getKey(profession);
            return id != null && id.getPath().equals(speaker);
        }

        public int specificity() {
            return speaker.equals("any") || speaker.equals("adult") ? 0 : 1;
        }

        public String text(String language) {
            String normalized = normalize(language);
            return text.getOrDefault(normalized, text.getOrDefault("en_us", text.getOrDefault("ru_ru", "")));
        }
    }

    public enum ActivationMode {
        OCCURRENCE,
        STATE_ENTER,
        STATE_EXIT,
        PERCEPTION_ENTER,
        STATE_ENTER_WITH_SPARSE_REPEAT,
        SPARSE_AMBIENT
    }

    public enum Scope {
        LOCAL,
        WORLD,
        MEMORY,
        LOCAL_OR_GOSSIP
    }

    public enum Channel {
        SIGHT,
        CONTEXT,
        WORLD_AWARENESS,
        HEARING,
        GOSSIP_OR_MEMORY,
        DIRECT_OR_HEARING,
        DIRECT_OR_GOSSIP,
        SIGHT_OR_HEARING
    }

    public enum KnowledgeType {
        DIRECT_CONTEXT,
        DIRECT_OR_HEARD,
        DIRECT_VISUAL,
        HEARD,
        MEMORY_OR_GOSSIP,
        WORLD_AWARENESS
    }

    public enum SceneMode {
        SINGLE_SPEAKER,
        MULTI_SPEAKER_OR_SEQUENCE
    }
}
