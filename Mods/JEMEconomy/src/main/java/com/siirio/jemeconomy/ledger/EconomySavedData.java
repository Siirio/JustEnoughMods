package com.siirio.jemeconomy.ledger;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

public final class EconomySavedData extends SavedData {
    private static final String NAME = "jem_economy";
    private static final int MAX_RECEIPTS = 50_000;
    private final Map<UUID, Long> balances = new LinkedHashMap<>();
    private final LinkedHashMap<String, UUID> receipts = new LinkedHashMap<>();

    public EconomySavedData() {
        for (SystemAccount account : SystemAccount.values()) balances.put(account.id(), account.initialBalance());
    }

    public static EconomySavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(EconomySavedData::load, EconomySavedData::new, NAME);
    }

    public synchronized long balance(UUID id) { return balances.getOrDefault(id, 0L); }

    public synchronized TransferResult transfer(UUID source, UUID destination, long amount, String idempotencyKey) {
        if (source == null || destination == null || source.equals(destination) || amount <= 0L || idempotencyKey == null || idempotencyKey.isBlank() || idempotencyKey.length() > 160) {
            return new TransferResult(TransferResult.Status.INVALID, null, balance(source), balance(destination));
        }
        UUID prior = receipts.get(idempotencyKey);
        if (prior != null) return new TransferResult(TransferResult.Status.REPLAYED, prior, balance(source), balance(destination));
        long sourceBalance = balance(source);
        if (sourceBalance < amount) return new TransferResult(TransferResult.Status.INSUFFICIENT_FUNDS, null, sourceBalance, balance(destination));
        long destinationBalance = Math.addExact(balance(destination), amount);
        balances.put(source, sourceBalance - amount);
        balances.put(destination, destinationBalance);
        UUID receipt = UUID.nameUUIDFromBytes(("jem:receipt:" + idempotencyKey).getBytes(StandardCharsets.UTF_8));
        receipts.put(idempotencyKey, receipt);
        while (receipts.size() > MAX_RECEIPTS) receipts.remove(receipts.keySet().iterator().next());
        setDirty();
        return new TransferResult(TransferResult.Status.COMMITTED, receipt, sourceBalance - amount, destinationBalance);
    }

    @Override
    public synchronized CompoundTag save(CompoundTag tag) {
        CompoundTag balanceTag = new CompoundTag();
        balances.forEach((id, amount) -> balanceTag.putLong(id.toString(), amount));
        CompoundTag receiptTag = new CompoundTag();
        receipts.forEach((key, id) -> receiptTag.putUUID(key, id));
        tag.put("Balances", balanceTag);
        tag.put("Receipts", receiptTag);
        return tag;
    }

    private static EconomySavedData load(CompoundTag tag) {
        EconomySavedData data = new EconomySavedData();
        CompoundTag balanceTag = tag.getCompound("Balances");
        for (String key : balanceTag.getAllKeys()) {
            try { data.balances.put(UUID.fromString(key), Math.max(0L, balanceTag.getLong(key))); }
            catch (IllegalArgumentException ignored) { }
        }
        CompoundTag receiptTag = tag.getCompound("Receipts");
        for (String key : receiptTag.getAllKeys()) {
            if (receiptTag.hasUUID(key)) data.receipts.put(key, receiptTag.getUUID(key));
        }
        return data;
    }
}
