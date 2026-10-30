package com.justenoughmods.achievementguide.compat.ftb;

import com.google.gson.GsonBuilder;
import dev.ftb.mods.ftblibrary.snbt.SNBT;
import dev.ftb.mods.ftbquests.quest.BaseQuestFile;
import dev.ftb.mods.ftbquests.quest.ServerQuestFile;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

/** Versioned per-world authored-book projection. Native FTB remains the sole progress/save/sync owner. */
public final class FtbQuestProjection {
    private static final LevelResource ROOT = new LevelResource("data/jem_quest_projection");
    private static final Set<String> LEGACY = Set.of(
            "1100000000000013", "1100000000000023", "1100000000000033", "1100000000000025");
    private static final Set<String> ECONOMY = Set.of(
            "1100000000000007", "1100000000000017", "1100000000000047", "1100000000000057",
            "1100000000000067", "1100000000000077", "1100000000000087");
    private static final Set<String> SETTLEMENTS = Set.of(
            "1100000000000027", "1100000000000037", "1100000000000097", "1100000000000107",
            "1100000000000117", "1100000000000127", "1100000000000137");
    private record Features(boolean economy, boolean settlements, boolean recruits, boolean races,
                            boolean campaign, boolean events, boolean create, boolean planning) {}
    private record Projection(Features features, Set<Long> active, Map<Long, Long> dormantTasks) {}
    private static final Map<MinecraftServer, Projection> CURRENT = new HashMap<>();

    private FtbQuestProjection() {}

    private static Features features(MinecraftServer server) {
        return new Features(QuestSignals.economy(server), QuestSignals.loaded("jem_settlements"),
                QuestSignals.loaded("recruits"), QuestSignals.loaded("jem_roles"), QuestSignals.campaign(server),
                QuestSignals.events(server), QuestSignals.loaded("create"),
                QuestSignals.loaded("jem_server") && QuestSignals.loaded("xaeroworldmap"));
    }

