package com.siirio.jemworldbosstiers.encounter;

import java.util.List;
import java.util.UUID;
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
        if (recipients.isEmpty()) return;
        java.util.Map<UUID, java.util.ArrayList<ItemStack>> shares = new java.util.LinkedHashMap<>();
        recipients.forEach(id -> shares.put(id, new java.util.ArrayList<>()));
        int cursor = 0;
        for (ItemStack stack : loot) {
            int quotient = stack.getCount() / recipients.size();
            int remainder = stack.getCount() % recipients.size();
            for (int index = 0; index < recipients.size(); index++) {
                int amount = quotient + (index < remainder ? 1 : 0);
                if (amount > 0) shares.get(recipients.get((cursor + index) % recipients.size()))
                        .add(stack.copyWithCount(amount));
            }
            cursor = (cursor + remainder) % recipients.size();
        }
        shares.forEach((id, items) -> PendingRewardContainer.enqueue(server, id,
                sourceId, "reward.jem_world_boss_tiers.boss_loot", items));
    }

    public void deliver(ServerPlayer player) {
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
        save(file);
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
    public CompoundTag save(CompoundTag tag) {
        for (String key : pending.getAllKeys()) {
            tag.put(key, pending.get(key).copy());
        }
        return tag;
    }
}
