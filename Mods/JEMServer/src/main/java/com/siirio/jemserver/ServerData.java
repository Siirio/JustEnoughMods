package com.siirio.jemserver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

public final class ServerData extends SavedData {
    public record Death(UUID id, ResourceLocation dimension, double x, double y, double z, float yaw, float pitch, long timestamp, String cause) {}
    public record Landmark(UUID id, String name, String nameKey, String category, ResourceLocation dimension, BlockPos position, UUID creator, String creatorName, long createdAt) {}
    public record Hunter(UUID id, String name, Map<ResourceLocation, Long> kills) {
        public long total() { return kills.values().stream().mapToLong(Long::longValue).sum(); }
        public int unique() { return kills.size(); }
    }

    private final Map<UUID, List<Death>> deaths = new HashMap<>();
    private final Map<UUID, Map<ResourceLocation, Long>> kills = new HashMap<>();
    private final Map<UUID, String> names = new HashMap<>();
    private final Map<UUID, Landmark> accepted = new LinkedHashMap<>();

    public static ServerData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(ServerData::load, ServerData::new, "jem_server");
    }

    public List<Death> deaths(UUID player) { return List.copyOf(deaths.getOrDefault(player, List.of())); }
    public List<Landmark> landmarks() { return List.copyOf(accepted.values()); }
    public List<Hunter> hunters() {
        return kills.entrySet().stream().map(entry -> new Hunter(entry.getKey(), names.getOrDefault(entry.getKey(), entry.getKey().toString()), Map.copyOf(entry.getValue()))).toList();
    }

    public void addDeath(UUID player, Death death) {
        List<Death> history = deaths.computeIfAbsent(player, ignored -> new ArrayList<>());
        history.add(0, death);
        while (history.size() > ServerConfig.HISTORY_SIZE.get()) history.remove(history.size() - 1);
        setDirty();
    }

    public void recordKill(UUID player, String name, ResourceLocation boss) {
        Map<ResourceLocation, Long> counts = kills.computeIfAbsent(player, ignored -> new HashMap<>());
        long previous = counts.getOrDefault(boss, 0L);
        if (previous == Long.MAX_VALUE) return;
        counts.put(boss, previous + 1);
        names.put(player, name);
        setDirty();
    }

    public boolean resetKills(UUID player) {
        if (kills.remove(player) == null) return false;
        names.remove(player);
        setDirty();
        return true;
    }

    public boolean publish(Landmark landmark) {
        if (accepted.containsKey(landmark.id()) || accepted.size() >= ServerConfig.MAX_LANDMARKS.get()
                || accepted.values().stream().filter(value -> value.creator().equals(landmark.creator())).count() >= ServerConfig.MAX_PUBLIC_PER_PLAYER.get()) return false;
        accepted.put(landmark.id(), landmark);
        setDirty();
        return true;
    }

    public boolean publishSystem(Landmark landmark) {
        if (accepted.containsKey(landmark.id())) return false;
        accepted.put(landmark.id(), landmark);
        setDirty();
        return true;
    }

    public boolean removeLandmark(UUID id) {
        if (accepted.remove(id) == null) return false;
        setDirty();
        return true;
    }

    private static ServerData load(CompoundTag root) {
        ServerData data = new ServerData();
        CompoundTag histories = root.getCompound("Deaths");
        for (String player : histories.getAllKeys()) {
            List<Death> records = new ArrayList<>();
            for (Tag raw : histories.getList(player, Tag.TAG_COMPOUND)) {
                CompoundTag tag = (CompoundTag) raw;
                records.add(new Death(tag.getUUID("Id"), new ResourceLocation(tag.getString("Dimension")), tag.getDouble("X"), tag.getDouble("Y"), tag.getDouble("Z"), tag.getFloat("Yaw"), tag.getFloat("Pitch"), tag.getLong("Time"), tag.getString("Cause")));
            }
            data.deaths.put(UUID.fromString(player), records);
        }
        CompoundTag hunters = root.getCompound("Hunters");
        for (String player : hunters.getAllKeys()) {
            UUID id = UUID.fromString(player);
            CompoundTag hunter = hunters.getCompound(player);
            data.names.put(id, hunter.getString("Name"));
            Map<ResourceLocation, Long> counts = new HashMap<>();
            CompoundTag entries = hunter.getCompound("Kills");
            for (String boss : entries.getAllKeys()) if (entries.getLong(boss) > 0) counts.put(new ResourceLocation(boss), entries.getLong(boss));
            data.kills.put(id, counts);
        }
        readLandmarks(root.getList("Landmarks", Tag.TAG_COMPOUND), data.accepted);
        readLandmarks(root.getList("Pending", Tag.TAG_COMPOUND), data.accepted);
        return data;
    }

    private static void readLandmarks(ListTag list, Map<UUID, Landmark> destination) {
        for (Tag raw : list) {
            CompoundTag tag = (CompoundTag) raw;
            Landmark landmark = new Landmark(tag.getUUID("Id"), tag.getString("Name"), tag.getString("NameKey"), tag.getString("Category"), new ResourceLocation(tag.getString("Dimension")), BlockPos.of(tag.getLong("Position")), tag.getUUID("Creator"), tag.getString("CreatorName"), tag.getLong("Time"));
            destination.put(landmark.id(), landmark);
        }
    }

    @Override
    public CompoundTag save(CompoundTag root) {
        CompoundTag histories = new CompoundTag();
        deaths.forEach((player, records) -> {
            ListTag list = new ListTag();
            for (Death death : records) {
                CompoundTag tag = new CompoundTag();
                tag.putUUID("Id", death.id());
                tag.putString("Dimension", death.dimension().toString());
                tag.putDouble("X", death.x());
                tag.putDouble("Y", death.y());
                tag.putDouble("Z", death.z());
                tag.putFloat("Yaw", death.yaw());
                tag.putFloat("Pitch", death.pitch());
                tag.putLong("Time", death.timestamp());
                tag.putString("Cause", death.cause());
                list.add(tag);
            }
            histories.put(player.toString(), list);
        });
        root.put("Deaths", histories);
        CompoundTag hunters = new CompoundTag();
        kills.forEach((player, counts) -> {
            CompoundTag hunter = new CompoundTag();
            hunter.putString("Name", names.getOrDefault(player, player.toString()));
            CompoundTag entries = new CompoundTag();
            counts.forEach((boss, count) -> entries.putLong(boss.toString(), count));
            hunter.put("Kills", entries);
            hunters.put(player.toString(), hunter);
        });
        root.put("Hunters", hunters);
        root.put("Landmarks", writeLandmarks(accepted));
        return root;
    }

    private static ListTag writeLandmarks(Map<UUID, Landmark> landmarks) {
        ListTag list = new ListTag();
        for (Landmark landmark : landmarks.values()) {
            CompoundTag tag = new CompoundTag();
            tag.putUUID("Id", landmark.id());
            tag.putString("Name", landmark.name());
            tag.putString("NameKey", landmark.nameKey());
            tag.putString("Category", landmark.category());
            tag.putString("Dimension", landmark.dimension().toString());
            tag.putLong("Position", landmark.position().asLong());
            tag.putUUID("Creator", landmark.creator());
            tag.putString("CreatorName", landmark.creatorName());
            tag.putLong("Time", landmark.createdAt());
            list.add(tag);
        }
        return list;
    }
}
