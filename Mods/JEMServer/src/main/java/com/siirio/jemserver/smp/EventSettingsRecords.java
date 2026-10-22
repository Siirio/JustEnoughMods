package com.siirio.jemserver.smp;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;

/** settings persistence boundary. Working records exist only inside an owning command scope. */
public final class EventSettingsRecords {
    public CompoundTag bind(CompoundTag record) {
        if (record == null) return null;
        SmpRecords.require(record.hasUUID("id"), "unavailable");
        CompoundTag current = current(record.getUUID("id"));
        SmpRecords.require(current != null, "unavailable");
        return current;
    }
    private final SmpData data;
    EventSettingsRecords(SmpData data) { this.data = data; }
    public CompoundTag snapshot(UUID id) { return data.snapshot("settings", id); }
    public List<CompoundTag> snapshots() { return data.snapshots("settings"); }
    public CompoundTag current(UUID id) { return data.current("settings", id); }
    public Collection<CompoundTag> currentRows() { return data.currentRows("settings"); }
    public CompoundTag create(UUID owner) { return data.create("settings", owner); }
}
