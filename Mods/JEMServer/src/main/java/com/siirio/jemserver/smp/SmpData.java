package com.siirio.jemserver.smp;

import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.*;

public final class SmpData extends SavedData {
    private static final String NAME = "jem_smp_v2";
    private final CompoundTag unknownTables = new CompoundTag();
    private final Set<String> presentTables = new HashSet<>();
    private CompoundTag unreadableData;
    private RuntimeException loadFailure;
    private long revision;
    private CompoundTag futureData;
    private MinecraftServer server;

    public static SmpData get(MinecraftServer server) {
        var data =
                server.overworld()
                        .getDataStorage()
                        .computeIfAbsent(SmpData::load, SmpData::new, NAME);
        if (data.futureData != null) throw new IllegalStateException("future_data_version");
        if (data.loadFailure != null) throw new IllegalStateException("SMP saved data could not be decoded", data.loadFailure);
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

    private boolean editing;
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
        boolean initialized = false;
        try {
            if (outer) { editing = true; parties.beginEdit(); profiles.beginEdit(); events.beginEdit(); contracts.beginEdit(); shops.beginEdit(); settings.beginEdit(); bossQuarantine.beginEdit(); }
            initialized = true;
            return operation.get();
        }
        finally {
            editDepth--;
            if (outer) {
                var notifications = List.copyOf(afterEdit);
                afterEdit.clear();
                try { if (initialized) checkpoint(); }
                finally { parties.endEdit(); profiles.endEdit(); events.endEdit(); contracts.endEdit(); shops.endEdit(); settings.endEdit(); bossQuarantine.endEdit(); editing = false; }
                notifications.forEach(Runnable::run);
            }
        }
    }

    public void afterEdit(Runnable notification) {
        if (!editing) notification.run();
        else afterEdit.add(notification);
    }

    public SmpRecord view(SmpTable table, UUID id) {
        return switch (table) {
            case PARTIES -> parties.snapshot(id);
            case EVENTS -> events.snapshot(id);
            case PROFILES -> profiles.snapshot(id);
            default -> null;
        };
    }

    private void checkpoint() {
        if (!editing) return;
        changed(parties.checkpoint()); changed(profiles.checkpoint()); changed(events.checkpoint());
        changed(contracts.checkpoint()); changed(shops.checkpoint()); changed(settings.checkpoint()); changed(bossQuarantine.checkpoint());
    }

    private void changed(int count) { if (count > 0) { revision += count; setDirty(); } }

    public boolean inCommand() { return editing && editDepth > 0; }

    public long revision() { return revision; }

    public static boolean closed(SmpRecord row) { return row.closed(); }

    private static SmpData load(CompoundTag root) {
        var data = new SmpData();
        if (root.getInt("dataVersion") > 1) {
            data.futureData = root.copy();
            return data;
        }
        try {
        var tables = root.getCompound("tables");
        data.presentTables.addAll(tables.getAllKeys());
        data.parties.read(tables.getList("parties", Tag.TAG_COMPOUND));
        data.profiles.read(tables.getList("profiles", Tag.TAG_COMPOUND));
        data.events.read(tables.getList("events", Tag.TAG_COMPOUND));
        data.contracts.read(tables.getList("contracts", Tag.TAG_COMPOUND));
        data.shops.read(tables.getList("shops", Tag.TAG_COMPOUND));
        data.settings.read(tables.getList("settings", Tag.TAG_COMPOUND));
        data.bossQuarantine.read(tables.getList("boss_quarantine", Tag.TAG_COMPOUND));
        for (String key : tables.getAllKeys()) if (!Set.of("parties", "profiles", "events", "contracts", "shops", "settings", "boss_quarantine").contains(key)) data.unknownTables.put(key, tables.get(key).copy());
        data.revision = root.getLong("revision");
        } catch (RuntimeException failure) { data.unreadableData = root.copy(); data.loadFailure = failure; }
        return data;
    }

    private void writeTable(CompoundTag out, String name, ListTag rows) {
        if (presentTables.contains(name) || !rows.isEmpty()) { out.put(name, rows); presentTables.add(name); }
    }

    @Override
    public CompoundTag save(CompoundTag root) {
        if (futureData != null) return futureData.copy();
        if (unreadableData != null) return unreadableData.copy();
        checkpoint();
        root.putInt("dataVersion", 1);
        root.putLong("revision", revision);
        var out = unknownTables.copy();
        writeTable(out, "parties", parties.write()); writeTable(out, "profiles", profiles.write());
        writeTable(out, "events", events.write()); writeTable(out, "contracts", contracts.write());
        writeTable(out, "shops", shops.write()); writeTable(out, "settings", settings.write());
        writeTable(out, "boss_quarantine", bossQuarantine.write());
        root.put("tables", out);
        return root;
    }
}