    public static Path prepare(MinecraftServer server, Path authored) {
        if (!Files.isRegularFile(authored.resolve("data.snbt"))) return authored;
        try {
            Map<String, CompoundTag> definitions = read(authored);
            if (!definitions.get("data.snbt").getBoolean("jem_universal_book")) return authored;
            CompoundTag metadata = definitions.get("data.snbt");
            boolean visualV4 = metadata.getBoolean("jem_visual_v4");
            if (visualV4 && metadata.getBoolean("jem_history_migration")
                    && metadata.getString("jem_migration_version").equals("visual-v4-1"))
                BookMigrationDefinitions.prepare(server, definitions);
            else BookMigration.clear(server);
            BookMigration.currentBook(server, visualV4);
            if (definitions.containsKey("chapters/_jem_unavailable.snbt"))
                throw new IOException("The reserved JEM dormant chapter path is already authored: " + authored);
            Features features = features(server);
            boolean authoredBook = definitions.get("data.snbt").getInt("jem_questbook_revision") >= 3;
            Map<String, String> unavailableReasons = new TreeMap<>();
            List<CompoundTag> dormant = new ArrayList<>();
            Set<String> unavailable = new HashSet<>(LEGACY);
            Set<String> conditional = new HashSet<>();
            Map<String, CompoundTag> quests = new LinkedHashMap<>();
            for (var entry : definitions.entrySet()) {
                if (!entry.getKey().startsWith("chapters/")) continue;
                CompoundTag chapter = entry.getValue();
                boolean enabled = !chapter.getBoolean("jem_archive") && chapterEnabled(chapter.getString("filename"), features);
                chapter.putBoolean("always_invisible", !enabled || chapter.getBoolean("always_invisible"));
                for (Tag raw : chapter.getList("quests", Tag.TAG_COMPOUND)) {
                    CompoundTag quest = (CompoundTag)raw;
                    String id = quest.getString("id");
                    quests.put(id, quest);
                    if (!enabled || !questEnabled(id, features) || !referencesAvailable(quest)) unavailable.add(id);
                    if (authoredBook && !chapter.getBoolean("jem_archive")) {
                        pruneUnavailableRewards(quest);
                        String reason = authoredUnavailable(quest, definitions, features, server);
                        if (!reason.isEmpty()) {
                            unavailable.add(id); unavailableReasons.put(id, reason);
                            String condition = quest.getString("jem_native_condition");
                            if (!condition.isEmpty() && !QuestSignals.nativeCondition(condition)) conditional.add(id);
                        }
                    }
                }
            }
            // A native calendar/config condition is optional context, never a gate on ordinary V4 goals.
            if (definitions.get("data.snbt").getBoolean("jem_visual_v4") && !conditional.isEmpty()) {
                for (CompoundTag quest : quests.values()) {
                    ListTag originalDependencies = quest.getList("dependencies", Tag.TAG_STRING);
                    if (originalDependencies.stream().noneMatch(dependency -> conditional.contains(dependency.getAsString()))) continue;
                    ListTag dependencies = new ListTag();
                    for (Tag dependency : originalDependencies)
                        if (!conditional.contains(dependency.getAsString())) dependencies.add(dependency.copy());
                    quest.put("dependencies", dependencies);
                }
            }
            // Active children never retain an impossible dependency on a dormant objective.
            boolean changed;
            do {
                changed = false;
                for (var entry : quests.entrySet()) {
                    if (unavailable.contains(entry.getKey())) continue;
                    for (Tag dependency : entry.getValue().getList("dependencies", Tag.TAG_STRING)) {
                        if (unavailable.contains(dependency.getAsString())) {
                            changed |= unavailable.add(entry.getKey());
                            break;
                        }
                    }
                }
            } while (changed);
            Set<Long> active = new HashSet<>();
            Map<Long, Long> dormantTasks = new HashMap<>();
            for (var entry : definitions.entrySet()) {
                if (!entry.getKey().startsWith("chapters/")) continue;
                CompoundTag chapter = entry.getValue();
                ListTag visible = new ListTag();
                List<CompoundTag> blocked = new ArrayList<>();
                for (Tag raw : chapter.getList("quests", Tag.TAG_COMPOUND)) {
                    CompoundTag quest = (CompoundTag)raw;
                    if (unavailable.contains(quest.getString("id"))) {
                        for (Tag taskRaw : quest.getList("tasks", Tag.TAG_COMPOUND)) {
                            CompoundTag task = (CompoundTag)taskRaw;
                            dormantTasks.put(Long.parseUnsignedLong(task.getString("id"), 16), Math.max(1L, Math.max(task.getLong("count"), task.getLong("value"))));
                        }
                        blocked.add(dormant(quest, chapter.getBoolean("jem_claims_archive")));
                    } else {
                        visible.add(quest);
                        active.add(Long.parseUnsignedLong(quest.getString("id"), 16));
                    }
                }
                if (visible.isEmpty() && (!authoredBook || chapter.getBoolean("jem_archive"))) {
                    // Keep disabled whole chapters' children in place: an empty chapter can auto-complete natively.
                    chapter.putBoolean("always_invisible", true);
                    blocked.forEach(visible::add);
                } else dormant.addAll(blocked);
                chapter.put("quests", visible);
                if (!itemAvailable(chapter.getString("icon"))) chapter.putString("icon", "minecraft:book");
            }
            if (!dormant.isEmpty()) definitions.put("chapters/_jem_unavailable.snbt", dormantChapter(dormant));
            Map<String, byte[]> output = new TreeMap<>();
            for (var entry : definitions.entrySet()) output.put(entry.getKey(),
                    (String.join("\n", SNBT.writeLines(entry.getValue())) + "\n").getBytes(StandardCharsets.UTF_8));
            String fingerprint = fingerprint(features, output);
            Path versions = server.getWorldPath(ROOT).resolve(fingerprint);
            Files.createDirectories(versions);
            // Native FTB may legitimately save editor/schema changes into its current folder.
            // A new load gets a new projection; previous authored projections are never overwritten.
            Path destination = Files.createTempDirectory(versions, "book-");
            for (var entry : output.entrySet()) writeNew(destination.resolve(entry.getKey()), entry.getValue());
            String receipt = new GsonBuilder().setPrettyPrinting().create().toJson(Map.of(
                    "schema", 1, "fingerprint", fingerprint, "features", features,
                    "activeQuestIds", active.stream().map(id -> String.format("%016X", id)).sorted().toList(),
                    "dormantQuestIds", unavailable.stream().sorted().toList(),
                    "unavailableReasons", unavailableReasons));
            writeNew(destination.resolve("projection.json"), receipt.getBytes(StandardCharsets.UTF_8));
            CURRENT.put(server, new Projection(features, Set.copyOf(active), Map.copyOf(dormantTasks)));
            return destination;
        } catch (IOException failure) {
            throw new IllegalStateException("Cannot prepare the world's authored FTB quest projection", failure);
        }
    }

