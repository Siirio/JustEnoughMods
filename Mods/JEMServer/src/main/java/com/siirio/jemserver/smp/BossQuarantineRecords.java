package com.siirio.jemserver.smp;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;

/** boss_quarantine persistence boundary. Working records exist only inside an owning command scope. */
public final class BossQuarantineRecords {
    public CompoundTag bind(CompoundTag record) {
        if (record == null) return null;
        SmpRecords.require(record.hasUUID("id"), "unavailable");
        CompoundTag current = current(record.getUUID("id"));
        SmpRecords.require(current != null, "unavailable");
        return current;
    }
    private final SmpData data;
    BossQuarantineRecords(SmpData data) { this.data = data; }
    public CompoundTag snapshot(UUID id) { return data.snapshot("boss_quarantine", id); }
    public List<CompoundTag> snapshots() { return data.snapshots("boss_quarantine"); }
    public CompoundTag current(UUID id) { return data.current("boss_quarantine", id); }
    public Collection<CompoundTag> currentRows() { return data.currentRows("boss_quarantine"); }
    public void put(CompoundTag row) { data.put("boss_quarantine", row); }
}
