package com.siirio.jemserver.smp;

import java.io.IOException;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.LevelResource;

/** Persistent account entitlement transitions, including recoverable legacy delivery obligations. */
final class StarterEntitlements extends SavedData {
    private static final String NAME = "jem_starter_kits";
    private final CompoundTag accounts;
    private boolean persistenceUncertain;

    private StarterEntitlements() { this(new CompoundTag()); }
    private StarterEntitlements(CompoundTag accounts) { this.accounts = accounts; }

    static StarterEntitlements get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                tag -> new StarterEntitlements(tag.getCompound("Accounts")), StarterEntitlements::new, NAME);
    }

    @Override public CompoundTag save(CompoundTag tag) {
        tag.put("Accounts", accounts.copy());
        return tag;
    }

    void ensure(MinecraftServer server, UUID account, ListTag items) throws IOException {
        if (!accounts.contains(account.toString(), Tag.TAG_COMPOUND)) {
            CompoundTag row = new CompoundTag();
            row.put("Remaining", items.copy());
            accounts.put(account.toString(), row);
            flush(server);
        }
    }

    boolean exists(UUID account) { return accounts.contains(account.toString(), Tag.TAG_COMPOUND); }
    CompoundTag pending(UUID account) { return row(account).getCompound("Pending").copy(); }

    boolean complete(MinecraftServer server, UUID account) throws IOException {
        if (!row(account).getBoolean("Complete")) return false;
        if (persistenceUncertain) flush(server);
        return true;
    }

    CompoundTag prepareRewardTransfer(MinecraftServer server, UUID account, UUID player) throws IOException {
        CompoundTag row = row(account);
        if (!row.contains("RewardTransfer", Tag.TAG_COMPOUND)) {
            CompoundTag transfer = new CompoundTag();
            transfer.putUUID("Player", player);
            transfer.put("Items", row.getList("Remaining", Tag.TAG_COMPOUND).copy());
            row.put("RewardTransfer", transfer);
        }
        flush(server);
        return row.getCompound("RewardTransfer").copy();
    }

    void completeTransfer(MinecraftServer server, UUID account) throws IOException {
        CompoundTag row = row(account);
        row.put("Remaining", new ListTag());
        row.putBoolean("Complete", true);
        flush(server);
    }

    void reconcileLegacy(MinecraftServer server, UUID account, boolean delivered) throws IOException {
        CompoundTag row = row(account);
        if (delivered) {
            row.put("Remaining", row.getCompound("Pending").getList("Remaining", Tag.TAG_COMPOUND).copy());
            row.putBoolean("Complete", row.getList("Remaining", Tag.TAG_COMPOUND).isEmpty());
        }
        row.remove("Pending");
        flush(server);
    }

    private CompoundTag row(UUID account) { return accounts.getCompound(account.toString()); }

    private void flush(MinecraftServer server) throws IOException {
        persistenceUncertain = true;
        setDirty();
        var file = server.getWorldPath(LevelResource.ROOT).resolve("data").resolve(NAME + ".dat").toFile();
        save(file);
        if (!accounts.equals(NbtIo.readCompressed(file).getCompound("data").getCompound("Accounts"))) {
            setDirty();
            throw new IOException("Starter kit journal was not persisted");
        }
        persistenceUncertain = false;
    }

}
