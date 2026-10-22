package com.siirio.jemserver.smp;

import java.util.List;
import java.util.UUID;
import net.minecraft.nbt.*;

/** Immutable persisted schedule override; null entries mean configuration defaults. */
public final class EventScheduleState implements SmpRecord {
    private final CompoundTag original;
    private final UUID id, owner;
    private final long created;
    private final int revision;
    private final String state, kind;
    private final List<String> entries;
    private final boolean entriesChanged;
    private EventScheduleState(CompoundTag source, List<String> entries, boolean changed, int revision) {
        original = source.copy();
        id = source.hasUUID("id") ? source.getUUID("id") : null;
        owner = source.hasUUID("owner") ? source.getUUID("owner") : null;
        created = source.getLong("created"); state = source.getString("state"); kind = source.getString("kind");
        this.revision = revision; this.entries = entries == null ? null : List.copyOf(entries); entriesChanged = changed;
    }
    static EventScheduleState decode(CompoundTag source) {
        List<String> entries = source.contains("entries", Tag.TAG_LIST)
                ? source.getList("entries", Tag.TAG_STRING).stream().map(Tag::getAsString).toList() : null;
        return new EventScheduleState(source, entries, false, source.getInt("revision"));
    }
    static EventScheduleState create(UUID owner) {
        var source = new CompoundTag(); source.putUUID("id", UUID.randomUUID()); source.putUUID("owner", owner);
        source.putInt("revision", 0); source.putLong("created", System.currentTimeMillis()); source.putString("state", "DRAFT");
        source.putString("kind", "event_schedule");
        return decode(source);
    }
    CompoundTag encode() {
        var result = original.copy();
        if (revision != original.getInt("revision")) result.putInt("revision", revision);
        if (entriesChanged) {
            if (entries == null) result.remove("entries");
            else { var list = new ListTag(); entries.forEach(value -> list.add(StringTag.valueOf(value))); result.put("entries", list); }
        }
        return result;
    }
    EventScheduleState withEntries(List<String> values) { return new EventScheduleState(original, values, true, revision); }
    EventScheduleState withRevision(int value) { return new EventScheduleState(original, entries, entriesChanged, value); }
    public UUID id() { return id; }
    public UUID owner() { return owner; }
    public long created() { return created; }
    public int revision() { return revision; }
    public String state() { return state; }
    public String kind() { return kind; }
    public boolean hasEntries() { return entries != null; }
    public List<String> entries() { return entries == null ? List.of() : entries; }
}
