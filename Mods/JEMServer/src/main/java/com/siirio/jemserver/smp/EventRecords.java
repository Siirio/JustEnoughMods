package com.siirio.jemserver.smp;

import java.util.*;

/** Owns canonical event aggregates; snapshots are recursively immutable. */
public final class EventRecords {
    private final SmpData storage;
    private final java.util.LinkedHashMap<UUID, EventState> saved = new java.util.LinkedHashMap<>();
    private java.util.LinkedHashMap<UUID, EventState> working;
    private boolean editing;
    EventRecords(SmpData storage) { this.storage = storage; }
    void beginEdit() { editing = true; }
    void endEdit() { working = null; editing = false; }
    private java.util.LinkedHashMap<UUID, EventState> working() {
        if (!editing) throw new IllegalStateException("Mutation requires an SMP command");
        if (working == null) { working = new java.util.LinkedHashMap<>(); saved.forEach((id, row) -> working.put(id, row.copy())); }
        return working;
    }
    public EventState snapshot(UUID id) { var row = (working == null ? saved : working).get(id); return row == null ? null : row.snapshot(); }
    public java.util.List<EventState> snapshots() { return (working == null ? saved : working).values().stream().map(EventState::snapshot).toList(); }
    public EventState current(UUID id) { return working().get(id); }
    public java.util.Collection<EventState> currentRows() { return java.util.List.copyOf(working().values()); }
    public EventState bind(EventState row) {
        if (row == null) return null;
        var current = current(row.id()); SmpRecords.require(current != null, "unavailable"); return current;
    }
    void read(net.minecraft.nbt.ListTag rows) { for (var tag : rows) { var row = EventNbtCodec.decodeEventState((net.minecraft.nbt.CompoundTag) tag); if (row.id() == null || saved.containsKey(row.id())) throw new IllegalArgumentException("Invalid or duplicate event identity"); saved.put(row.id(), row); } }
    net.minecraft.nbt.ListTag write() { var rows = new net.minecraft.nbt.ListTag(); saved.values().forEach(row -> rows.add(EventNbtCodec.encode(row))); return rows; }
    int checkpoint() {
        if (working == null) return 0;
        int changes = 0;
        for (var row : working.values()) {
            var previous = saved.get(row.id());
            if (previous != null && EventNbtCodec.encode(row).equals(EventNbtCodec.encode(previous))) continue;
            row.revision(previous == null ? Math.max(1, row.revision() + 1) : previous.revision() + 1);
            saved.put(row.id(), row.copy()); changes++;
        }
        for (UUID id : java.util.List.copyOf(saved.keySet())) if (!working.containsKey(id)) { saved.remove(id); changes++; }
        return changes;
    }
    public EventState create(UUID owner) {
        var row = new EventState(); row.id(UUID.randomUUID()); row.owner(owner); row.revision(0);
        row.created(System.currentTimeMillis()); row.state("DRAFT"); working().put(row.id(), row); return row;
    }
    public void prune() {
        int excess = working().size() - SmpConfig.HISTORY.get();
        var iterator = working().values().iterator();
        while (iterator.hasNext() && excess > 0) {
            var row = iterator.next();
            if (row.closed() && !com.siirio.jemserver.smp.events.BloodMoonDeaths.hasRecovery(row) && !com.siirio.jemserver.smp.events.StructureStaging.hasPendingRaidExit(row)) { iterator.remove(); excess--; }
        }
    }

    public void replace(EventState row) { SmpRecords.require(current(row.id()) != null, "unavailable"); working().put(row.id(), row.copy()); }
}
