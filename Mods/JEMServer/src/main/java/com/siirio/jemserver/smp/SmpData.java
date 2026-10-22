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
    private MinecraftServer server;

    public static SmpData get(MinecraftServer server) {
        var data =
                server.overworld()
                        .getDataStorage()
                        .computeIfAbsent(SmpData::load, SmpData::new, NAME);
        if (data.futureData != null) throw new IllegalStateException("future_data_version");
        data.server = server;
        return data;
    }

    public void flush(MinecraftServer server) {
        checkpoint();
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

    private Map<String, LinkedHashMap<UUID, CompoundTag>> editing;
    private int editDepth;
    private final List<Runnable> afterEdit = new ArrayList<>();
    private final Parties parties = new Parties(this);
    private final Profiles profiles = new Profiles(this);
    private final EventRecords events = new EventRecords(this);
    private final LegacyContractRecords contracts = new LegacyContractRecords(this);
    private final ShopRecords shops = new ShopRecords(this);
    private final EventSettingsRecords settings = new EventSettingsRecords(this);
    private final BossQuarantineRecords bossQuarantine = new BossQuarantineRecords(this);

    public Parties parties() { return parties; }
    public Profiles profiles() { return profiles; }
    public EventRecords events() { return events; }
    public LegacyContractRecords contracts() { return contracts; }
    public ShopRecords shops() { return shops; }
    public EventSettingsRecords settings() { return settings; }
    public BossQuarantineRecords bossQuarantine() { return bossQuarantine; }

    public void edit(Runnable operation) { edit(() -> { operation.run(); return null; }); }

    /** One reentrant server-thread command owns a detached graph; all changes are committed together. */
    public <T> T edit(java.util.function.Supplier<T> operation) {
        if (server == null || !server.isSameThread()) throw new IllegalStateException("SMP commands require the server thread");
        boolean outer = editDepth++ == 0;
        if (outer) editing = new HashMap<>();
        try { return operation.get(); }
        finally {
            editDepth--;
            if (outer) {
                var notifications = List.copyOf(afterEdit);
                afterEdit.clear();
                try { checkpoint(); }
                finally { editing = null; }
                notifications.forEach(Runnable::run);
            }
        }
    }

    public void afterEdit(Runnable notification) {
        if (editing == null) notification.run();
        else afterEdit.add(notification);
    }

    public CompoundTag view(SmpTable table, UUID id) {
        return switch (table) {
            case PARTIES -> parties.snapshot(id);
            case EVENTS -> events.snapshot(id);
            case PROFILES -> profiles.snapshot(id);
            default -> null;
        };
    }

    public List<CompoundTag> views(SmpTable table) {
        return switch (table) {
            case PARTIES -> parties.snapshots();
            case EVENTS -> events.snapshots();
            case PROFILES -> profiles.snapshots();
            default -> List.of();
        };
    }

    CompoundTag snapshot(String table, UUID id) {
        var rows = editing == null ? tables.get(table) : workingRows(table);
        CompoundTag row = rows == null ? null : rows.get(id);
        return row == null ? null : row.copy();
    }

    List<CompoundTag> snapshots(String table) {
        var rows = editing == null ? tables.get(table) : workingRows(table);
        return rows == null ? List.of() : rows.values().stream().map(CompoundTag::copy).toList();
    }

    CompoundTag current(String table, UUID id) {
        requireEdit();
        var rows = workingRows(table);
        return rows == null ? null : rows.get(id);
    }

    Collection<CompoundTag> currentRows(String table) {
        requireEdit();
        var rows = workingRows(table);
        return rows == null ? List.of() : List.copyOf(rows.values());
    }

    CompoundTag create(String table, UUID owner) {
        requireEdit();
        var row = new CompoundTag();
        row.putUUID("id", UUID.randomUUID());
        row.putUUID("owner", owner);
        row.putInt("revision", 0);
        row.putLong("created", System.currentTimeMillis());
        row.putString("state", "DRAFT");
        put(table, row);
        return row;
    }

    void put(String table, CompoundTag row) {
        requireEdit();
        workingRows(table).put(row.getUUID("id"), row);
    }

    CompoundTag remove(String table, UUID id) {
        requireEdit();
        var rows = workingRows(table);
        return rows == null ? null : rows.remove(id);
    }

    void prune(String table, java.util.function.Predicate<CompoundTag> retained) {
        requireEdit();
        var rows = workingRows(table);
        if (rows == null) return;
        int excess = rows.size() - SmpConfig.HISTORY.get();
        var iterator = rows.values().iterator();
        while (iterator.hasNext() && excess > 0) {
            var row = iterator.next();
            if (closed(row) && !retained.test(row)) { iterator.remove(); excess--; }
        }
    }

    private LinkedHashMap<UUID, CompoundTag> workingRows(String table) {
        requireEdit();
        return editing.computeIfAbsent(table, name -> {
            var copy = new LinkedHashMap<UUID, CompoundTag>();
            var rows = tables.get(name);
            if (rows != null) rows.forEach((id, row) -> copy.put(id, row.copy()));
            return copy;
        });
    }

    private void requireEdit() {
        if (editing == null) throw new IllegalStateException("SMP mutation requires an owning command scope");
    }

    private void checkpoint() {
        if (editing == null) return;
        for (var table : editing.entrySet()) {
            if (table.getValue().isEmpty() && !tables.containsKey(table.getKey())) continue;
            var saved = tables.computeIfAbsent(table.getKey(), ignored -> new LinkedHashMap<>());
            for (var entry : table.getValue().entrySet()) {
                var previous = saved.get(entry.getKey());
                var row = entry.getValue();
                if (row.equals(previous)) continue;
                row.putInt("revision", previous == null ? Math.max(1, row.getInt("revision") + 1) : previous.getInt("revision") + 1);
                saved.put(entry.getKey(), row.copy());
                revision++;
                setDirty();
            }
            for (UUID id : List.copyOf(saved.keySet())) {
                if (!table.getValue().containsKey(id)) { saved.remove(id); revision++; setDirty(); }
            }
        }
    }

    public long revision() { return revision; }

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
        checkpoint();
        root.putInt("dataVersion", 1);
        root.putLong("revision", revision);
        var out = new CompoundTag();
        tables.forEach(
                (name, rows) -> {
                    var list = new ListTag();
                    rows.values().forEach(row -> list.add(row.copy()));
                    out.put(name, list);
                });
        root.put("tables", out);
        return root;
    }
}
