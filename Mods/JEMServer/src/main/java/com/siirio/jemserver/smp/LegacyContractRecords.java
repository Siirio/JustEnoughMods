package com.siirio.jemserver.smp;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

/** The recovery owner never exposes a persistent mutable contract. */
public final class LegacyContractRecords {
    private final LinkedHashMap<UUID, LegacyContractState> saved = new LinkedHashMap<>();
    private LinkedHashMap<UUID, LegacyContractState> editing;
    private boolean active;
    LegacyContractRecords(SmpData data) {}
    void read(ListTag rows) {
        for (var tag : rows) {
            var row = LegacyContractState.decode((CompoundTag) tag);
            if (row.id() != null) saved.put(row.id(), row);
        }
    }
    ListTag write() {
        var result = new ListTag();
        saved.values().forEach(row -> result.add(row.encode()));
        return result;
    }
    void beginEdit() { active = true; }
    int checkpoint() {
        if (editing == null) return 0;
        int changed = 0;
        for (var row : editing.values()) {
            var previous = saved.get(row.id());
            if (previous == null || !previous.encode().equals(row.encode())) {
                row.revision((previous == null ? 0 : previous.revision()) + 1);
                saved.put(row.id(), row.copy());
                changed++;
            }
        }
        return changed;
    }
    void endEdit() { active = false; editing = null; }
    private LinkedHashMap<UUID, LegacyContractState> current() {
        if (!active) throw new IllegalStateException("Contract recovery requires an owning command scope");
        if (editing == null) {
            editing = new LinkedHashMap<>();
            saved.forEach((id, row) -> editing.put(id, row.copy()));
        }
        return editing;
    }
    public LegacyContractState bind(LegacyContractState record) {
        if (record == null) return null;
        var row = current(record.id());
        SmpRecords.require(row != null, "unavailable");
        return row;
    }
    public LegacyContractState snapshot(UUID id) {
        var row = (editing == null ? saved : editing).get(id);
        return row == null ? null : row.snapshot();
    }
    public List<LegacyContractState> snapshots() { return (editing == null ? saved : editing).values().stream().map(LegacyContractState::snapshot).toList(); }
    public LegacyContractState current(UUID id) { return current().get(id); }
    public Collection<LegacyContractState> currentRows() { return List.copyOf(current().values()); }
}
