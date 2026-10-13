package com.siirio.jemsettlements.town;

import com.siirio.jemsettlements.workforce.WorkforceRecord;
import java.util.LinkedHashMap;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

public final class SettlementSavedData extends SavedData {
    private static final String NAME = "jem_settlements";
    private final Map<UUID, TownRecord> towns = new LinkedHashMap<>();
    private final Map<UUID, WorkforceRecord> workforce = new LinkedHashMap<>();

    public static SettlementSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(SettlementSavedData::load, SettlementSavedData::new, NAME);
    }

    public Optional<TownRecord> ownedBy(UUID owner) { return towns.values().stream().filter(town -> town.owner().equals(owner)).findFirst(); }
    public Optional<TownRecord> town(UUID id) { return Optional.ofNullable(towns.get(id)); }
    public Collection<TownRecord> towns() { return java.util.List.copyOf(towns.values()); }
    public Collection<WorkforceRecord> workforce(UUID town) { return workforce.values().stream().filter(record -> record.townId().equals(town)).toList(); }
    public boolean nameUsed(String normalized) { return towns.values().stream().anyMatch(town -> town.name().equalsIgnoreCase(normalized)); }
    public boolean anchorWithin(net.minecraft.resources.ResourceLocation dimension, net.minecraft.core.BlockPos center, int distance) {
        long squared = (long) distance * distance;
        return towns.values().stream().anyMatch(town -> town.dimension().equals(dimension) && town.center().distSqr(center) < squared);
    }
    public long workforceCount(UUID town) { return workforce.values().stream().filter(record -> record.townId().equals(town)).count(); }
    public boolean entityAssigned(UUID entity) { return workforce.containsKey(entity); }
    public void add(TownRecord town) { towns.put(town.id(), town); setDirty(); }
    public void add(WorkforceRecord record) { workforce.put(record.entityId(), record); setDirty(); }
    public void changed() { setDirty(); }

    @Override
    public CompoundTag save(CompoundTag tag) {
        CompoundTag townRows = new CompoundTag();
        towns.forEach((id, town) -> townRows.put(id.toString(), town.save()));
        CompoundTag workerRows = new CompoundTag();
        workforce.forEach((id, worker) -> workerRows.put(id.toString(), worker.save()));
        tag.put("Towns", townRows);
        tag.put("Workforce", workerRows);
        return tag;
    }

    private static SettlementSavedData load(CompoundTag tag) {
        SettlementSavedData data = new SettlementSavedData();
        CompoundTag towns = tag.getCompound("Towns");
        for (String key : towns.getAllKeys()) {
            try { TownRecord town = TownRecord.load(towns.getCompound(key)); data.towns.put(town.id(), town); }
            catch (RuntimeException ignored) { }
        }
        CompoundTag workers = tag.getCompound("Workforce");
        for (String key : workers.getAllKeys()) {
            try { WorkforceRecord worker = WorkforceRecord.load(workers.getCompound(key)); data.workforce.put(worker.entityId(), worker); }
            catch (RuntimeException ignored) { }
        }
        return data;
    }
}
