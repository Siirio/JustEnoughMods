package com.siirio.jemworldbosstiers.encounter;

import java.util.List;
import java.util.UUID;
import java.util.Collection;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;

public final class HostedRewards extends SavedData {
    private static final String NAME = "jem_hosted_boss_rewards";
    private final CompoundTag pending;
    private long revision;

    public long revision() {
        return revision;
    }

    @Override
    public void setDirty() {
        revision++;
        super.setDirty();
    }

    HostedRewards(CompoundTag pending) {
        this.pending = pending;
    }

    public static HostedRewards get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(HostedRewards::new,
                () -> new HostedRewards(new CompoundTag()), NAME);
    }

    public void distribute(MinecraftServer server, List<UUID> recipients, List<ItemStack> loot, String sourceId) {
        var grants = new java.util.LinkedHashSet<>(recipients).stream()
                .map(recipient -> new com.siirio.jemrewards.RewardGrant(recipient,
                        "reward.jem_world_boss_tiers.boss_loot",
                        List.of(new com.siirio.jemrewards.RewardGroup("reward.jem_world_boss_tiers.native_loot", loot))))
                .toList();
        com.siirio.jemrewards.PendingRewardContainer.enqueueBatch(server, sourceId, grants);
    }

    public void deliver(ServerPlayer player) {
        deliverExperience(player);
        String advancementKey = "Advancements:" + player.getStringUUID();
        ListTag advancements = pending.getList(advancementKey, Tag.TAG_STRING);
        for (Tag value : advancements) {
            var advancement = player.server.getAdvancements().getAdvancement(ResourceLocation.tryParse(value.getAsString()));
            if (advancement != null) {
                for (String criterion : player.getAdvancements().getOrStartProgress(advancement).getRemainingCriteria()) {
                    player.getAdvancements().award(advancement, criterion);
                }
            }
        }
        if (!advancements.isEmpty()) {
            pending.remove(advancementKey);
            setDirty();
        }
        String recovery = "Recovery:" + player.getStringUUID();
        if (pending.contains(recovery)) {
            double health = pending.getDouble(recovery);
            pending.remove(recovery);
            player.getPersistentData().remove("JEMHostedDowned");
            if (health > 0) {
                player.setHealth((float) (player.getMaxHealth() * health));
            } else {
                player.hurt(player.damageSources().genericKill(), Float.MAX_VALUE);
            }
            setDirty();
        }
    }

    CompoundTag pending() {
        return pending;
    }

    /** Non-item XP stays here, with one frozen source and a receipt in the native player save. */
    public void enqueueExperience(MinecraftServer server, String source, Collection<UUID> recipients, int amount) {
        String key = "ExperienceBatch:" + source;
        if (pending.getCompound("ExperienceIssued").getBoolean(source)) {
            pending.remove(key);
            flush(server);
            return;
        }
        if (!pending.contains(key)) {
            CompoundTag snapshot = new CompoundTag();
            if (amount > 0) recipients.forEach(id -> snapshot.putInt(id.toString(), amount));
            pending.put(key, snapshot);
            setDirty();
        }
        flush(server);
        CompoundTag snapshot = pending.getCompound(key);
        for (String id : snapshot.getAllKeys()) {
            CompoundTag deliveries = pending.getCompound("Experience:" + id);
            deliveries.putInt(source, snapshot.getInt(id));
            pending.put("Experience:" + id, deliveries);
        }
        CompoundTag issued = pending.getCompound("ExperienceIssued");
        issued.putBoolean(source, true);
        pending.put("ExperienceIssued", issued);
        setDirty();
        flush(server);
        pending.remove(key);
        setDirty();
        flush(server);
    }

    void retryExperienceBatches(MinecraftServer server) {
        int remaining = 4;
        for (String key : java.util.List.copyOf(pending.getAllKeys())) {
            if (!key.startsWith("ExperienceBatch:") || remaining-- <= 0) continue;
            try { enqueueExperience(server, key.substring("ExperienceBatch:".length()), List.of(), 0); }
            catch (RuntimeException failure) {
                com.mojang.logging.LogUtils.getLogger().error("Personal boss XP batch {} remains pending", key, failure);
            }
        }
    }

    private void deliverExperience(ServerPlayer player) {
        if (!player.isAlive()) return;
        String key = "Experience:" + player.getUUID();
        CompoundTag deliveries = pending.getCompound(key);
        for (String source : java.util.List.copyOf(deliveries.getAllKeys())) {
            // Prepare and acknowledge delivery before touching XP, Mending or player receipts.
            flush(player.server);
            CompoundTag receipts = player.getPersistentData().getCompound("JEMHostedXpReceipts");
            if (!receipts.getBoolean(source)) {
                com.siirio.jemworldbosstiers.api.ExperienceRewardApi.give(player, deliveries.getInt(source));
                receipts.putBoolean(source, true);
                player.getPersistentData().put("JEMHostedXpReceipts", receipts);
            }
            saveExperiencePlayer(player);
            deliveries.remove(source);
            if (deliveries.isEmpty()) pending.remove(key);
            setDirty();
            flush(player.server);
        }
    }

    private static void saveExperiencePlayer(ServerPlayer player) {
        CompoundTag expected = player.saveWithoutId(new CompoundTag());
        player.server.getPlayerList().saveAll();
        var file = player.server.getWorldPath(net.minecraft.world.level.storage.LevelResource.PLAYER_DATA_DIR)
                .resolve(player.getUUID() + ".dat").toFile();
        try {
            CompoundTag actual = net.minecraft.nbt.NbtIo.readCompressed(file);
            if (expected.getInt("XpTotal") != actual.getInt("XpTotal")
                    || expected.getInt("XpLevel") != actual.getInt("XpLevel")
                    || expected.getFloat("XpP") != actual.getFloat("XpP")
                    || !expected.getList("Inventory", Tag.TAG_COMPOUND).equals(actual.getList("Inventory", Tag.TAG_COMPOUND))
                    || !expected.getCompound("ForgeData").getCompound("JEMHostedXpReceipts")
                        .equals(actual.getCompound("ForgeData").getCompound("JEMHostedXpReceipts")))
                throw new java.io.IOException("Personal experience not persisted");
        } catch (java.io.IOException failure) { throw new IllegalStateException("storage_unavailable", failure); }
    }

    public void resolveDowned(MinecraftServer server, UUID playerId, boolean rescued) {
        pending.putDouble("Recovery:" + playerId, rescued ? HostedConfig.REVIVE_HEALTH.get() : -1);
        setDirty();
        ServerPlayer player = server.getPlayerList().getPlayer(playerId);
        if (player != null) {
            deliver(player);
        }
    }

    public void award(MinecraftServer server, UUID playerId, ResourceLocation advancement) {
        String key = "Advancements:" + playerId;
        ListTag values = pending.getList(key, Tag.TAG_STRING);
        StringTag value = StringTag.valueOf(advancement.toString());
        if (!values.contains(value)) {
            values.add(value);
            pending.put(key, values);
            setDirty();
        }
        ServerPlayer player = server.getPlayerList().getPlayer(playerId);
        if (player != null) {
            deliver(player);
        }
    }

    void flush(MinecraftServer server) {
        var file = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)
                .resolve("data").resolve(NAME + ".dat").toFile();
        setDirty();
        save(file);
        if (isDirty()) throw new IllegalStateException("storage_unavailable");
        try {
            if (!pending.equals(net.minecraft.nbt.NbtIo.readCompressed(file).getCompound("data")))
                throw new java.io.IOException("Pending rewards not persisted");
        } catch (java.io.IOException failure) {
            setDirty();
            com.mojang.logging.LogUtils.getLogger().error("Could not persist pending rewards", failure);
            throw new IllegalStateException("storage_unavailable");
        }
    }

    @Override
    public void save(java.io.File file) {
        if (!isDirty()) return;
        var target = file.toPath().toAbsolutePath();
        java.nio.file.Path temporary = null;
        try {
            java.nio.file.Files.createDirectories(target.getParent());
            temporary = java.nio.file.Files.createTempFile(target.getParent(), NAME + "-", ".tmp");
            CompoundTag root = new CompoundTag();
            root.put("data", pending.copy());
            net.minecraft.nbt.NbtUtils.addCurrentDataVersion(root);
            net.minecraft.nbt.NbtIo.writeCompressed(root, temporary.toFile());
            if (!pending.equals(net.minecraft.nbt.NbtIo.readCompressed(temporary.toFile()).getCompound("data")))
                throw new java.io.IOException("Prepared hosted entitlement not persisted");
            net.minecraft.Util.safeReplaceFile(target, temporary, target.resolveSibling(file.getName() + "_old"));
            if (!pending.equals(net.minecraft.nbt.NbtIo.readCompressed(file).getCompound("data")))
                throw new java.io.IOException("Hosted entitlement replacement not persisted");
            super.setDirty(false);
        } catch (java.io.IOException failure) {
            super.setDirty();
            com.mojang.logging.LogUtils.getLogger().error("Hosted entitlement journal remains dirty after native save failure", failure);
        } finally {
            if (temporary != null) try { java.nio.file.Files.deleteIfExists(temporary); }
            catch (java.io.IOException failure) { com.mojang.logging.LogUtils.getLogger().warn("Could not remove prepared hosted entitlement {}", temporary, failure); }
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        for (String key : pending.getAllKeys()) {
            tag.put(key, pending.get(key).copy());
        }
        return tag;
    }
}
