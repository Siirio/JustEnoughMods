package com.siirio.jemsettlements.collection;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

public final class PlayerMountCollectionSavedData extends SavedData {
    public static final int ROSTER_REVISION = 1;
    private static final String NAME = "jem_mount_collections";
    public static final class CollectionRecord {
        private final Set<UUID> individuals = new LinkedHashSet<>(); private final Set<UUID> dead = new LinkedHashSet<>(); private final Set<ResourceLocation> types = new LinkedHashSet<>(); private final Set<String> rewards = new LinkedHashSet<>();
        public int individualCount() { return individuals.size(); } public int livingCount() { return individuals.size() - dead.size(); } public int typeCount() { return types.size(); } public Set<UUID> individuals() { return Set.copyOf(individuals); } public Set<ResourceLocation> types() { return Set.copyOf(types); } public Set<String> rewards() { return Set.copyOf(rewards); }
        boolean record(UUID entity, ResourceLocation type) { boolean added = individuals.add(entity); if (added) { dead.remove(entity); types.add(type); } return added; }
        boolean markDead(UUID entity) { return individuals.contains(entity) && dead.add(entity); }
        CompoundTag save() { CompoundTag tag = new CompoundTag(); ListTag entities = new ListTag(); individuals.forEach(value -> { CompoundTag row = new CompoundTag(); row.putUUID("Id", value); entities.add(row); }); tag.put("Individuals", entities); ListTag deadRows = new ListTag(); dead.forEach(value -> { CompoundTag row = new CompoundTag(); row.putUUID("Id", value); deadRows.add(row); }); tag.put("Dead", deadRows); ListTag typeRows = new ListTag(); types.forEach(value -> typeRows.add(StringTag.valueOf(value.toString()))); tag.put("Types", typeRows); ListTag rewardRows = new ListTag(); rewards.forEach(value -> rewardRows.add(StringTag.valueOf(value))); tag.put("Rewards", rewardRows); return tag; }
        static CollectionRecord load(CompoundTag tag) { CollectionRecord record = new CollectionRecord(); ListTag entities = tag.getList("Individuals", Tag.TAG_COMPOUND); for (int i=0;i<entities.size();i++) if (entities.getCompound(i).hasUUID("Id")) record.individuals.add(entities.getCompound(i).getUUID("Id")); ListTag deadRows = tag.getList("Dead", Tag.TAG_COMPOUND); for (int i=0;i<deadRows.size();i++) if (deadRows.getCompound(i).hasUUID("Id")) record.dead.add(deadRows.getCompound(i).getUUID("Id")); record.dead.retainAll(record.individuals); ListTag types=tag.getList("Types",Tag.TAG_STRING); for(int i=0;i<types.size();i++){ResourceLocation id=ResourceLocation.tryParse(types.getString(i));if(id!=null)record.types.add(id);} ListTag rewards=tag.getList("Rewards",Tag.TAG_STRING);for(int i=0;i<rewards.size();i++)record.rewards.add(rewards.getString(i));return record; }
    }
    private final Map<UUID, CollectionRecord> records = new LinkedHashMap<>();
    public static PlayerMountCollectionSavedData get(MinecraftServer server) { return server.overworld().getDataStorage().computeIfAbsent(PlayerMountCollectionSavedData::load, PlayerMountCollectionSavedData::new, NAME); }
    public CollectionRecord record(UUID player) { return records.computeIfAbsent(player, ignored -> new CollectionRecord()); }
    public boolean record(UUID player, UUID entity, ResourceLocation type) { boolean added = record(player).record(entity, type); if (added) setDirty(); return added; }
    public boolean markDead(UUID entity) { boolean changed = records.values().stream().anyMatch(record -> record.markDead(entity)); if (changed) setDirty(); return changed; }
    @Override public CompoundTag save(CompoundTag tag) { tag.putInt("RosterRevision", ROSTER_REVISION); CompoundTag rows = new CompoundTag(); records.forEach((id, record) -> rows.put(id.toString(), record.save())); tag.put("Players", rows); return tag; }
    private static PlayerMountCollectionSavedData load(CompoundTag tag) { PlayerMountCollectionSavedData data = new PlayerMountCollectionSavedData(); CompoundTag rows = tag.getCompound("Players"); for(String key:rows.getAllKeys())try{data.records.put(UUID.fromString(key),CollectionRecord.load(rows.getCompound(key)));}catch(IllegalArgumentException ignored){} return data; }
}
