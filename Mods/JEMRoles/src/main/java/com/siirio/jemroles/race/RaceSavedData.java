package com.siirio.jemroles.race;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

public final class RaceSavedData extends SavedData {
    private static final String NAME = "jem_roles";
    private final Map<UUID, RaceRecord> records = new LinkedHashMap<>();
    private final CompoundTag quarantine = new CompoundTag();

    public static RaceSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(RaceSavedData::load, RaceSavedData::new, NAME);
    }

    public Optional<RaceRecord> find(UUID id) { return Optional.ofNullable(records.get(id)); }
    public void put(UUID id, RaceRecord record) { records.put(id, record); setDirty(); }
    public void changed() { setDirty(); }

    @Override
    public CompoundTag save(CompoundTag tag) {
        CompoundTag rows = new CompoundTag();
        records.forEach((id, record) -> rows.put(id.toString(), record.save()));
        tag.put("Records", rows);
        tag.put("Quarantine", quarantine.copy());
        return tag;
    }

    private static RaceSavedData load(CompoundTag tag) {
        RaceSavedData data = new RaceSavedData();
        CompoundTag rows = tag.getCompound("Records");
        for (String key : rows.getAllKeys()) {
            CompoundTag row = rows.getCompound(key);
            try {
                data.records.put(UUID.fromString(key), RaceRecord.load(row));
            } catch (RuntimeException invalid) {
                data.quarantine.put(key, row.copy());
            }
        }
        if (tag.contains("Quarantine", Tag.TAG_COMPOUND)) {
            CompoundTag old = tag.getCompound("Quarantine");
            old.getAllKeys().forEach(key -> data.quarantine.put(key, old.get(key).copy()));
        }
        return data;
    }
}
