package com.siirio.jemserver.smp;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

/** Owns shop persistence and exposes immutable snapshots outside commands. */
public final class ShopRecords {
    private final LinkedHashMap<UUID, ShopState> saved = new LinkedHashMap<>();
    private LinkedHashMap<UUID, ShopState> editing;
    private boolean active;
    ShopRecords(SmpData data) {}
    public ShopState bind(ShopState row) {
        SmpRecords.require(row != null, "unavailable");
        ShopState current = current(row.id());
        SmpRecords.require(current != null, "unavailable");
        return current;
    }
    public ShopState snapshot(UUID id) {
        ShopState row = (editing == null ? saved : editing).get(id);
        return row == null ? null : row.snapshot();
    }
    public List<ShopState> snapshots() {
        return (editing == null ? saved : editing).values().stream().map(ShopState::snapshot).toList();
    }
    private LinkedHashMap<UUID, ShopState> working() {
        if (!active) throw new IllegalStateException("Shop mutation requires an owning command scope");
        if (editing == null) {
            editing = new LinkedHashMap<>();
            saved.forEach((id, row) -> editing.put(id, row.copy()));
        }
        return editing;
    }
    public ShopState current(UUID id) { return working().get(id); }
    public Collection<ShopState> currentRows() { return List.copyOf(working().values()); }
    public void put(ShopState row) { working().put(row.id(), row.copy()); }
    public ShopState remove(UUID id) { return working().remove(id); }
    void beginEdit() { active = true; }
    void endEdit() { active = false; editing = null; }
    int checkpoint() {
        if (editing == null) return 0;
        int changes = 0;
        for (var entry : editing.entrySet()) {
            ShopState previous = saved.get(entry.getKey()), row = entry.getValue();
            if (row.equals(previous)) continue;
            row.revision(previous == null ? Math.max(1, row.revision() + 1) : previous.revision() + 1);
            saved.put(entry.getKey(), row.copy());
            changes++;
        }
        for (UUID id : List.copyOf(saved.keySet())) {
            if (!editing.containsKey(id)) { saved.remove(id); changes++; }
        }
        return changes;
    }
    void read(ListTag rows) {
        saved.clear();
        rows.forEach(value -> {
            CompoundTag tag = (CompoundTag) value;
            if (!tag.hasUUID("id")) return;
            ShopState row = ShopState.read(tag);
            saved.put(row.id(), row);
        });
    }
    ListTag write() {
        ListTag rows = new ListTag();
        saved.values().forEach(row -> rows.add(row.write()));
        return rows;
    }
}
