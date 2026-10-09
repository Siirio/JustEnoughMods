package com.siirio.jemserver.smp;

import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.*;

public final class SmpData extends SavedData {
    private static final String NAME = "jem_smp_v2";
    private final Map<String, LinkedHashMap<UUID, CompoundTag>> tables = new HashMap<>();
    private long revision;
    private CompoundTag futureData;

    public static SmpData get(MinecraftServer server) {
        var data =
                server.overworld()
                        .getDataStorage()
                        .computeIfAbsent(SmpData::load, SmpData::new, NAME);
        if (data.futureData != null) throw new IllegalStateException("future_data_version");
        return data;
    }

    public void flush(MinecraftServer server) {
        var file =
                server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)
                        .resolve("data")
                        .resolve(NAME + ".dat")
                        .toFile();
        save(file);
        try {
            var saved = NbtIo.readCompressed(file).getCompound("data");
            if (saved.getLong("revision") != revision)
                throw new java.io.IOException("Revision not persisted");
        } catch (java.io.IOException failure) {
            setDirty();
            com.mojang.logging.LogUtils.getLogger()
                    .error("Could not verify SMP escrow save", failure);
            throw new IllegalStateException("storage_unavailable");
        }
    }

    public Collection<CompoundTag> all(String table) {
        return tables.computeIfAbsent(table, key -> new LinkedHashMap<>()).values();
    }

    public CompoundTag find(String table, UUID id) {
        var rows = tables.get(table);
        return rows == null ? null : rows.get(id);
    }

    public CompoundTag remove(String table, UUID id) {
        var rows = tables.get(table);
        if (rows == null) return null;
        var removed = rows.remove(id);
        if (removed != null) {
            revision++;
            setDirty();
        }
        return removed;
    }

    public CompoundTag create(String table, UUID owner) {
        var row = new CompoundTag();
        row.putUUID("id", UUID.randomUUID());
        row.putUUID("owner", owner);
        row.putInt("revision", 0);
        row.putLong("created", System.currentTimeMillis());
        row.putString("state", "DRAFT");
        put(table, row);
        return row;
    }

    public void put(String table, CompoundTag row) {
        tables.computeIfAbsent(table, key -> new LinkedHashMap<>()).put(row.getUUID("id"), row);
        changed(row);
    }

    public void changed(CompoundTag row) {
        row.putInt("revision", row.getInt("revision") + 1);
        revision++;
        setDirty();
    }

    public long revision() {
        return revision;
    }

    public void prune(String table) {
        var rows = tables.get(table);
        if (rows == null) return;
        int excess = rows.size() - SmpConfig.HISTORY.get();
        if (excess <= 0) return;
        var iterator = rows.values().iterator();
        while (iterator.hasNext() && excess > 0) {
            var row = iterator.next();
            if (closed(row) && !com.siirio.jemserver.smp.events.BloodMoonDeaths.hasRecovery(row)
                    && !com.siirio.jemserver.smp.events.StructureStaging.hasPendingRaidExit(row)) {
                iterator.remove();
                excess--;
                revision++;
                setDirty();
            }
        }
    }

    public static boolean closed(CompoundTag row) {
        return Set.of("CLOSED", "CANCELLED", "COMPLETED", "FAILED", "SKIPPED")
                .contains(row.getString("state"));
    }

    private static SmpData load(CompoundTag root) {
        var data = new SmpData();
        if (root.getInt("dataVersion") > 1) {
            data.futureData = root.copy();
            return data;
        }
        var tables = root.getCompound("tables");
        for (String name : tables.getAllKeys())
            for (Tag tag : tables.getList(name, Tag.TAG_COMPOUND)) {
                var row = (CompoundTag) tag;
                if (row.hasUUID("id"))
                    data.tables
                            .computeIfAbsent(name, key -> new LinkedHashMap<>())
                            .put(row.getUUID("id"), row);
            }
        data.revision = root.getLong("revision");
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag root) {
        if (futureData != null) return futureData.copy();
        root.putInt("dataVersion", 1);
        root.putLong("revision", revision);
        var out = new CompoundTag();
        tables.forEach(
                (name, rows) -> {
                    var list = new ListTag();
                    rows.values().forEach(list::add);
                    out.put(name, list);
                });
        root.put("tables", out);
        return root;
    }
}