    static Map<String, CompoundTag> read(Path root) throws IOException {
        Map<String, CompoundTag> definitions = new TreeMap<>();
        try (var paths = Files.walk(root)) {
            for (Path path : paths.filter(Files::isRegularFile).filter(path -> path.toString().endsWith(".snbt")).sorted().toList()) {
                CompoundTag tag = SNBT.tryRead(path);
                if (tag == null) throw new IOException("Invalid authored quest file: " + path);
                definitions.put(root.relativize(path).toString().replace('\\', '/'), tag);
            }
        }
        return definitions;
    }

    private static boolean chapterEnabled(String chapter, Features features) {
        return switch (chapter) {
            case "create" -> features.create && itemAvailable("create:cogwheel") && itemAvailable("create:wrench")
                    && itemAvailable("create:water_wheel") && itemAvailable("create:mechanical_press");
            case "races" -> features.races;
            case "bosses_12_eyes" -> features.campaign;
            case "events" -> features.events;
            default -> true;
        };
    }

    private static boolean questEnabled(String id, Features features) {
        if (ECONOMY.contains(id)) return features.economy;
        if (id.equals("1100000000000024")) return features.planning;
        if (SETTLEMENTS.contains(id)) {
            return features.settlements && (!id.equals("1100000000000117") || features.recruits);
        }
        return true;
    }

    private static boolean referencesAvailable(CompoundTag quest) {
        if (!itemAvailable(quest.getString("icon"))) return false;
        for (Tag raw : quest.getList("tasks", Tag.TAG_COMPOUND)) {
            CompoundTag task = (CompoundTag)raw;
            if (task.contains("item", Tag.TAG_COMPOUND) && !itemAvailable(task.getCompound("item").getString("id"))) return false;
        }
        return true;
    }

    private static String authoredUnavailable(CompoundTag quest, Map<String, CompoundTag> definitions,
                                              Features features, MinecraftServer server) {
        for (Tag mod : quest.getList("jem_required_mods", Tag.TAG_STRING))
            if (!QuestSignals.loaded(mod.getAsString())) return "Не установлено требуемое содержимое: " + mod.getAsString();
        String nativeCondition = quest.getString("jem_native_condition");
        if (!nativeCondition.isEmpty() && !QuestSignals.nativeCondition(nativeCondition))
            return "Нативное условие мода сейчас недоступно / Native mod condition is currently unavailable: " + nativeCondition;
        if (!QuestSignals.loaded("jem_rewards")) return "Недоступна система получения наград";
        if (quest.getBoolean("jem_economy") && !features.economy) return "Торговая экономика отключена в этом мире";
        if (quest.getBoolean("jem_dedicated_event") && !features.events)
            return "События и Raid требуют выделенного сервера с включёнными событиями";
        for (Tag raw : quest.getList("tasks", Tag.TAG_COMPOUND)) {
            CompoundTag task = (CompoundTag)raw;
            String reason = BookRules.unavailable(Long.parseUnsignedLong(task.getString("id"), 16));
            if (!reason.isEmpty()) return reason;
            if (task.getString("type").equals("item") && !itemAvailable(itemId(task)))
                return "Не зарегистрирован нужный предмет: " + itemId(task);
            if (task.getString("type").equals("kill") && !BuiltInRegistries.ENTITY_TYPE.containsKey(new ResourceLocation(task.getString("entity"))))
                return "Не зарегистрирована цель: " + task.getString("entity");
            if (task.getString("type").equals("dimension") && server.getLevel(net.minecraft.resources.ResourceKey.create(
                    net.minecraft.core.registries.Registries.DIMENSION, new ResourceLocation(task.getString("dimension")))) == null)
                return "Измерение недоступно: " + task.getString("dimension");
        }
        for (Tag raw : quest.getList("rewards", Tag.TAG_COMPOUND)) {
            CompoundTag reward = (CompoundTag)raw;
            if (reward.contains("item") && !itemAvailable(itemId(reward))) return "Предмет награды отсутствует: " + itemId(reward);
            if (reward.getString("type").equals("choice")) {
                long tableId = reward.getLong("table_id");
                CompoundTag table = definitions.values().stream().filter(candidate -> candidate.getString("id").equals(String.format("%016X", tableId))).findFirst().orElse(null);
                if (table == null) return "Отсутствует таблица выбора награды";
                for (Tag choice : table.getList("rewards", Tag.TAG_COMPOUND))
                    if (!itemAvailable(itemId((CompoundTag)choice))) return "Вариант награды отсутствует: " + itemId((CompoundTag)choice);
            }
        }
        return "";
    }

