package com.justenoughmods.achievementguide.compat.ftb;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.ftb.mods.ftblibrary.snbt.SNBT;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.ServerQuestFile;
import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftbquests.quest.reward.ChoiceReward;
import dev.ftb.mods.ftbquests.quest.reward.Reward;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MobBucketItem;
import net.minecraft.world.item.SpawnEggItem;

/** Versioned eligibility references to native history. FTB owns all task progress and claim bits. */
public final class BookMigration {
    static final String RESOURCE = "/assets/jem_advancements/questbook/migration/";
    private static final Map<MinecraftServer, BookMigration> CURRENT = new HashMap<>();
    private static final Set<MinecraftServer> CURRENT_BOOKS = new HashSet<>();
    private final ServerQuestFile file;
    private final JsonArray mappings = resource("migration-map.json").getAsJsonArray();
    private final Map<Long, JsonObject> decisions = new HashMap<>();
    private final Map<Long, JsonObject> exact = new HashMap<>();
    private final JsonObject state;
    private final Path path;
    private final String contract;
    private boolean ready;
    private boolean retry;

    private BookMigration(ServerQuestFile file) throws IOException {
        this.file = file;
        path = BookMigrationDefinitions.root(file.server).resolve("checkpoint.json");
        for (JsonElement raw : resource("all-old-rewards.json").getAsJsonArray()) {
            JsonObject row = raw.getAsJsonObject();
            if (row.get("emitted_native").getAsBoolean()) decisions.put(id(row, "old_native_reward_id"), row);
        }
        for (JsonElement raw : mappings) {
            JsonObject mapping = raw.getAsJsonObject();
            if (!"COMPILED_MATCH".equals(text(mapping, "native_identity_status"))) throw new IOException("Unverified active native migration identity");
            if ("EXACT".equals(text(mapping, "equivalence"))) exact.put(id(mapping, "ftb_id"), mapping);
        }
        try {
            contract = java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
                    .digest((mappings.toString() + resource("all-old-rewards.json")).getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException failure) { throw new IllegalStateException(failure); }
        if (Files.isRegularFile(path)) {
            try (var reader = Files.newBufferedReader(path)) { state = JsonParser.parseReader(reader).getAsJsonObject(); }
            if (state.get("schema").getAsInt() != 1 || !text(state, "version").equals("visual-v4-1") || !text(state, "contract").equals(contract))
                throw new IOException("Native migration contract changed without a new version");
        } else {
            state = new JsonObject();
            state.addProperty("schema", 1);
            state.addProperty("version", "visual-v4-1");
            state.addProperty("contract", contract);
            state.add("origins", new JsonObject());
            state.add("applied", new JsonObject());
            freeze();
            persist();
        }
    }

    /** Called only after native definitions and every native TeamData file have loaded. */
    public static void loaded(ServerQuestFile file) {
        if (!BookMigrationDefinitions.active(file.server)) return;
        if (!file.server.isSameThread()) throw new IllegalStateException("Quest migration requires the server thread");
        CURRENT.remove(file.server);
        try {
            BookMigration migration = new BookMigration(file);
            CURRENT.put(file.server, migration);
            migration.restoreOrigins();
            migration.apply();
            migration.ready = true;
        } catch (IOException failure) { throw new IllegalStateException("Cannot freeze/reconcile original quest entitlements", failure); }
    }

    private void freeze() throws IOException {
        // Freeze every team before applying even the first exact mapping. Offline members come from native Teams.
        for (TeamData team : List.copyOf(file.getAllTeamData())) {
            CompoundTag nativeState = team.serializeNBT();
            BookMigrationDefinitions.write(BookMigrationDefinitions.root(file.server).resolve("native-teams/" + team.getTeamId() + ".snbt"), nativeState);
            Set<UUID> subjects = new HashSet<>();
            FTBTeamsAPI.api().getManager().getTeamByID(team.getTeamId()).ifPresent(nativeTeam -> subjects.addAll(nativeTeam.getMembers()));
            // Native per-player UI data and merged claim keys survive team changes.
            // They prove neither present source membership nor an unclaimed entitlement.
            JsonObject origin = new JsonObject();
            JsonArray players = new JsonArray();
            subjects.stream().map(UUID::toString).sorted().forEach(players::add);
            origin.add("subjects", players);
            JsonObject completed = new JsonObject();
            for (long questId : BookMigrationDefinitions.archived(file.server)) {
                Quest quest = file.getQuest(questId);
                if (quest != null && team.isCompleted(quest)) completed.addProperty(hex(questId), true);
            }
            origin.add("completed", completed);
            state.getAsJsonObject("origins").add(team.getTeamId().toString(), origin);
        }
    }

    private void restoreOrigins() throws IOException {
        for (String key : state.getAsJsonObject("origins").keySet()) {
            UUID id = UUID.fromString(key);
            if (file.getNullableTeamData(id) != null) continue;
            var saved = SNBT.tryRead(BookMigrationDefinitions.root(file.server).resolve("native-teams/" + id + ".snbt"));
            if (saved == null) throw new IOException("Missing preserved original native TeamData " + key);
            TeamData team = new TeamData(id, file);
            team.deserializeNBT(saved);
            file.addData(team, true);
            team.markDirty();
        }
    }

    private void apply() throws IOException {
        retry = false;
        for (var originEntry : state.getAsJsonObject("origins").entrySet()) {
            TeamData team = file.getNullableTeamData(UUID.fromString(originEntry.getKey()));
            if (team == null) throw new IOException("Original native TeamData is unavailable");
            for (JsonObject mapping : exact.values()) {
                JsonArray predecessors = mapping.getAsJsonArray("exact_originals");
                if (predecessors.size() != 1) throw new IOException("Only one explicitly exact original goal may migrate");
                long oldQuest = id(predecessors.get(0).getAsJsonObject(), "old_ftb_id");
                if (!BookMigrationDefinitions.verified(file.server, oldQuest)) continue;
                String checkpoint = originEntry.getKey() + "/" + text(mapping, "ftb_id");
                if (state.getAsJsonObject("applied").has(checkpoint)) continue;
                if (team.isLocked()) { retry = true; continue; }
                boolean completed = originEntry.getValue().getAsJsonObject().getAsJsonObject("completed").has(hex(oldQuest));
                Quest target = file.getQuest(id(mapping, "ftb_id"));
                if (target == null) throw new IOException("Missing exact successor " + text(mapping, "ftb_id"));
                // Validate the complete transfer before touching any task.
                for (JsonElement raw : mapping.getAsJsonArray("task_map")) {
                    JsonObject row = raw.getAsJsonObject();
                    var oldTask = file.getTask(id(row, "old"));
                    var newTask = file.getTask(id(row, "new"));
                    if (oldTask == null || newTask == null || oldTask.getMaxProgress() != row.get("old_maximum").getAsLong()
                            || newTask.getMaxProgress() != row.get("new_maximum").getAsLong())
                        throw new IOException("Exact native task maximum changed");
                }
                for (JsonElement raw : mapping.getAsJsonArray("task_map")) {
                    JsonObject row = raw.getAsJsonObject();
                    var newTask = file.getTask(id(row, "new"));
                    long oldProgress = team.getProgress(file.getTask(id(row, "old")));
                    team.setProgress(newTask, Math.max(team.getProgress(newTask), Math.min(newTask.getMaxProgress(), oldProgress)));
                }
                if (completed) {
                    Date time = team.getCompletedTime(oldQuest).orElse(new Date());
                    team.setCompleted(target.id, time);
                    // Settle only subjects of this source history. Never invoke a delivery callback.
                    boolean settled = true;
                    for (JsonElement subject : originEntry.getValue().getAsJsonObject().getAsJsonArray("subjects"))
                        settled &= settle(team, UUID.fromString(subject.getAsString()), mapping);
                    if (!settled) { retry = true; file.saveNow(); continue; }
                }
                file.saveNow();
                state.getAsJsonObject("applied").addProperty(checkpoint, true);
                persist();
            }
        }
    }

    private boolean settle(TeamData team, UUID player, JsonObject mapping) {
        boolean settled = true;
        for (JsonElement raw : mapping.getAsJsonArray("new_reward_parent_ids")) {
            Reward reward = file.getReward(BookMigrationDefinitions.id(raw.getAsString()));
            if (reward == null || !team.isRewardClaimed(player, reward)
                    && !team.markRewardAsClaimed(player, reward, System.currentTimeMillis())) settled = false;
        }
        return settled;
    }

    private boolean eligible(UUID player, UUID team, long quest) {
        JsonObject origin = state.getAsJsonObject("origins").getAsJsonObject(team.toString());
        if (origin == null || !origin.getAsJsonObject("completed").has(hex(quest))) return false;
        for (JsonElement subject : origin.getAsJsonArray("subjects")) if (subject.getAsString().equals(player.toString())) return true;
        return false;
    }

    private boolean legacyGoal(ServerPlayer player, JsonObject mapping) {
        long oldQuest = id(mapping.getAsJsonArray("exact_originals").get(0).getAsJsonObject(), "old_ftb_id");
        for (String origin : state.getAsJsonObject("origins").keySet())
            if (eligible(player.getUUID(), UUID.fromString(origin), oldQuest)) return true;
        // A permanent receipt proves the original whole exact goal even after a team move.
        for (JsonObject decision : decisions.values()) if (id(decision, "old_ftb_id") == oldQuest && paid(player, decision)) return true;
        return false;
    }

    /** False cancels the native claim before its paid bit and before any delivery. */
    public static boolean allow(TeamData team, ServerPlayer player, Reward parent) {
        BookMigration migration = CURRENT.get(player.server);
        if (migration == null) return !BookMigrationDefinitions.active(player.server)
                && (!managed(player.server) || currentRewardAllowed(team, player, parent));
        if (!migration.ready || migration.file.isLoading()) return false;
        if (BookMigrationDefinitions.archived(player.server).contains(parent.getQuest().id)) return false;
        JsonObject mapping = migration.exact.get(parent.getQuest().id);
        if (mapping != null && migration.legacyGoal(player, mapping)) {
            long original = id(mapping.getAsJsonArray("exact_originals").get(0).getAsJsonObject(), "old_ftb_id");
            if (!BookMigrationDefinitions.verified(player.server, original)) return false;
            migration.settle(team, player.getUUID(), mapping);
            migration.file.saveNow();
            return false;
        }
        return currentRewardAllowed(team, player, parent);
    }

    private static boolean currentRewardAllowed(TeamData team, ServerPlayer player, Reward parent) {
        return !team.getFile().isLoading() && team.isCompleted(parent.getQuest()) && !team.isLocked()
                && !team.isRewardBlocked(parent) && !team.isRewardClaimed(player.getUUID(), parent);
    }

    /** Covers fixed native claims without changing native ordering or the reward instance. */
    public static void invoke(Reward reward, ServerPlayer player, boolean notify, TeamData source) {
        if (reward instanceof BookItemReward item) item.claimFrom(player, source, reward);
        else if (reward instanceof BookXpReward xp) xp.claimFrom(player, source, reward, notify);
        else reward.claim(player, notify);
    }

    /** Exact native choice packet interception: upstream calls the option before checking its parent. */
    public static boolean choice(ServerPlayer player, long rewardId, int optionIndex) {
        if (!managed(player.server)) return false;
        ServerQuestFile file = ServerQuestFile.INSTANCE;
        Reward raw = file.getReward(rewardId);
        if (!(raw instanceof ChoiceReward parent)) return true;
        TeamData team = file.getOrCreateTeamData(player);
        if (!allow(team, player, parent) || parent.getTable() == null || optionIndex < 0
                || optionIndex >= parent.getTable().getWeightedRewards().size()) return true;
        Reward option = parent.getTable().getWeightedRewards().get(optionIndex).getReward();
        if (!(option instanceof BookItemReward item) || item.parentId() != parent.id) return true;
        if (!team.markRewardAsClaimed(player.getUUID(), parent, System.currentTimeMillis())) return true;
        item.claimFrom(player, team, parent);
        file.saveNow();
        return true;
    }

    static List<ItemStack> delivery(ServerPlayer player, BookItemReward reward) {
        BookMigration migration = CURRENT.get(player.server);
        JsonObject decision = migration == null ? null : migration.decisions.get(reward.id);
        if (migration == null || !BookMigrationDefinitions.archived(player.server).contains(reward.getQuest().id)
                && decision == null) return List.of(reward.getItem().copyWithCount(reward.getCount()));
        if (decision == null || !migration.matches(reward, decision)) throw new IllegalStateException("Historical reward awaits compatibility reconciliation");
        List<ItemStack> items = new ArrayList<>();
        JsonObject delivery = decision.getAsJsonObject("legacy_delivery");
        switch (text(delivery, "status")) {
            case "allowed_frozen_original" -> {
                if (!text(decision, "old_claim_source").equals(reward.source())
                        || !text(decision, "item").equals(BuiltInRegistries.ITEM.getKey(reward.getItem().getItem()).toString())
                        || decision.get("count").getAsInt() != reward.getCount())
                    throw new IllegalStateException("Frozen reward differs from audited original");
                items.add(reward.getItem().copyWithCount(reward.getCount()));
            }
            case "exact_allowed_compensation" -> {
                if (!text(decision, "old_claim_source").equals(reward.source())) throw new IllegalStateException("Compensation source changed");
                for (JsonElement raw : delivery.getAsJsonArray("replacement_stacks")) {
                    JsonObject row = raw.getAsJsonObject();
                    ResourceLocation id = new ResourceLocation(text(row, "id"));
                    if (!BuiltInRegistries.ITEM.containsKey(id)) throw new IllegalStateException("Unavailable compensation");
                    items.add(new ItemStack(BuiltInRegistries.ITEM.get(id), row.get("count").getAsInt()));
                }
            }
            default -> throw new IllegalStateException("Historical reward is unavailable and remains unclaimed");
        }
        for (ItemStack stack : items) compatible(stack);
        return items;
    }

    private static void compatible(ItemStack stack) {
        if (stack.isEmpty() || stack.getCount() < 1 || stack.getItem() instanceof SpawnEggItem || stack.getItem() instanceof MobBucketItem)
            throw new IllegalStateException("Historical entity creation reward is unavailable");
        if (QuestSignals.loaded("item_obliterator") && Boolean.TRUE.equals(BookSignals.call("elocindev.item_obliterator.forge.utils.Utils", "isDisabled", new Class<?>[]{ItemStack.class}, stack)))
            throw new IllegalStateException("Historical reward is disabled by current item compatibility");
        if (QuestSignals.loaded("jem_world_boss_tiers") && Boolean.TRUE.equals(BookSignals.call("com.siirio.jemworldbosstiers.protection.WeaponProtection", "bossWeapon", new Class<?>[]{ItemStack.class}, stack)))
            throw new IllegalStateException("Earned boss weapons cannot be issued by historical quests");
        if (QuestSignals.loaded("jem_twelve_eyes")) {
            JsonArray bosses = (JsonArray) BookSignals.call("com.siirio.jemtwelveeyes.CampaignDefinition", "entries", new Class<?>[]{String.class}, "bosses");
            String item = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
            for (JsonElement boss : bosses) if (item.equals(text(boss.getAsJsonObject(), "eye")))
                throw new IllegalStateException("Campaign eyes cannot be issued by historical quests");
        }
    }

    private static boolean paid(ServerPlayer player, JsonObject decision) {
        Reward parent = ServerQuestFile.INSTANCE.getReward(id(decision, "old_claim_parent_id"));
        if (parent != null) for (TeamData team : ServerQuestFile.INSTANCE.getAllTeamData())
            if (team.isRewardClaimed(player.getUUID(), parent)) return true;
        return receipt(player, decision);
    }

    private boolean matches(Reward reward, JsonObject decision) {
        CompoundTag frozen = BookMigrationDefinitions.reward(file.server, reward.id);
        if (frozen == null) return false;
        if (reward instanceof BookXpReward) {
            CompoundTag current = new CompoundTag();
            reward.writeData(current);
            return frozen.getString("type").equals("jem_advancements:quest_xp")
                    && frozen.getInt("xp") == decision.get("count").getAsInt() && current.getInt("xp") == frozen.getInt("xp")
                    && "allowed_frozen_original".equals(text(decision.getAsJsonObject("legacy_delivery"), "status"));
        }
        if (!(reward instanceof BookItemReward item)) return false;
        String original = frozen.contains("item", net.minecraft.nbt.Tag.TAG_COMPOUND) ? frozen.getCompound("item").getString("id") : frozen.getString("item");
        return original.equals(text(decision, "item")) && frozen.getInt("count") == decision.get("count").getAsInt()
                && ("ftb_quest:" + frozen.getString("claim_key")).equals(text(decision, "old_claim_source"))
                && item.source().equals(text(decision, "old_claim_source"))
                && BookMigrationDefinitions.id(frozen.getString("claim_reward_id")) == id(decision, "old_claim_parent_id");
    }

    private static boolean receipt(ServerPlayer player, JsonObject decision) {
        return "xp".equals(text(decision, "kind")) ? BookXpReward.issued(player, id(decision, "old_native_reward_id"))
                : BookItemReward.issued(player, text(decision, "old_claim_source"));
    }

    public record Claim(UUID origin, long quest, long parent, long option, boolean available, boolean compensation) {}

    public static List<Claim> pending(ServerPlayer player, int offset) {
        BookMigration migration = CURRENT.get(player.server);
        if (migration == null || !migration.ready || offset < 0 || offset > 100000) return List.of();
        Map<Long, List<Claim>> parents = new LinkedHashMap<>();
        for (String originId : migration.state.getAsJsonObject("origins").keySet().stream().sorted().toList()) {
            UUID origin = UUID.fromString(originId);
            JsonObject metadata = migration.state.getAsJsonObject("origins").getAsJsonObject(originId);
            boolean subject = false;
            for (JsonElement raw : metadata.getAsJsonArray("subjects")) if (raw.getAsString().equals(player.getUUID().toString())) { subject = true; break; }
            if (!subject) continue;
            TeamData team = migration.file.getNullableTeamData(origin);
            if (team == null) continue;
            for (long questId : metadata.getAsJsonObject("completed").keySet().stream().map(BookMigrationDefinitions::id).sorted().toList()) {
                Quest quest = migration.file.getQuest(questId);
                if (quest == null || !migration.eligible(player.getUUID(), origin, questId) || !team.isCompleted(quest)) continue;
                for (Reward parent : quest.getRewards()) {
                    List<Claim> previous = parents.get(parent.id);
                    if (previous != null && previous.stream().anyMatch(Claim::available)
                            || team.isRewardClaimed(player.getUUID(), parent)) continue;
                    List<Reward> options = parent instanceof ChoiceReward choice && choice.getTable() != null
                            ? choice.getTable().getWeightedRewards().stream().map(weight -> weight.getReward()).toList() : List.of(parent);
                    boolean issued = options.stream().map(option -> migration.decisions.get(option.id)).filter(java.util.Objects::nonNull).anyMatch(decision -> paid(player, decision));
                    if (issued) continue;
                    List<Claim> candidates = new ArrayList<>();
                    for (Reward option : options) {
                        JsonObject decision = migration.decisions.get(option.id);
                        boolean available = !team.isLocked() && !team.isRewardBlocked(parent) && decision != null && migration.matches(option, decision);
                        if (available && option instanceof BookItemReward item) try { delivery(player, item); } catch (RuntimeException unavailable) { available = false; }
                        else if (!(option instanceof BookXpReward)) available = false;
                        candidates.add(new Claim(origin, questId, parent.id, option == parent ? 0 : option.id, available,
                                decision != null && "exact_allowed_compensation".equals(text(decision.getAsJsonObject("legacy_delivery"), "status"))));
                    }
                    // A locked earlier source must not hide the same earned parent in an eligible unlocked source.
                    if (previous == null || candidates.stream().anyMatch(Claim::available)) parents.put(parent.id, candidates);
                }
            }
        }
        return parents.values().stream().flatMap(List::stream).skip(offset).limit(64).toList();
    }

    /** Bounded identifiers are proposals, all original membership/eligibility is revalidated here. */
    public static boolean claim(ServerPlayer player, UUID originId, long questId, long parentId, long optionId) {
        BookMigration migration = CURRENT.get(player.server);
        if (migration == null || !migration.ready || !player.server.isSameThread() || !player.isAlive()
                || !migration.eligible(player.getUUID(), originId, questId)) return false;
        TeamData team = migration.file.getNullableTeamData(originId);
        Quest quest = migration.file.getQuest(questId);
        Reward parent = migration.file.getReward(parentId);
        if (team == null || quest == null || parent == null || parent.getQuest() != quest || !quest.getRewards().contains(parent)
                || !team.isCompleted(quest) || team.isLocked() || team.isRewardBlocked(parent)
                || team.isRewardClaimed(player.getUUID(), parent)) return false;
        Reward option = parent;
        if (parent instanceof ChoiceReward choice) {
            if (choice.getTable() == null) return false;
            option = choice.getTable().getWeightedRewards().stream().map(weight -> weight.getReward()).filter(reward -> reward.id == optionId).findFirst().orElse(null);
        } else if (optionId != 0) return false;
        if (option == null) return false;
        JsonObject decision = migration.decisions.get(option.id);
        if (decision == null || !migration.matches(option, decision) || id(decision, "old_ftb_id") != questId || id(decision, "old_claim_parent_id") != parentId) return false;
        if (paid(player, decision)) return false;
        if (option instanceof BookItemReward item) {
            try { delivery(player, item); } catch (RuntimeException unavailable) { return false; }
            if (item.parentId() != parentId) return false;
        } else if (!(option instanceof BookXpReward)) return false;
        if (!team.markRewardAsClaimed(player.getUUID(), parent, System.currentTimeMillis())) return false;
        try {
            if (option instanceof BookItemReward item) item.claimFrom(player, team, parent);
            else if (option instanceof BookXpReward xp) xp.claimFrom(player, team, parent, true);
            else option.claim(player, true);
            migration.file.saveNow();
            return true;
        } catch (RuntimeException failure) {
            if (!receipt(player, decision)) team.resetReward(player.getUUID(), parent);
            migration.file.saveNow();
            throw failure;
        }
    }

    static void clear(MinecraftServer server) { CURRENT.remove(server); CURRENT_BOOKS.remove(server); BookMigrationDefinitions.clear(server); }
    static void currentBook(MinecraftServer server, boolean active) {
        if (active) CURRENT_BOOKS.add(server);
        else CURRENT_BOOKS.remove(server);
    }
    static boolean managed(MinecraftServer server) { return CURRENT_BOOKS.contains(server) || BookMigrationDefinitions.active(server); }
    static void refresh(MinecraftServer server) {
        BookMigration migration = CURRENT.get(server);
        if (migration == null || !migration.ready || !migration.retry || server.getTickCount() % 100 != 0) return;
        try { migration.apply(); } catch (IOException failure) { migration.ready = false; throw new IllegalStateException("Native quest reconciliation failed", failure); }
    }
    private void persist() throws IOException {
        Files.createDirectories(path.getParent());
        Path pending = path.resolveSibling("checkpoint.pending.json");
        Files.writeString(pending, new GsonBuilder().setPrettyPrinting().create().toJson(state), StandardCharsets.UTF_8);
        Files.move(pending, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    }
    static JsonElement resource(String name) {
        try (var stream = BookMigration.class.getResourceAsStream(RESOURCE + name)) {
            if (stream == null) throw new IllegalStateException("Missing quest migration resource " + name);
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
        } catch (IOException failure) { throw new IllegalStateException("Cannot read quest migration resource " + name, failure); }
    }
    private static String text(JsonObject row, String key) { return row.has(key) && !row.get(key).isJsonNull() ? row.get(key).getAsString() : ""; }
    private static long id(JsonObject row, String key) { return BookMigrationDefinitions.id(text(row, key)); }
    private static String hex(long id) { return String.format("%016X", id); }
}
