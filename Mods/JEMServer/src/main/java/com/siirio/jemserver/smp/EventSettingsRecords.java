package com.siirio.jemserver.smp;

import java.util.*;
import net.minecraft.nbt.*;

/** Owns the persisted schedule override; reads never open an edit scope. */
public final class EventSettingsRecords {
    private final LinkedHashMap<UUID, EventScheduleState> saved = new LinkedHashMap<>();
    private LinkedHashMap<UUID, EventScheduleState> editing;
    private boolean active;
    EventSettingsRecords(SmpData data) {}
    void beginEdit() { active = true; }
    void endEdit() { active = false; editing = null; }
    private Map<UUID, EventScheduleState> working() {
        if (!active) throw new IllegalStateException("Schedule changes require an owning command scope");
        if (editing == null) editing = new LinkedHashMap<>(saved);
        return editing;
    }
    public EventScheduleState schedule() {
        return (editing == null ? saved : editing).values().stream()
                .filter(row -> row.kind().equals("event_schedule")).findFirst().orElse(null);
    }
    public void replaceSchedule(List<String> definitions) {
        var rows = working(); var row = schedule();
        if (row == null) row = EventScheduleState.create(new UUID(0, 0));
        rows.put(row.id(), row.withEntries(definitions));
    }
    public void resetSchedule() {
        var rows = working(); var row = schedule();
        if (row != null) rows.put(row.id(), row.withEntries(null));
    }
    int checkpoint() {
        if (editing == null) return 0;
        int changes = 0;
        for (var row : List.copyOf(editing.values())) {
            var prior = saved.get(row.id());
            if (prior != null && prior.encode().equals(row.encode())) continue;
            var updated = row.withRevision(prior == null ? Math.max(1, row.revision() + 1) : prior.revision() + 1);
            editing.put(row.id(), updated); saved.put(row.id(), updated); changes++;
        }
        return changes;
    }
    void read(ListTag rows) { for (var value : rows) { var row = EventScheduleState.decode((CompoundTag) value); if (row.id() != null) saved.put(row.id(), row); } }
    ListTag write() { var result = new ListTag(); saved.values().forEach(row -> result.add(row.encode())); return result; }
}