    private static String itemId(CompoundTag row) {
        return row.contains("item", Tag.TAG_COMPOUND) ? row.getCompound("item").getString("id") : row.getString("item");
    }

    private static void pruneUnavailableRewards(CompoundTag quest) {
        ListTag retained = new ListTag();
        for (Tag raw : quest.getList("rewards", Tag.TAG_COMPOUND)) {
            CompoundTag reward = (CompoundTag)raw;
            if (reward.contains("item") && !itemAvailable(itemId(reward))) continue;
            retained.add(reward);
        }
        quest.put("rewards", retained);
    }

    private static boolean itemAvailable(String raw) {
        if (raw.isEmpty()) return true;
        ResourceLocation id = ResourceLocation.tryParse(raw);
        return id != null && BuiltInRegistries.ITEM.containsKey(id);
    }

    private static CompoundTag dormant(CompoundTag original, boolean claimsArchive) {
        CompoundTag quest = original.copy();
        quest.putString("icon", "minecraft:book");
        quest.putBoolean("optional", true);
        if (!claimsArchive) quest.remove("rewards");
        ListTag tasks = new ListTag();
        for (Tag raw : quest.getList("tasks", Tag.TAG_COMPOUND)) {
            CompoundTag task = new CompoundTag();
            task.putString("id", ((CompoundTag)raw).getString("id"));
            task.putString("type", "custom");
            task.putLong("count", Math.max(1L, Math.max(((CompoundTag)raw).getLong("count"), ((CompoundTag)raw).getLong("value"))));
            tasks.add(task);
        }
        quest.put("tasks", tasks);
        return quest;
    }

    private static CompoundTag dormantChapter(List<CompoundTag> quests) {
        CompoundTag chapter = new CompoundTag();
        chapter.putString("id", "10000000000000FE");
        chapter.putString("filename", "_jem_unavailable");
        chapter.putString("title", "Unavailable content");
        chapter.putString("icon", "minecraft:book");
        chapter.putBoolean("always_invisible", true);
        ListTag children = new ListTag();
        quests.forEach(children::add);
        chapter.put("quests", children);
        return chapter;
    }

    private static String fingerprint(Features features, Map<String, byte[]> output) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(features.toString().getBytes(StandardCharsets.UTF_8));
            output.forEach((path, bytes) -> {
                digest.update(path.getBytes(StandardCharsets.UTF_8));
                digest.update((byte)0);
                digest.update(bytes);
            });
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException failure) { throw new IllegalStateException(failure); }
    }

    private static void writeNew(Path target, byte[] bytes) throws IOException {
        Files.createDirectories(target.getParent());
        Files.write(target, bytes, StandardOpenOption.CREATE_NEW);
    }

    static boolean active(BaseQuestFile file, long quest) {
        return file instanceof ServerQuestFile serverFile && CURRENT.containsKey(serverFile.server)
                && CURRENT.get(serverFile.server).active.contains(quest);
    }

    static Long dormantMaximum(BaseQuestFile file, long task) {
        if (!(file instanceof ServerQuestFile serverFile)) return null;
        Projection projection = CURRENT.get(serverFile.server);
        return projection == null ? null : projection.dormantTasks.get(task);
    }

    static boolean featuresChanged(MinecraftServer server) {
        Projection projection = CURRENT.get(server);
        return projection != null && !projection.features.equals(features(server));
    }

    static void clear(MinecraftServer server) { CURRENT.remove(server); }
}
