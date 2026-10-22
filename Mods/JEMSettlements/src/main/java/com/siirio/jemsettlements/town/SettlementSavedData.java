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
    private final Map<UUID, com.siirio.jemsettlements.payroll.EmployerPayroll> employers = new LinkedHashMap<>();
    private final Map<UUID, ResidentMood> residents = new LinkedHashMap<>();
    // Derived only: ResidentMood membership is immutable, so replacement is the move operation.
    private final Map<UUID, java.util.LinkedHashSet<UUID>> residentsByTown = new LinkedHashMap<>();
    private final net.minecraft.nbt.ListTag quarantine = new net.minecraft.nbt.ListTag();
    private CompoundTag unknownRoot = new CompoundTag();
    private CompoundTag unsupportedRoot;

    public static SettlementSavedData get(MinecraftServer server) {
        SettlementSavedData data = server.overworld().getDataStorage().computeIfAbsent(
                SettlementSavedData::load, SettlementSavedData::new, NAME);
        // Reject outside the native loader: its failure recovery must not substitute a fresh store.
        if (data.unsupportedRoot != null)
            throw new IllegalStateException("Settlement storage format is unsupported; original data retained, settlement access disabled");
        return data;
    }

    public Collection<WorkforceRecord> workforce() { return java.util.List.copyOf(workforce.values()); }
    public com.siirio.jemsettlements.payroll.EmployerPayroll employer(UUID owner) { return employers.computeIfAbsent(owner, id -> new com.siirio.jemsettlements.payroll.EmployerPayroll()); }
    public java.util.Set<UUID> employers() { return java.util.Set.copyOf(employers.keySet()); }
    public Optional<TownRecord> ownedBy(UUID owner) { return towns.values().stream().filter(town -> town.owner().equals(owner)).findFirst(); }
    public java.util.List<TownRecord> ownedTowns(UUID owner) { return towns.values().stream().filter(town -> town.owner().equals(owner)).toList(); }
    public Optional<TownRecord> town(UUID id) { return Optional.ofNullable(towns.get(id)); }
    public Collection<TownRecord> towns() { return java.util.List.copyOf(towns.values()); }
    public Collection<WorkforceRecord> workforce(UUID town) { return workforce.values().stream().filter(record -> record.townId().equals(town)).toList(); }
    public Optional<WorkforceRecord> workforceEntity(UUID entity) { return Optional.ofNullable(workforce.get(entity)); }
    public Collection<ResidentMood> residents(UUID town) {
        var ids = residentsByTown.get(town);
        if (ids == null) return java.util.List.of();
        return ids.stream().map(residents::get).toList();
    }
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
    public void add(ResidentMood resident) {
        indexResident(resident);
        setDirty();
    }

    private void indexResident(ResidentMood resident) {
        ResidentMood previous = residents.put(resident.villagerId(), resident);
        if (previous != null && previous.townId().equals(resident.townId())) return;
        if (previous != null) {
            unindexResident(previous);
            // Replacement retains canonical map insertion order, including moves to another town.
            var ids = new java.util.LinkedHashSet<UUID>();
            for (ResidentMood value : residents.values())
                if (value.townId().equals(resident.townId())) ids.add(value.villagerId());
            residentsByTown.put(resident.townId(), ids);
        } else {
            residentsByTown.computeIfAbsent(resident.townId(), ignored -> new java.util.LinkedHashSet<>()).add(resident.villagerId());
        }
    }

    private void unindexResident(ResidentMood resident) {
        var ids = residentsByTown.get(resident.townId());
        if (ids == null) return;
        ids.remove(resident.villagerId());
        if (ids.isEmpty()) residentsByTown.remove(resident.townId());
    }

    private boolean removeResidentRecord(UUID entity) {
        ResidentMood removed = residents.remove(entity);
        if (removed == null) return false;
        unindexResident(removed);
        return true;
    }
    public boolean removeResident(UUID town, UUID entity) { ResidentMood value = residents.get(entity); if (value == null || !value.townId().equals(town)) return false; removeResidentRecord(entity); setDirty(); return true; }
    public boolean removeWorkforce(UUID town, UUID entity) { WorkforceRecord value = workforce.get(entity); if (value == null || !value.townId().equals(town)) return false; if (value.type() == com.siirio.jemsettlements.workforce.WorkforceType.RECRUIT) return false; workforce.remove(entity); setDirty(); return true; }
    public boolean removeEntity(UUID entity) {
        boolean removed = removeResidentRecord(entity);
        WorkforceRecord worker = workforce.get(entity);
        if (worker != null) { if (worker.type() == com.siirio.jemsettlements.workforce.WorkforceType.RECRUIT) worker.markDead(); else workforce.remove(entity); removed = true; }
        if (removed) setDirty();
        return removed;
    }
    public boolean removeTown(UUID town) {
        if (towns.remove(town) == null) return false;
        var residentIds = residentsByTown.remove(town);
        if (residentIds != null) residentIds.forEach(residents::remove);
        workforce.values().removeIf(value -> value.townId().equals(town) && value.type() != com.siirio.jemsettlements.workforce.WorkforceType.RECRUIT);
        for (WorkforceRecord value : workforce.values()) if (value.townId().equals(town)) {
            value.assignTown(value.ownerId()); employer(value.ownerId());
        }
        setDirty();
        return true;
    }
    public void purgeDeadRecords() {
        boolean changed = false;
        for (var iterator = residents.values().iterator(); iterator.hasNext();) {
            ResidentMood resident = iterator.next();
            if (!resident.living()) {
                iterator.remove();
                unindexResident(resident);
                changed = true;
            }
        }
        changed |= workforce.values().removeIf(value -> !value.living() && value.type() != com.siirio.jemsettlements.workforce.WorkforceType.RECRUIT);
        if (changed) setDirty();
    }
    public void changed() { setDirty(); }

    @Override
    public CompoundTag save(CompoundTag tag) {
        if (unsupportedRoot != null) return unsupportedRoot.copy();
        tag.merge(unknownRoot.copy());
        tag.putInt("FormatVersion", 1);
        tag.put("Quarantine", quarantine.copy());
        CompoundTag townRows = new CompoundTag();
        towns.forEach((id, town) -> townRows.put(id.toString(), town.save()));
        CompoundTag workerRows = new CompoundTag();
        workforce.forEach((id, worker) -> workerRows.put(id.toString(), worker.save()));
        CompoundTag employerRows = new CompoundTag(); employers.forEach((id, book) -> employerRows.put(id.toString(), book.save())); tag.put("Employers", employerRows);
        tag.put("Towns", townRows);
        tag.put("Workforce", workerRows);
        CompoundTag residentRows = new CompoundTag();
        residents.forEach((id, resident) -> residentRows.put(id.toString(), resident.save()));
        tag.put("Residents", residentRows);
        return tag;
    }

    /** Invalid rows stay lossless in Quarantine and never participate in gameplay. Unexpected
     * programming failures propagate instead of being misreported as bad user data. */
    private static SettlementSavedData load(CompoundTag tag) {
        SettlementSavedData data = new SettlementSavedData();
        if (tag.contains("FormatVersion") && tag.getInt("FormatVersion") != 1) {
            data.unsupportedRoot = tag.copy();
            com.mojang.logging.LogUtils.getLogger().error(
                    "Unsupported settlement storage format {}; original data retained, settlement access disabled",
                    tag.get("FormatVersion"));
            return data;
        }
        data.unknownRoot = tag.copy();
        for (String key : java.util.List.of("Towns", "Workforce", "Employers", "Residents", "Quarantine", "FormatVersion"))
            data.unknownRoot.remove(key);
        if (tag.get("Quarantine") instanceof net.minecraft.nbt.ListTag rows
                && (rows.isEmpty() || rows.getElementType() == net.minecraft.nbt.Tag.TAG_COMPOUND)) {
            for (var row : rows) data.quarantine.add(row.copy());
            if (!rows.isEmpty()) com.mojang.logging.LogUtils.getLogger().error(
                    "Settlement storage contains {} quarantined records requiring repair", rows.size());
        } else if (tag.contains("Quarantine")) {
            data.quarantine("Root", "Quarantine", tag.get("Quarantine"), "Invalid quarantine container");
        }
        data.loadRows(tag, "Towns", TownRecord::load, TownRecord::id, data.towns::put);
        data.loadRows(tag, "Workforce", WorkforceRecord::load, WorkforceRecord::entityId, data.workforce::put);
        data.loadRows(tag, "Employers", com.siirio.jemsettlements.payroll.EmployerPayroll::load,
                null, data.employers::put);
        data.loadRows(tag, "Residents", ResidentMood::load, ResidentMood::villagerId,
                (id, resident) -> data.indexResident(resident));
        return data;
    }

    private <T> void loadRows(CompoundTag root, String group,
            java.util.function.Function<CompoundTag, T> decode,
            java.util.function.Function<T, UUID> identity, java.util.function.BiConsumer<UUID, T> accept) {
        if (!root.contains(group)) return;
        if (!root.contains(group, net.minecraft.nbt.Tag.TAG_COMPOUND)) {
            quarantine("Root", group, root.get(group), "Expected a compound record collection");
            return;
        }
        CompoundTag rows = root.getCompound(group);
        var identities = new java.util.HashSet<UUID>();
        for (String key : rows.getAllKeys()) {
            net.minecraft.nbt.Tag raw = rows.get(key);
            UUID id;
            T value;
            try {
                id = UUID.fromString(key);
                if (identities.contains(id)) throw new IllegalArgumentException("Duplicate record identity");
                if (!(raw instanceof CompoundTag row)) throw new IllegalArgumentException("Expected compound record");
                if (row.contains("FormatVersion") && row.getInt("FormatVersion") != 1)
                    throw new IllegalArgumentException("Unsupported record format version");
                value = decode.apply(row);
                if (identity != null && !id.equals(identity.apply(value)))
                    throw new IllegalArgumentException("Record key differs from embedded identity");
            } catch (IllegalArgumentException | net.minecraft.ResourceLocationException invalid) {
                quarantine(group, key, raw, invalid.getMessage());
                continue;
            }
            identities.add(id);
            accept.accept(id, value);
        }
    }

    private void quarantine(String group, String key, net.minecraft.nbt.Tag raw, String problem) {
        CompoundTag row = new CompoundTag();
        row.putString("Group", group);
        row.putString("Key", key);
        row.putString("Problem", String.valueOf(problem));
        row.put("Raw", raw.copy());
        quarantine.add(row);
        com.mojang.logging.LogUtils.getLogger().error("Settlement record quarantined: {}/{}: {}", group, key, problem);
    }
}
