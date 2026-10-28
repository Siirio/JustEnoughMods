package com.justenoughmods.achievementguide.compat.ftb;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.ftb.mods.ftblibrary.snbt.SNBT;
import dev.ftb.mods.ftbquests.quest.ServerQuestFile;
import dev.ftb.mods.ftbquests.util.QuestKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

/** Recovers only actual retained native definitions that the known dormant transform stripped. */
final class BookMigrationRecovery {
    private BookMigrationRecovery() {}

    static void restore(MinecraftServer server, Map<String, CompoundTag> actual, Map<String, CompoundTag> expected) throws IOException {
        Map<Long, CompoundTag> baseline = BookMigrationDefinitions.quests(expected);
        Map<Long, CompoundTag> latest = BookMigrationDefinitions.quests(actual);
        Map<Long, CompoundTag> baselineTables = BookMigrationDefinitions.tables(expected);
        Set<Long> history = nativeHistory(server);
        Set<Long> pending = new LinkedHashSet<>();
        for (var entry : baseline.entrySet()) {
            CompoundTag current = latest.get(entry.getKey());
            if (entry.getValue().getList("rewards", Tag.TAG_COMPOUND).isEmpty()) continue;
            if (current != null && stripped(current, entry.getValue())) pending.add(entry.getKey());
            else if (current == null && referenced(entry.getKey(), entry.getValue(), history)) pending.add(entry.getKey());
        }
        Path root = BookMigrationDefinitions.root(server).resolve("recovered-native-history");
        Files.createDirectories(root);
        Path reportPath = root.resolve("recovery.json");
        JsonObject recovered;
        if (Files.isRegularFile(reportPath)) try (var reader = Files.newBufferedReader(reportPath)) {
            recovered = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("recovered");
        } else recovered = new JsonObject();
        // Replay the already frozen native evidence before consulting mutable older projection folders.
        for (long id : List.copyOf(pending)) if (recovered.has(hex(id))) {
            JsonObject row = recovered.getAsJsonObject(hex(id));
            Path source = root.resolve("quests/" + hex(id) + ".snbt");
            if (!Files.isRegularFile(source) || !sha(source).equals(row.get("frozen_quest_sha256").getAsString()))
                throw new IOException("Recovered native quest evidence changed: " + hex(id));
            CompoundTag quest = SNBT.tryRead(source);
            Map<Long, CompoundTag> tables = new HashMap<>();
            for (var raw : row.getAsJsonArray("tables")) {
                JsonObject tableRow = raw.getAsJsonObject();
                Path tablePath = root.resolve("tables/" + tableRow.get("id").getAsString() + ".snbt");
                if (!Files.isRegularFile(tablePath) || !sha(tablePath).equals(tableRow.get("sha256").getAsString()))
                    throw new IOException("Recovered native choice evidence changed");
                tables.put(BookMigrationDefinitions.id(tableRow.get("id").getAsString()), SNBT.tryRead(tablePath));
            }
            if (!compatible(quest, baseline.get(id), tables, baselineTables, actual)) throw new IOException("Recovered original no longer matches its frozen contract");
            install(actual, latest, quest, tables);
            pending.remove(id);
        }
        if (!pending.isEmpty()) for (Path projection : projections(server)) {
            Map<String, CompoundTag> nativeFiles = FtbQuestProjection.read(projection);
            Map<Long, CompoundTag> candidates = BookMigrationDefinitions.quests(nativeFiles);
            Map<Long, CompoundTag> tables = BookMigrationDefinitions.tables(nativeFiles);
            for (long id : List.copyOf(pending)) {
                CompoundTag quest = candidates.get(id);
                if (!compatible(quest, baseline.get(id), tables, baselineTables, actual)) continue;
                CompoundTag current = latest.get(id);
                if (current != null && !stripped(current, quest)) continue;
                if (current == null && !referenced(id, quest, history)) continue;
                Path source = nativeFiles.entrySet().stream().filter(entry -> entry.getKey().startsWith("chapters/")
                        && contains(entry.getValue(), id)).map(entry -> projection.resolve(entry.getKey())).findFirst().orElseThrow();
                Path frozen = root.resolve("quests/" + hex(id) + ".snbt");
                BookMigrationDefinitions.write(frozen, quest);
                JsonObject row = new JsonObject();
                row.addProperty("native_projection", projection.toString());
                row.addProperty("native_chapter_path", source.toString());
                row.addProperty("native_chapter_sha256", sha(source));
                row.addProperty("frozen_quest_sha256", sha(frozen));
                row.addProperty("reason", current == null ? "actual native history references a retired object, exact earlier native definition recovered"
                        : "latest native object exactly matches the known dormant reward/task stripping transform");
                JsonArray tableRows = new JsonArray();
                Map<Long, CompoundTag> required = requiredTables(quest, tables);
                for (var table : required.entrySet()) {
                    Path tablePath = root.resolve("tables/" + hex(table.getKey()) + ".snbt");
                    BookMigrationDefinitions.write(tablePath, table.getValue());
                    JsonObject tableRow = new JsonObject();
                    tableRow.addProperty("id", hex(table.getKey()));
                    tableRow.addProperty("sha256", sha(tablePath));
                    tableRows.add(tableRow);
                }
                row.add("tables", tableRows);
                recovered.add(hex(id), row);
                install(actual, latest, quest, required);
                pending.remove(id);
            }
            if (pending.isEmpty()) break;
        }
        JsonObject report = new JsonObject();
        report.addProperty("version", "visual-v4-1");
        report.addProperty("policy", "Exact earlier native definitions only. Newest native snapshot remains unchanged. No source-derived reward reconstruction.");
        report.add("recovered", recovered);
        JsonArray unresolved = new JsonArray();
        for (long id : pending) {
            JsonObject row = new JsonObject();
            row.addProperty("quest", hex(id));
            row.addProperty("reason", "No retained earlier native definition with identical predicate, task/reward IDs, claim sources and choice tables was found");
            unresolved.add(row);
        }
        for (var entry : baseline.entrySet()) if (!latest.containsKey(entry.getKey()) && !pending.contains(entry.getKey())) {
            JsonObject row = new JsonObject();
            row.addProperty("quest", hex(entry.getKey()));
            row.addProperty("reason", "No actual current native object or native team-history reference. Source baseline alone does not establish a world entitlement");
            unresolved.add(row);
        }
        for (var entry : baseline.entrySet()) {
            CompoundTag current = latest.get(entry.getKey());
            if (current == null || pending.contains(entry.getKey()) || current.getList("rewards", Tag.TAG_COMPOUND).size() > 0
                    || entry.getValue().getList("rewards", Tag.TAG_COMPOUND).isEmpty()) continue;
            JsonObject row = new JsonObject();
            row.addProperty("quest", hex(entry.getKey()));
            row.addProperty("reason", "Current native object has no rewards but its predicate/options do not match the known dormant stripping transform. Exact reconciliation is required");
            row.addProperty("actual_native_definition", String.join("\n", SNBT.writeLines(current)));
            unresolved.add(row);
        }
        report.add("unresolved", unresolved);
        Path staging = reportPath.resolveSibling("recovery.pending.json");
        Files.writeString(staging, new GsonBuilder().setPrettyPrinting().create().toJson(report), StandardCharsets.UTF_8);
        Files.move(staging, reportPath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    }

    private static boolean compatible(CompoundTag candidate, CompoundTag baseline, Map<Long, CompoundTag> tables,
                                      Map<Long, CompoundTag> baselineTables, Map<String, CompoundTag> actual) {
        if (candidate == null || baseline == null || !BookMigrationDefinitions.semantic(candidate).equals(BookMigrationDefinitions.semantic(baseline))
                || !BookMigrationDefinitions.sameTables(candidate, tables, baselineTables)) return false;
        Map<Long, CompoundTag> currentTables = BookMigrationDefinitions.tables(actual);
        for (var table : requiredTables(candidate, tables).entrySet())
            if (currentTables.containsKey(table.getKey()) && !currentTables.get(table.getKey()).equals(table.getValue())) return false;
        return true;
    }
    private static Map<Long, CompoundTag> requiredTables(CompoundTag quest, Map<Long, CompoundTag> tables) {
        Map<Long, CompoundTag> result = new HashMap<>();
        for (Tag raw : quest.getList("rewards", Tag.TAG_COMPOUND)) {
            CompoundTag reward = (CompoundTag)raw;
            if (reward.getString("type").equals("choice")) result.put(reward.getLong("table_id"), tables.get(reward.getLong("table_id")));
        }
        return result;
    }
    private static boolean stripped(CompoundTag actual, CompoundTag original) {
        if (!actual.getList("rewards", Tag.TAG_COMPOUND).isEmpty() || original.getList("rewards", Tag.TAG_COMPOUND).isEmpty()) return false;
        for (boolean maxima : List.of(false, true)) {
            CompoundTag dormant = original.copy();
            dormant.putString("icon", "minecraft:book");
            dormant.putBoolean("optional", true);
            dormant.remove("rewards");
            ListTag tasks = new ListTag();
            for (Tag raw : original.getList("tasks", Tag.TAG_COMPOUND)) {
                CompoundTag task = new CompoundTag(), before = (CompoundTag)raw;
                task.putString("id", before.getString("id"));
                task.putString("type", "custom");
                if (maxima) task.putLong("count", Math.max(1L, Math.max(before.getLong("count"), before.getLong("value"))));
                tasks.add(task);
            }
            dormant.put("tasks", tasks);
            if (BookMigrationDefinitions.semantic(dormant).equals(BookMigrationDefinitions.semantic(actual))) return true;
        }
        return false;
    }
    private static void install(Map<String, CompoundTag> actual, Map<Long, CompoundTag> latest, CompoundTag quest, Map<Long, CompoundTag> tables) {
        long id = BookMigrationDefinitions.id(quest.getString("id"));
        boolean replaced = false;
        for (var entry : actual.entrySet()) if (entry.getKey().startsWith("chapters/")) {
            ListTag children = entry.getValue().getList("quests", Tag.TAG_COMPOUND);
            for (int index = 0; index < children.size(); index++) if (BookMigrationDefinitions.id(children.getCompound(index).getString("id")) == id) {
                children.set(index, quest.copy()); replaced = true;
            }
        }
        if (!replaced) actual.entrySet().stream().filter(entry -> entry.getKey().startsWith("chapters/")).findFirst().orElseThrow()
                .getValue().getList("quests", Tag.TAG_COMPOUND).add(quest.copy());
        for (var table : tables.entrySet()) if (!BookMigrationDefinitions.tables(actual).containsKey(table.getKey()))
            actual.put("reward_tables/_recovered_" + hex(table.getKey()) + ".snbt", table.getValue().copy());
        latest.put(id, quest);
    }
    private static List<Path> projections(MinecraftServer server) throws IOException {
        Path root = server.getWorldPath(new LevelResource("data/jem_quest_projection"));
        if (!Files.isDirectory(root)) return List.of();
        try (var paths = Files.walk(root, 3)) {
            return paths.filter(path -> path.getFileName().toString().equals("data.snbt"))
                    .filter(path -> { try { var data = SNBT.tryRead(path); return data != null && !data.getBoolean("jem_visual_v4"); }
                        catch (IOException failure) { throw new IllegalStateException(failure); } })
                    .sorted(Comparator.<Path>comparingLong(path -> { try { return Files.getLastModifiedTime(path).toMillis(); }
                        catch (IOException failure) { throw new IllegalStateException(failure); } }).reversed().thenComparing(Path::toString))
                    .map(Path::getParent).toList();
        }
    }
    private static Set<Long> nativeHistory(MinecraftServer server) throws IOException {
        Set<Long> ids = new HashSet<>();
        Path root = server.getWorldPath(ServerQuestFile.FTBQUESTS_DATA);
        if (!Files.isDirectory(root)) return ids;
        try (var paths = Files.list(root)) {
            for (Path path : paths.filter(path -> path.getFileName().toString().contains("-") && path.toString().endsWith(".snbt")).toList()) {
                CompoundTag team = SNBT.tryRead(path);
                if (team == null) throw new IOException("Unreadable native team history " + path);
                for (String kind : List.of("completed", "started", "task_progress")) for (String key : team.getCompound(kind).getAllKeys())
                    if (team.getCompound(kind).getLong(key) > 0) ids.add(BookMigrationDefinitions.id(key));
                for (String key : team.getCompound("claimed_rewards").getAllKeys()) ids.add(QuestKey.fromString(key).id());
            }
        }
        return ids;
    }
    private static boolean referenced(long id, CompoundTag quest, Set<Long> history) {
        if (history.contains(id)) return true;
        for (String kind : List.of("tasks", "rewards")) for (Tag raw : quest.getList(kind, Tag.TAG_COMPOUND))
            if (history.contains(BookMigrationDefinitions.id(((CompoundTag)raw).getString("id")))) return true;
        return false;
    }
    private static boolean contains(CompoundTag chapter, long id) {
        for (Tag raw : chapter.getList("quests", Tag.TAG_COMPOUND)) if (BookMigrationDefinitions.id(((CompoundTag)raw).getString("id")) == id) return true;
        return false;
    }
    private static String sha(Path path) throws IOException {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path))); }
        catch (NoSuchAlgorithmException failure) { throw new IllegalStateException(failure); }
    }
    private static String hex(long id) { return String.format("%016X", id); }
}
