package com.siirio.jemsettlements.family;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.saveddata.SavedData;

public final class VillagerFamilySavedData extends SavedData {
    private static final String NAME = "jem_villager_families";
    private final Map<UUID, FamilyRecord> people = new LinkedHashMap<>();
    private final Map<UUID, UUID> entityToPerson = new LinkedHashMap<>();
    private final Set<String> seededVillages = new LinkedHashSet<>();

    public static VillagerFamilySavedData get(MinecraftServer server) { return server.overworld().getDataStorage().computeIfAbsent(VillagerFamilySavedData::load, VillagerFamilySavedData::new, NAME); }
    public Collection<FamilyRecord> people() { return java.util.List.copyOf(people.values()); }
    public Optional<FamilyRecord> byPerson(UUID id) { return Optional.ofNullable(people.get(id)); }
    public Optional<FamilyRecord> byEntity(UUID id) { return Optional.ofNullable(entityToPerson.get(id)).map(people::get); }
    public FamilyRecord ensure(LivingEntity entity) {
        FamilyRecord existing = byEntity(entity.getUUID()).orElse(null); if (existing != null) return existing;
        UUID person = entity.getPersistentData().hasUUID("JemFamilyPerson") ? entity.getPersistentData().getUUID("JemFamilyPerson") : UUID.randomUUID();
        FamilyRecord restored = people.get(person); if (restored != null) { bind(restored, entity); return restored; }
        long mixed = person.getMostSignificantBits() ^ Long.rotateLeft(person.getLeastSignificantBits(), 19);
        FamilyRecord.Sex sex = (mixed & 1L) == 0 ? FamilyRecord.Sex.MALE : FamilyRecord.Sex.FEMALE;
        FamilyRecord.Orientation orientation = Math.floorMod(Long.hashCode(mixed >>> 1), 100) < 5 ? FamilyRecord.Orientation.SAME_SEX : FamilyRecord.Orientation.STRAIGHT;
        String name = FamilyNames.name(person, sex);
        FamilyRecord created = new FamilyRecord(person, entity.getUUID(), name, sex, orientation); people.put(person, created); bind(created, entity); setDirty(); return created;
    }
    public void bind(FamilyRecord record, LivingEntity entity) {
        if (record.entityId() != null) entityToPerson.remove(record.entityId()); record.entity(entity.getUUID()); entityToPerson.put(entity.getUUID(), record.personId());
        entity.getPersistentData().putUUID("JemFamilyPerson", record.personId()); setDirty();
    }
    public void transfer(LivingEntity from, LivingEntity to) { FamilyRecord record = ensure(from); bind(record, to); }
    public boolean seedVillage(String key) { boolean added = seededVillages.add(key); if (added) setDirty(); return added; }
    public void changed() { setDirty(); }

    @Override public CompoundTag save(CompoundTag tag) {
        ListTag rows = new ListTag(); people.values().forEach(row -> rows.add(row.save())); tag.put("People", rows);
        ListTag villages = new ListTag(); for (String key : seededVillages) { CompoundTag row = new CompoundTag(); row.putString("Key", key); villages.add(row); } tag.put("SeededVillages", villages); return tag;
    }
    private static VillagerFamilySavedData load(CompoundTag tag) {
        VillagerFamilySavedData data = new VillagerFamilySavedData(); ListTag rows = tag.getList("People", Tag.TAG_COMPOUND);
        for (int i = 0; i < rows.size(); i++) { FamilyRecord row = FamilyRecord.load(rows.getCompound(i)); data.people.put(row.personId(), row); if (row.entityId() != null) data.entityToPerson.put(row.entityId(), row.personId()); }
        ListTag villages = tag.getList("SeededVillages", Tag.TAG_COMPOUND); for (int i = 0; i < villages.size(); i++) data.seededVillages.add(villages.getCompound(i).getString("Key")); return data;
    }
}
