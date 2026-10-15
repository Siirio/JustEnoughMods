package com.siirio.jemsettlements.town;

import com.siirio.jemsettlements.workforce.WorkforceRecord;
import com.siirio.jemsettlements.resident.ResidentMood;
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
    private final Map<UUID, ResidentMood> residents = new LinkedHashMap<>();

    public static SettlementSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(SettlementSavedData::load, SettlementSavedData::new, NAME);
    }

    public Optional<TownRecord> ownedBy(UUID owner) { return towns.values().stream().filter(town -> town.owner().equals(owner)).findFirst(); }
    public java.util.List<TownRecord> ownedTowns(UUID owner) { return towns.values().stream().filter(town -> town.owner().equals(owner)).toList(); }
    public Optional<TownRecord> town(UUID id) { return Optional.ofNullable(towns.get(id)); }
    public Collection<TownRecord> towns() { return java.util.List.copyOf(towns.values()); }
    public Collection<WorkforceRecord> workforce(UUID town) { return workforce.values().stream().filter(record -> record.townId().equals(town)).toList(); }
    public Optional<WorkforceRecord> workforceEntity(UUID entity) { return Optional.ofNullable(workforce.get(entity)); }
    public Collection<ResidentMood> residents(UUID town) { return residents.values().stream().filter(record -> record.townId().equals(town)).toList(); }
    public Optional<ResidentMood> resident(UUID villager) { return Optional.ofNullable(residents.get(villager)); }
    public boolean nameUsed(String normalized) { return towns.values().stream().anyMatch(town -> town.name().equalsIgnoreCase(normalized)); }
    public boolean anchorWithin(net.minecraft.resources.ResourceLocation dimension, net.minecraft.core.BlockPos center, int distance) {
        long squared = (long) distance * distance;
        return towns.values().stream().anyMatch(town -> town.dimension().equals(dimension) && town.center().distSqr(center) < squared);
    }
    public long workforceCount(UUID town) { return workforce.values().stream().filter(record -> record.townId().equals(town)).count(); }
    public boolean entityAssigned(UUID entity) { return workforce.containsKey(entity); }
    public boolean residentAssigned(UUID entity) { return residents.containsKey(entity); }
    public void add(TownRecord town) { towns.put(town.id(), town); setDirty(); }
    public void add(WorkforceRecord record) { workforce.put(record.entityId(), record); setDirty(); }
    public void add(ResidentMood resident) { residents.put(resident.villagerId(), resident); setDirty(); }
    public boolean removeResident(UUID town, UUID entity) { ResidentMood value = residents.get(entity); if (value == null || !value.townId().equals(town)) return false; residents.remove(entity); setDirty(); return true; }
    public boolean removeWorkforce(UUID town, UUID entity) { WorkforceRecord value = workforce.get(entity); if (value == null || !value.townId().equals(town)) return false; workforce.remove(entity); setDirty(); return true; }
    public void changed() { setDirty(); }

    @Override
    public CompoundTag save(CompoundTag tag) {
        CompoundTag townRows = new CompoundTag();
        towns.forEach((id, town) -> townRows.put(id.toString(), town.save()));
        CompoundTag workerRows = new CompoundTag();
        workforce.forEach((id, worker) -> workerRows.put(id.toString(), worker.save()));
        tag.put("Towns", townRows);
        tag.put("Workforce", workerRows);
        CompoundTag residentRows = new CompoundTag();
        residents.forEach((id, resident) -> residentRows.put(id.toString(), resident.save()));
        tag.put("Residents", residentRows);
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
        CompoundTag residents = tag.getCompound("Residents");
        for (String key : residents.getAllKeys()) {
            try { ResidentMood resident = ResidentMood.load(residents.getCompound(key)); data.residents.put(resident.villagerId(), resident); }
            catch (RuntimeException ignored) { }
        }
        return data;
    }
}
