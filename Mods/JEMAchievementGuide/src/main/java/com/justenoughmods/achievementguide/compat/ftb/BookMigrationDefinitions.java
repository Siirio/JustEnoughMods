package com.justenoughmods.achievementguide.compat.ftb;

import com.google.gson.JsonObject;
import dev.ftb.mods.ftblibrary.snbt.SNBT;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

/** Freezes actual pre-cutover native definitions before the new book can deserialize team history. */
final class BookMigrationDefinitions {
    private static final Map<MinecraftServer, Set<Long>> VERIFIED = new HashMap<>();
    private static final Map<MinecraftServer, Set<Long>> ARCHIVED = new HashMap<>();
    private static final Map<MinecraftServer, Map<Long, CompoundTag>> REWARDS = new HashMap<>();
    static Path root(MinecraftServer server) { return server.getWorldPath(new LevelResource("data/jem_quest_migration/visual-v4-1")); }

    static void prepare(MinecraftServer server, Map<String, CompoundTag> authored) throws IOException {
        if (!authored.get("data.snbt").getBoolean("jem_visual_v4")) { BookMigration.clear(server); return; }
        Path frozen = root(server).resolve("native-definitions");
        Map<String, CompoundTag> expected = expected();
        Map<String, CompoundTag> actual;
        if (Files.isRegularFile(frozen.resolve("data.snbt"))) actual = FtbQuestProjection.read(frozen);
        else {
            Path preceding = preceding(server);
            if (preceding == null) {
                Path teams = server.getWorldPath(dev.ftb.mods.ftbquests.quest.ServerQuestFile.FTBQUESTS_DATA);
                if (Files.isDirectory(teams)) try (var paths = Files.list(teams)) {
                    if (paths.anyMatch(path -> path.getFileName().toString().endsWith(".snbt")))
                        throw new IOException("Existing FTB history has no actual pre-upgrade native book to freeze");
                }
                actual = expected;
            } else actual = FtbQuestProjection.read(preceding);
            // The snapshot is committed only after every source file has been written.
            Path staging = root(server).resolve("native-definitions.pending");
            Files.createDirectories(staging);
            for (var entry : actual.entrySet()) {
                if (entry.getKey().equals("data.snbt")) continue;
                write(staging.resolve(entry.getKey()), entry.getValue());
            }
            write(staging.resolve("data.snbt"), actual.get("data.snbt"));
            Files.createDirectories(frozen.getParent());
            Files.move(staging, frozen);
        }
        BookMigrationRecovery.restore(server, actual, expected);
        Map<Long, CompoundTag> oldQuests = quests(actual), baseline = quests(expected);
        Map<Long, CompoundTag> oldTables = tables(actual), expectedTables = tables(expected);
        Map<Long, CompoundTag> frozenRewards = new HashMap<>();
        List<CompoundTag> rewardOwners = new ArrayList<>(oldQuests.values());
        rewardOwners.addAll(oldTables.values());
        for (CompoundTag owner : rewardOwners) for (Tag raw : owner.getList("rewards", Tag.TAG_COMPOUND)) {
            CompoundTag reward = (CompoundTag)raw;
            if (frozenRewards.putIfAbsent(id(reward.getString("id")), reward.copy()) != null)
                throw new IOException("Duplicate original native reward " + reward.getString("id"));
        }
        Set<Long> verified = new HashSet<>();
        for (var entry : oldQuests.entrySet()) {
            CompoundTag reference = baseline.get(entry.getKey());
            if (reference != null && semantic(reference).equals(semantic(entry.getValue()))
                    && sameTables(entry.getValue(), oldTables, expectedTables)) verified.add(entry.getKey());
        }
        JsonObject comparison = new JsonObject();
        comparison.addProperty("version", "visual-v4-1");
        com.google.gson.JsonArray verifiedIds = new com.google.gson.JsonArray(), unresolvedIds = new com.google.gson.JsonArray();
        verified.stream().sorted().forEach(value -> verifiedIds.add(String.format("%016X", value)));
        oldQuests.keySet().stream().filter(value -> !verified.contains(value)).sorted().forEach(value -> unresolvedIds.add(String.format("%016X", value)));
        comparison.add("verified_original_native_quests", verifiedIds);
        comparison.add("preserved_actual_quests_requiring_reconciliation", unresolvedIds);
        Files.writeString(root(server).resolve("definition-comparison.json"), new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(comparison), StandardCharsets.UTF_8);
        // Every actual original remains exactly once, including identities predating the 602-card snapshot.
        Set<Long> retained = new HashSet<>();
        for (var entry : authored.entrySet()) if (entry.getKey().startsWith("chapters/")) {
            CompoundTag chapter = entry.getValue();
            if (!chapter.getBoolean("jem_archive")) continue;
            chapter.putBoolean("jem_claims_archive", true);
            ListTag children = new ListTag();
            for (Tag raw : chapter.getList("quests", Tag.TAG_COMPOUND)) {
                long id = id(((CompoundTag) raw).getString("id"));
                if (oldQuests.containsKey(id) && retained.add(id)) children.add(oldQuests.get(id).copy());
            }
            chapter.put("quests", children);
        }
        ListTag remaining = new ListTag();
        for (var entry : oldQuests.entrySet()) if (retained.add(entry.getKey())) remaining.add(entry.getValue().copy());
        if (!remaining.isEmpty()) {
            CompoundTag chapter = new CompoundTag();
            chapter.putString("id", "10000000000000FD");
            chapter.putString("filename", "_jem_frozen_claims");
            chapter.putBoolean("jem_archive", true);
            chapter.putBoolean("jem_claims_archive", true);
            chapter.putBoolean("always_invisible", true);
            chapter.put("quests", remaining);
            authored.put("chapters/_jem_frozen_claims.snbt", chapter);
        }
        for (var entry : actual.entrySet()) if (entry.getKey().startsWith("reward_tables/")) {
            long tableId = id(entry.getValue().getString("id"));
            authored.entrySet().removeIf(candidate -> candidate.getKey().startsWith("reward_tables/")
                    && id(candidate.getValue().getString("id")) == tableId);
            authored.put("reward_tables/_frozen_" + Long.toHexString(tableId) + ".snbt", entry.getValue().copy());
        }
        Set<Long> identities = new HashSet<>();
        for (var entry : authored.entrySet()) if (entry.getKey().startsWith("chapters/") || entry.getKey().startsWith("reward_tables/")) {
            if (!identities.add(id(entry.getValue().getString("id")))) throw new IOException("Native chapter/table identity collision");
            if (entry.getKey().startsWith("reward_tables/")) for (Tag raw : entry.getValue().getList("rewards", Tag.TAG_COMPOUND))
                if (!identities.add(id(((CompoundTag)raw).getString("id")))) throw new IOException("Native choice option identity collision");
        }
        for (CompoundTag quest : quests(authored).values()) {
            if (!identities.add(id(quest.getString("id")))) throw new IOException("Duplicate native quest identity");
            for (String kind : List.of("tasks", "rewards")) for (Tag raw : quest.getList(kind, Tag.TAG_COMPOUND))
                if (!identities.add(id(((CompoundTag)raw).getString("id")))) throw new IOException("Native migration identity collision");
        }
        VERIFIED.put(server, Set.copyOf(verified));
        ARCHIVED.put(server, Set.copyOf(oldQuests.keySet()));
        REWARDS.put(server, Map.copyOf(frozenRewards));
    }

