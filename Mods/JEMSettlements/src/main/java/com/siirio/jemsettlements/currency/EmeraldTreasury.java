package com.siirio.jemsettlements.currency;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/** Physical deposits and explicitly authorized daily NPC emerald income; coin saves are never read. */
final class EmeraldTreasury extends SavedData {
    final Map<UUID, Long> accounts = new LinkedHashMap<>();
    final Map<String, CompoundTag> pending = new LinkedHashMap<>();
    final Map<String, CompoundTag> receipts = new LinkedHashMap<>();
    static EmeraldTreasury get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(EmeraldTreasury::load, EmeraldTreasury::new, "jem_settlements_emeralds");
    }
    long balance(UUID account) { return accounts.getOrDefault(account, 0L); }
    CompoundTag prior(String key) { return receipts.get(key); }
    boolean full() { return receipts.size() >= 100_000; }
    UUID record(String key, String terms, UUID source, UUID destination, long amount) {
        UUID receipt = UUID.nameUUIDFromBytes(key.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        CompoundTag row = new CompoundTag(); row.putString("Key", key); row.putString("Terms", terms);
        row.putUUID("Receipt", receipt); row.putUUID("Source", source); row.putUUID("Destination", destination); row.putLong("Amount", amount);
        receipts.put(key, row); setDirty(); return receipt;
    }
    void flush(MinecraftServer server) {
        var target = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve("data/jem_settlements_emeralds.dat");
        CompoundTag expected = save(new CompoundTag()); java.nio.file.Path temporary = null;
        try {
            java.nio.file.Files.createDirectories(target.getParent());
            temporary = java.nio.file.Files.createTempFile(target.getParent(), "jem_settlements_emeralds-", ".dat.tmp");
            // SavedData owns the native envelope/DataVersion. Never truncate the last durable intent file.
            setDirty(); save(temporary.toFile());
            if (!expected.equals(net.minecraft.nbt.NbtIo.readCompressed(temporary.toFile()).getCompound("data")))
                throw new java.io.IOException("Emerald treasury temporary save mismatch");
            try (var channel = java.nio.channels.FileChannel.open(temporary, java.nio.file.StandardOpenOption.WRITE)) { channel.force(true); }
            java.nio.file.Files.move(temporary, target, java.nio.file.StandardCopyOption.ATOMIC_MOVE, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            if (!expected.equals(net.minecraft.nbt.NbtIo.readCompressed(target.toFile()).getCompound("data")))
                throw new java.io.IOException("Emerald treasury save mismatch");
        } catch (java.io.IOException failure) { setDirty(); throw new IllegalStateException("Emerald treasury storage unavailable", failure); }
        finally {
            if (temporary != null) try { java.nio.file.Files.deleteIfExists(temporary); }
            catch (java.io.IOException failure) { com.mojang.logging.LogUtils.getLogger().warn("Could not remove emerald treasury temporary file", failure); }
        }
    }
    boolean locked(UUID account) { return pending.values().stream().anyMatch(row -> row.getUUID("Account").equals(account)); }
    ListTag history(UUID account) {
        var rows = receipts.values().stream().filter(row -> row.getUUID("Source").equals(account) || row.getUUID("Destination").equals(account)).toList();
        ListTag result = new ListTag();
        for (int i = rows.size() - 1; i >= Math.max(0, rows.size() - 16); i--) {
            CompoundTag row = rows.get(i).copy(); boolean incoming = row.getUUID("Destination").equals(account);
            row.putBoolean("Incoming", incoming); row.putString("Kind", incoming ? "incoming" : "outgoing"); row.remove("Key"); row.remove("Terms"); result.add(row);
        }
        return result;
    }
    @Override public CompoundTag save(CompoundTag tag) {
        CompoundTag balances = new CompoundTag(); accounts.forEach((id, value) -> balances.putLong(id.toString(), value)); tag.put("Accounts", balances);
        ListTag rows = new ListTag(); receipts.values().forEach(row -> rows.add(row.copy())); tag.put("Receipts", rows); ListTag intents = new ListTag(); pending.values().forEach(row -> intents.add(row.copy())); tag.put("Pending", intents); return tag;
    }
    private static EmeraldTreasury load(CompoundTag tag) {
        EmeraldTreasury data = new EmeraldTreasury(); CompoundTag balances = tag.getCompound("Accounts");
        for (String id : balances.getAllKeys()) data.accounts.put(UUID.fromString(id), Math.max(0L, balances.getLong(id)));
        for (Tag value : tag.getList("Receipts", Tag.TAG_COMPOUND)) { CompoundTag row = (CompoundTag) value; data.receipts.put(row.getString("Key"), row.copy()); }
        for (Tag value : tag.getList("Pending", Tag.TAG_COMPOUND)) { CompoundTag row = (CompoundTag) value; data.pending.put(row.getString("Key"), row.copy()); }
        return data;
    }
}
