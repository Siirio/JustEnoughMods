package com.siirio.jemprofessions.progress;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

public final class ProfessionSavedData extends SavedData {
    private static final String NAME = "jem_professions";
    private final Map<UUID, ProfessionRecord> records = new LinkedHashMap<>();
    private final ArrayDeque<String> placedOrder = new ArrayDeque<>();
    private final Set<String> placedBlocks = new HashSet<>();
    private static final int MAX_PLACED = 65536;

    public static ProfessionSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(ProfessionSavedData::load, ProfessionSavedData::new, NAME);
    }

    public ProfessionRecord record(UUID id) { return records.computeIfAbsent(id, ignored -> new ProfessionRecord()); }
    public ProfessionRecord findOrEmpty(UUID id) { return records.getOrDefault(id, new ProfessionRecord()); }
    public void changed() { setDirty(); }
    public void markPlayerPlaced(String key) {
        if (key == null || key.length() > 192 || !placedBlocks.add(key)) return;
        placedOrder.addLast(key);
        while (placedOrder.size() > MAX_PLACED) placedBlocks.remove(placedOrder.removeFirst());
        setDirty();
    }
    public boolean wasPlayerPlaced(String key) { return placedBlocks.contains(key); }
    public void removePlaced(String key) { if (placedBlocks.remove(key)) { placedOrder.remove(key); setDirty(); } }

    @Override
    public CompoundTag save(CompoundTag tag) {
        CompoundTag rows = new CompoundTag();
        records.forEach((id, record) -> rows.put(id.toString(), record.save()));
        tag.put("Records", rows);
        ListTag placed = new ListTag();
        placedOrder.forEach(value -> placed.add(StringTag.valueOf(value)));
        tag.put("PlayerPlaced", placed);
        return tag;
    }

    private static ProfessionSavedData load(CompoundTag tag) {
        ProfessionSavedData data = new ProfessionSavedData();
        CompoundTag rows = tag.getCompound("Records");
        for (String key : rows.getAllKeys()) {
            try { data.records.put(UUID.fromString(key), ProfessionRecord.load(rows.getCompound(key))); }
            catch (RuntimeException ignored) { }
        }
        ListTag placed = tag.getList("PlayerPlaced", Tag.TAG_STRING);
        for (int i = Math.max(0, placed.size() - MAX_PLACED); i < placed.size(); i++) {
            String key = placed.getString(i);
            if (data.placedBlocks.add(key)) data.placedOrder.addLast(key);
        }
        return data;
    }
}