    static boolean active(MinecraftServer server) { return VERIFIED.containsKey(server); }
    static Set<Long> archived(MinecraftServer server) { return ARCHIVED.getOrDefault(server, Set.of()); }
    static boolean verified(MinecraftServer server, long quest) { return VERIFIED.getOrDefault(server, Set.of()).contains(quest); }
    static CompoundTag reward(MinecraftServer server, long reward) { return REWARDS.getOrDefault(server, Map.of()).get(reward); }
    static void clear(MinecraftServer server) { VERIFIED.remove(server); ARCHIVED.remove(server); REWARDS.remove(server); }
    static long id(String value) { return Long.parseUnsignedLong(value, 16); }

    private static Map<String, CompoundTag> expected() throws IOException {
        JsonObject manifest = BookMigration.resource("legacy/manifest.json").getAsJsonObject();
        Map<String, CompoundTag> definitions = new HashMap<>();
        for (String name : manifest.keySet()) try (var stream = BookMigrationDefinitions.class.getResourceAsStream(BookMigration.RESOURCE + "legacy/" + name)) {
            if (stream == null) throw new IOException("Missing frozen native definition " + name);
            CompoundTag tag = SNBT.readLines(new String(stream.readAllBytes(), StandardCharsets.UTF_8).lines().toList());
            if (tag == null) throw new IOException("Invalid frozen native definition " + name);
            definitions.put(name, tag);
        }
        return definitions;
    }

    private static Path preceding(MinecraftServer server) throws IOException {
        Path projections = server.getWorldPath(new LevelResource("data/jem_quest_projection"));
        if (!Files.isDirectory(projections)) return null;
        try (var paths = Files.walk(projections, 3)) {
            return paths.filter(path -> path.getFileName().toString().equals("data.snbt"))
                    .filter(path -> { try { var tag = SNBT.tryRead(path); return tag != null && !tag.getBoolean("jem_visual_v4"); }
                        catch (IOException failure) { throw new IllegalStateException(failure); } })
                    .max(Comparator.comparingLong(path -> { try { return Files.getLastModifiedTime(path).toMillis(); }
                        catch (IOException failure) { throw new IllegalStateException(failure); } })).map(Path::getParent).orElse(null);
        }
    }

    static Map<Long, CompoundTag> quests(Map<String, CompoundTag> definitions) throws IOException {
        Map<Long, CompoundTag> result = new HashMap<>();
        for (var entry : definitions.entrySet()) if (entry.getKey().startsWith("chapters/"))
            for (Tag raw : entry.getValue().getList("quests", Tag.TAG_COMPOUND)) {
                CompoundTag quest = (CompoundTag)raw;
                if (result.putIfAbsent(id(quest.getString("id")), quest) != null) throw new IOException("Duplicate original native quest " + quest.getString("id"));
            }
        return result;
    }
    static Map<Long, CompoundTag> tables(Map<String, CompoundTag> definitions) {
        Map<Long, CompoundTag> result = new HashMap<>();
        definitions.forEach((name, tag) -> { if (name.startsWith("reward_tables/")) result.put(id(tag.getString("id")), tag); });
        return result;
    }
    static boolean sameTables(CompoundTag quest, Map<Long, CompoundTag> actual, Map<Long, CompoundTag> expected) {
        for (Tag raw : quest.getList("rewards", Tag.TAG_COMPOUND)) {
            CompoundTag reward = (CompoundTag)raw;
            if (reward.getString("type").equals("choice")) {
                long id = reward.getLong("table_id");
                if (actual.get(id) == null || !actual.get(id).equals(expected.get(id))) return false;
            }
        }
        return true;
    }
    static CompoundTag semantic(CompoundTag quest) {
        CompoundTag semantic = quest.copy();
        for (String visual : List.of("x", "y", "shape", "size", "title", "subtitle", "description", "icon", "tags")) semantic.remove(visual);
        return semantic;
    }
    static void write(Path path, CompoundTag value) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, String.join("\n", SNBT.writeLines(value)) + "\n", StandardCharsets.UTF_8);
    }
}
