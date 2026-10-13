package com.siirio.jemprofessions.progress;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

public final class ProfessionSavedData extends SavedData {
    private static final String NAME = "jem_professions";
    private final Map<UUID, ProfessionRecord> records = new LinkedHashMap<>();

    public static ProfessionSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(ProfessionSavedData::load, ProfessionSavedData::new, NAME);
    }

    public ProfessionRecord record(UUID id) { return records.computeIfAbsent(id, ignored -> new ProfessionRecord()); }
    public ProfessionRecord findOrEmpty(UUID id) { return records.getOrDefault(id, new ProfessionRecord()); }
    public void changed() { setDirty(); }

    @Override
    public CompoundTag save(CompoundTag tag) {
        CompoundTag rows = new CompoundTag();
        records.forEach((id, record) -> rows.put(id.toString(), record.save()));
        tag.put("Records", rows);
        return tag;
    }

    private static ProfessionSavedData load(CompoundTag tag) {
        ProfessionSavedData data = new ProfessionSavedData();
        CompoundTag rows = tag.getCompound("Records");
        for (String key : rows.getAllKeys()) {
            try { data.records.put(UUID.fromString(key), ProfessionRecord.load(rows.getCompound(key))); }
            catch (RuntimeException ignored) { }
        }
        return data;
    }
}
