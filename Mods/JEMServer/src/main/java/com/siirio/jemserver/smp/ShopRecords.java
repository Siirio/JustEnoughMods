package com.siirio.jemserver.smp;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;

/** Shop persistence boundary. Working records exist only inside an owning command scope. */
public final class ShopRecords {
    public CompoundTag bind(CompoundTag record) {
        if (record == null) return null;
        SmpRecords.require(record.hasUUID("id"), "unavailable");
        CompoundTag current = current(record.getUUID("id"));
        SmpRecords.require(current != null, "unavailable");
        return current;
    }
    private final SmpData data;
    ShopRecords(SmpData data) { this.data = data; }
    public CompoundTag snapshot(UUID id) { return data.snapshot("shops", id); }
    public List<CompoundTag> snapshots() { return data.snapshots("shops"); }
    public CompoundTag current(UUID id) { return data.current("shops", id); }
    public Collection<CompoundTag> currentRows() { return data.currentRows("shops"); }
    public void put(CompoundTag row) { data.put("shops", row); }
    public CompoundTag remove(UUID id) { return data.remove("shops", id); }
}
