package com.siirio.jemrewards;

import java.util.*;
import java.nio.file.*;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.LevelResource;

/** Native reward snapshots, permanent issue receipts and recoverable inventory transfers. */
public final class RewardsData extends SavedData {
    private static final String NAME = "jem_rewards";
    static final String RECEIPT = "jem_rewards:claim";
    private final CompoundTag pending;
    private final Set<String> durableBatches = new HashSet<>();
    private long revision;
    RewardsData(CompoundTag root) {
        pending = root.copy();
        revision = root.getLong("Revision");
        durableBatches.addAll(root.getCompound("BatchReceipts").getAllKeys());
    }
    public static RewardsData get(MinecraftServer server) {
        RewardsData data = server.overworld().getDataStorage().computeIfAbsent(RewardsData::new, () -> new RewardsData(new CompoundTag()), NAME);
        data.migrate(server);
        return data;
    }
    CompoundTag pending() { return pending; }
    public long revision() { return revision; }
    @Override public void setDirty() { revision++; pending.putLong("Revision", revision); super.setDirty(); }
    @Override public CompoundTag save(CompoundTag tag) { return pending.copy(); }
    @Override public void save(java.io.File file) {
        if (!isDirty()) return;
        Path target = file.toPath();
        Path temporary = null;
        try {
            Files.createDirectories(target.toAbsolutePath().getParent());
            temporary = Files.createTempFile(target.toAbsolutePath().getParent(), NAME + "-", ".tmp");
            CompoundTag root = new CompoundTag();
            root.put("data", pending.copy());
            NbtUtils.addCurrentDataVersion(root);
            NbtIo.writeCompressed(root, temporary.toFile());
            if (!pending.equals(NbtIo.readCompressed(temporary.toFile()).getCompound("data")))
                throw new java.io.IOException("Prepared reward journal not persisted");
            net.minecraft.Util.safeReplaceFile(target, temporary, target.resolveSibling(file.getName() + "_old"));
            if (!pending.equals(NbtIo.readCompressed(file).getCompound("data")))
                throw new java.io.IOException("Reward journal replacement not persisted");
            durableBatches.clear();
            durableBatches.addAll(pending.getCompound("BatchReceipts").getAllKeys());
            super.setDirty(false);
        } catch (java.io.IOException failure) {
            super.setDirty();
            com.mojang.logging.LogUtils.getLogger().error("Reward journal remains dirty after native save failure", failure);
        } finally {
            if (temporary != null) try { Files.deleteIfExists(temporary); }
            catch (java.io.IOException failure) { com.mojang.logging.LogUtils.getLogger().warn("Could not remove prepared reward journal {}", temporary, failure); }
        }
    }
    void flush(MinecraftServer server) {
        setDirty();
        save(server.getWorldPath(LevelResource.ROOT).resolve("data").resolve(NAME + ".dat").toFile());
        if (isDirty()) throw new IllegalStateException("storage_unavailable");
    }
    boolean issued(UUID player, String source) { return pending.getCompound("Issued").getBoolean(player + ":" + source); }
    void markIssued(UUID player, String source) {
        var receipts = pending.getCompound("Issued"); receipts.putBoolean(player + ":" + source, true); pending.put("Issued", receipts);
    }
    private static CompoundTag read(MinecraftServer server, String name) {
        Path file = server.getWorldPath(LevelResource.ROOT).resolve("data").resolve(name + ".dat");
        if (!Files.exists(file)) return new CompoundTag();
        try { return NbtIo.readCompressed(file.toFile()).getCompound("data"); }
        catch (java.io.IOException failure) { throw new IllegalStateException("storage_unavailable", failure); }
    }
    private void migrate(MinecraftServer server) {
        if (pending.getBoolean("LegacyImported")) return;
        CompoundTag boss = read(server, "jem_hosted_boss_rewards");
        for (String key : boss.getAllKeys()) {
            if (key.startsWith("RewardContainers:")) pending.put(key, boss.get(key).copy());
            else try { UUID.fromString(key); pending.put(key, boss.get(key).copy()); } catch (IllegalArgumentException ignored) { }
        }
        for (String key : java.util.List.copyOf(pending.getAllKeys())) {
            if (!key.startsWith("RewardContainers:")) continue;
            UUID owner = UUID.fromString(key.substring("RewardContainers:".length()));
            for (Tag value : pending.getList(key, Tag.TAG_COMPOUND)) {
                CompoundTag row = (CompoundTag) value;
                if (!row.getString("SourceId").isEmpty()) markIssued(owner, row.getString("SourceId"));
            }
        }
        CompoundTag smp = read(server, "jem_smp_v2");
        if (smp.getInt("dataVersion") > 1) throw new IllegalStateException("future_data_version");
        for (Tag value : smp.getCompound("tables").getList("profiles", Tag.TAG_COMPOUND)) {
            CompoundTag profile = (CompoundTag) value;
            if (!profile.hasUUID("id")) continue;
            UUID owner = profile.getUUID("id");
            ListTag items = profile.getList("deliveries", Tag.TAG_COMPOUND).copy();
            CompoundTag claim = profile.getCompound("deliveryClaim");
            CompoundTag receipt = savedPlayer(server, owner).getCompound("ForgeData").getCompound("jem_smp_delivery_receipt");
            if (claim.hasUUID("id") && receipt.hasUUID("id") && claim.getUUID("id").equals(receipt.getUUID("id"))) items = receipt.getList("remaining", Tag.TAG_COMPOUND).copy();
            UUID migration = UUID.nameUUIDFromBytes(("jem_smp_deliveries:" + owner).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            importRow(server, owner, migration, items);
            CompoundTag journal = profile.getCompound("rewardMigration");
            if (journal.hasUUID("id")) importRow(server, owner, journal.getUUID("id"), journal.getList("items", Tag.TAG_COMPOUND));
        }
        // Retained source files are historical evidence only; the committed marker prevents replay.
        pending.putBoolean("LegacyImported", true);
        flush(server);
    }
    void importRow(MinecraftServer server, UUID owner, UUID id, ListTag items) {
        recover(server, owner);
        String key = "RewardContainers:" + owner;
        ListTag rows = pending.getList(key, Tag.TAG_COMPOUND);
        for (Tag value : rows) {
            CompoundTag row = (CompoundTag) value;
            if (row.hasUUID("Id") && id.equals(row.getUUID("Id"))) { row.remove("ImportPending"); return; }
        }
        if (pending.getCompound("Imports").getBoolean(id.toString())) return;
        var imports = pending.getCompound("Imports"); imports.putBoolean(id.toString(), true); pending.put("Imports", imports);
        if (items.isEmpty()) return;
        CompoundTag row = new CompoundTag(); row.putUUID("Id", id); row.putString("SourceId", "legacy_smp_delivery"); row.putString("TitleKey", "reward.jem_world_boss_tiers.legacy_delivery"); row.put("Items", items.copy()); rows.add(row); pending.put(key, rows);
    }
    void enqueueBatch(MinecraftServer server, String source, ListTag grants) {
        if (pending.getCompound("BatchReceipts").getBoolean(source)) { flush(server); return; }
        String key = "Batch:" + source;
        // Keep the exact rolled snapshot even when its first disk write fails.
        if (!pending.contains(key, Tag.TAG_LIST)) pending.put(key, grants.copy());
        flush(server);
        finishBatch(server, source);
    }
    boolean batchCommitted(String source) { return durableBatches.contains(source); }
    private void finishBatch(MinecraftServer server, String source) {
        String key = "Batch:" + source;
        ListTag grants = pending.getList(key, Tag.TAG_COMPOUND);
        // Resolve every prior inventory operation before mutating any recipient's list.
        for (Tag value : grants) recover(server, ((CompoundTag) value).getUUID("Player"));
        for (Tag value : grants) {
            CompoundTag grant = (CompoundTag) value;
            UUID owner = grant.getUUID("Player");
            if (issued(owner, source)) continue;
            String rowsKey = "RewardContainers:" + owner;
            ListTag rows = pending.getList(rowsKey, Tag.TAG_COMPOUND);
            rows.add(grant.getCompound("Container").copy());
            pending.put(rowsKey, rows);
            markIssued(owner, source);
        }
        CompoundTag receipts = pending.getCompound("BatchReceipts");
        receipts.putBoolean(source, true);
        pending.put("BatchReceipts", receipts);
        // All recipients and their receipts become durable together. Keep journal until verified.
        flush(server);
        pending.remove(key);
        flush(server);
    }
    void retryBatches(MinecraftServer server) {
        int remaining = 4;
        for (String key : java.util.List.copyOf(pending.getAllKeys())) {
            if (!key.startsWith("Batch:") || remaining-- <= 0) continue;
            try { finishBatch(server, key.substring("Batch:".length())); }
            catch (RuntimeException failure) {
                com.mojang.logging.LogUtils.getLogger().error("Reward batch {} remains pending; storage must be restored", key, failure);
            }
        }
        if (isDirty()) {
            try { flush(server); }
            catch (RuntimeException failure) { com.mojang.logging.LogUtils.getLogger().error("Reward storage remains unavailable; unsaved in-memory operations require this server process to remain alive", failure); }
        }
    }

    static CompoundTag savedPlayer(MinecraftServer server, UUID owner) {
        Path file = server.getWorldPath(LevelResource.PLAYER_DATA_DIR).resolve(owner + ".dat");
        if (!Files.exists(file)) return new CompoundTag();
        try { return NbtIo.readCompressed(file.toFile()); }
        catch (java.io.IOException failure) { throw new IllegalStateException("storage_unavailable", failure); }
    }
    static void savePlayer(ServerPlayer player) {
        CompoundTag expected = player.saveWithoutId(new CompoundTag());
        player.server.getPlayerList().saveAll();
        if (player.server.isSingleplayer()) player.server.saveEverything(false, true, true);
        CompoundTag actual = savedPlayer(player.server, player.getUUID());
        if (!expected.getList("Inventory", Tag.TAG_COMPOUND).equals(actual.getList("Inventory", Tag.TAG_COMPOUND))
                || !Objects.equals(expected.getCompound("ForgeData").get(RECEIPT), actual.getCompound("ForgeData").get(RECEIPT)))
            throw new IllegalStateException("storage_unavailable");
    }
    /** Resolve a prepared transfer before any mutation, including grants to offline players. */
    void recover(MinecraftServer server, UUID owner) {
        ServerPlayer online = server.getPlayerList().getPlayer(owner);
        if (online != null) { recover(online); return; }
        String key = "Claim:" + owner;
        if (!pending.contains(key, Tag.TAG_COMPOUND)) return;
        CompoundTag journal = pending.getCompound(key);
        CompoundTag receipt = savedPlayer(server, owner).getCompound("ForgeData").getCompound(RECEIPT);
        finishRecovery(server, owner, journal, receipt);
    }
    void recover(ServerPlayer player) {
        String key = "Claim:" + player.getUUID();
        if (!pending.contains(key, Tag.TAG_COMPOUND)) return;
        CompoundTag journal = pending.getCompound(key);
        CompoundTag receipt = player.getPersistentData().getCompound(RECEIPT);
        if (receipt.hasUUID("Id") && journal.getUUID("Id").equals(receipt.getUUID("Id"))) savePlayer(player);
        finishRecovery(player.server, player.getUUID(), journal, receipt);
    }
    private void finishRecovery(MinecraftServer server, UUID owner, CompoundTag journal, CompoundTag receipt) {
        if (receipt.hasUUID("Id") && journal.getUUID("Id").equals(receipt.getUUID("Id")))
            pending.put("RewardContainers:" + owner, journal.getList("Remaining", Tag.TAG_COMPOUND).copy());
        pending.remove("Claim:" + owner);
        flush(server);
    }
    void commitClaim(ServerPlayer player, ListTag remaining, java.util.List<net.minecraft.world.item.ItemStack> inventory) {
        recover(player);
        String key = "Claim:" + player.getUUID();
        UUID operation = UUID.randomUUID();
        CompoundTag journal = new CompoundTag(); journal.putUUID("Id", operation); journal.put("Remaining", remaining.copy());
        pending.put(key, journal); flush(player.server);
        for (int slot = 0; slot < inventory.size(); slot++) player.getInventory().items.set(slot, inventory.get(slot));
        CompoundTag receipt = new CompoundTag(); receipt.putUUID("Id", operation); player.getPersistentData().put(RECEIPT, receipt);
        player.getInventory().setChanged(); savePlayer(player);
        pending.put("RewardContainers:" + player.getUUID(), remaining.copy()); pending.remove(key); flush(player.server);
    }
}
