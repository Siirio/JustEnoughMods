package com.siirio.jemserver.smp;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public final class Profiles {
    public static CompoundTag get(net.minecraft.server.MinecraftServer server, UUID id) {
        var data = SmpData.get(server);
        var profile = data.find("profiles", id);
        if (profile == null) {
            profile = new CompoundTag();
            profile.putUUID("id", id);
            profile.putUUID("owner", id);
            profile.putLong("firstJoin", System.currentTimeMillis());
            data.put("profiles", profile);
        }
        return profile;
    }

    public static void login(ServerPlayer player) {
        var row = get(player.server, player.getUUID());
        row.putString("name", player.getGameProfile().getName());
        SmpData.get(player.server).changed(row);
        try {
            LegacyRewards.migrate(player);
        } catch (IllegalStateException | IllegalArgumentException failure) {
            player.sendSystemMessage(
                    net.minecraft.network.chat.Component.translatable(
                            "jem.smp.escrow_recovery_required"));
            com.mojang.logging.LogUtils.getLogger()
                    .error("SMP escrow login recovery failed for {}", player.getUUID(), failure);
        }
    }

    public static void count(net.minecraft.server.MinecraftServer server, UUID id, String key) {
        var row = get(server, id);
        row.putLong(key, row.getLong(key) + 1);
        SmpData.get(server).changed(row);
    }

    public static void countEvent(net.minecraft.server.MinecraftServer server, UUID id, String type) {
        var row = get(server, id);
        var statistics = row.getCompound("eventStats");
        statistics.putLong(type, statistics.getLong(type) + 1);
        row.put("eventStats", statistics);
        SmpData.get(server).changed(row);
    }

    public static CompoundTag view(ServerPlayer viewer, UUID id) {
        var source = SmpData.get(viewer.server).find("profiles", id);
        var row = new CompoundTag();
        if (source == null) return row;
        for (String key :
                java.util.List.of(
                        "id",
                        "owner",
                        "revision",
                        "name",
                        "firstJoin",
                        "playTicks",
                        "partiesHosted",
                        "partiesJoined",
                        "bossesHosted",
                        "bossAssists",
                        "raidsCompleted",
                        "bloodMoonClears",
                        "fishingEvents",
                        "creaturesTamed",
                        "landmarksCreated",
                        "biomesExplored",
                        "animalsTamed",
                        "tameSpecies",
                        "fishingMilestones")) {
            if (source.contains(key)) row.put(key, source.get(key).copy());
        }
        row.putInt("discoveredBiomes", source.getCompound("discoveredBiomes").size());
        var statistics = new net.minecraft.nbt.ListTag();
        var eventStats = source.getCompound("eventStats");
        for (String type : eventStats.getAllKeys()) {
            var statistic = new CompoundTag();
            statistic.putString("type", type);
            statistic.putLong("count", eventStats.getLong(type));
            statistics.add(statistic);
        }
        statistics.sort(java.util.Comparator.comparing(value -> ((CompoundTag) value).getString("type")));
        row.put("eventStats", statistics);
        var player = viewer.server.getPlayerList().getPlayer(id);
        row.putBoolean("online", player != null);
        if (player != null)
            row.putInt(
                    "playTicks",
                    player.getStats()
                            .getValue(
                                    net.minecraft.stats.Stats.CUSTOM.get(
                                            net.minecraft.stats.Stats.PLAY_TIME)));
        if (player != null)
            row.putInt(
                    "mobsKilled",
                    player.getStats().getValue(net.minecraft.stats.Stats.CUSTOM.get(net.minecraft.stats.Stats.MOB_KILLS)));
        else if (source.contains("mobsKilled")) row.putInt("mobsKilled", source.getInt("mobsKilled"));
        return row;
    }

    private Profiles() {}
}
