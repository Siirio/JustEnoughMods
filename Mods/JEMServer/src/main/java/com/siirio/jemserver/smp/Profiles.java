package com.siirio.jemserver.smp;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public final class Profiles {
    public CompoundTag bind(CompoundTag record) {
        if (record == null) return null;
        SmpRecords.require(record.hasUUID("id"), "unavailable");
        CompoundTag current = current(record.getUUID("id"));
        SmpRecords.require(current != null, "unavailable");
        return current;
    }
    private final SmpData storage;
    Profiles(SmpData storage) { this.storage = storage; }
    public net.minecraft.nbt.CompoundTag snapshot(UUID id) { return storage.snapshot("profiles", id); }
    public java.util.List<net.minecraft.nbt.CompoundTag> snapshots() { return storage.snapshots("profiles"); }
    public CompoundTag current(UUID id) { return storage.current("profiles", id); }
    public java.util.Collection<net.minecraft.nbt.CompoundTag> currentRows() { return storage.currentRows("profiles"); }
    public void put(CompoundTag row) { storage.put("profiles", row); }

    public static CompoundTag get(net.minecraft.server.MinecraftServer server, UUID id) {
        var data = SmpData.get(server);
        var profile = data.profiles().current(id);
        if (profile == null) {
            profile = new CompoundTag();
            profile.putUUID("id", id);
            profile.putUUID("owner", id);
            profile.putLong("firstJoin", System.currentTimeMillis());
            data.profiles().put(profile);
        }
        return profile;
    }

    public static CompoundTag snapshot(net.minecraft.server.MinecraftServer server, UUID id) {
        CompoundTag row = SmpData.get(server).profiles().snapshot(id);
        return row == null ? new CompoundTag() : row.copy();
    }

    public static void captureProfessions(ServerPlayer player) {
        var data = SmpData.get(player.server);
        data.edit(() -> captureProfessionsOwned(player));
    }

    private static void captureProfessionsOwned(ServerPlayer player) {
        if (!DomainFeatures.loaded("jem_professions")) return;
        var snapshot = com.siirio.jemserver.compat.professions.ProfessionHostAdapter.snapshot(player);
        var row = get(player.server, player.getUUID());
        if (!snapshot.equals(row.getList("professions", net.minecraft.nbt.Tag.TAG_COMPOUND))) {
            row.put("professions", snapshot);
            
        }
    }

    public static void login(ServerPlayer player) {
        var data = SmpData.get(player.server);
        data.edit(() -> loginOwned(player));
    }

    private static void loginOwned(ServerPlayer player) {
        var row = get(player.server, player.getUUID());
        row.putString("name", player.getGameProfile().getName());
        
        captureProfessions(player);
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
        var data = SmpData.get(server);
        data.edit(() -> countOwned(server, id, key));
    }

    private static void countOwned(net.minecraft.server.MinecraftServer server, UUID id, String key) {
        var row = get(server, id);
        row.putLong(key, row.getLong(key) + 1);
        
    }

    public static void countEvent(net.minecraft.server.MinecraftServer server, UUID id, String type) {
        var data = SmpData.get(server);
        data.edit(() -> countEventOwned(server, id, type));
    }

    private static void countEventOwned(net.minecraft.server.MinecraftServer server, UUID id, String type) {
        var row = get(server, id);
        var statistics = row.getCompound("eventStats");
        statistics.putLong(type, statistics.getLong(type) + 1);
        row.put("eventStats", statistics);
        
    }

    public static CompoundTag view(ServerPlayer viewer, UUID id) {
        var source = SmpData.get(viewer.server).profiles().snapshot(id);
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
                        "fishingMilestones",
                        "rewardAvailability")) {
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
        if (DomainFeatures.loaded("jem_roles")) RacesAdapter.project(viewer, id, row);
        var professions = new net.minecraft.nbt.ListTag();
        if (net.minecraftforge.fml.ModList.get().isLoaded("jem_professions")) {
            professions = source.getList("professions", net.minecraft.nbt.Tag.TAG_COMPOUND).copy();
            if (player != null) {
                professions = com.siirio.jemserver.compat.professions.ProfessionHostAdapter.snapshot(player);
            }
        }
        row.put("professions", professions);
        if (DomainFeatures.loaded("jem_settlements")) com.siirio.jemserver.compat.settlements.SettlementHostAdapter.profile(viewer, id, row);
        return row;
    }

    private static final class RacesAdapter {
        static void project(ServerPlayer viewer, UUID id, CompoundTag row) {
            com.siirio.jemroles.api.RaceApi.get(viewer.server, id).ifPresent(race -> row.putString("race", race.race().name()));
        }
    }


}
