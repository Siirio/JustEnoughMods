package com.siirio.jemsettlements.town;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

public final class TownRecord {
    private final UUID id;
    private final UUID owner;
    private final String name;
    private final ResourceLocation dimension;
    private final BlockPos center;
    private final Set<UUID> residents = new LinkedHashSet<>();
    private final List<StorageLink> storage = new ArrayList<>();
    private int happiness = 50;
    private int taxPercent;
    private long lastHappinessDay = Long.MIN_VALUE;

    public TownRecord(UUID id, UUID owner, String name, ResourceLocation dimension, BlockPos center) {
        this.id = id;
        this.owner = owner;
        this.name = name;
        this.dimension = dimension;
        this.center = center.immutable();
        residents.add(owner);
    }

    public UUID id() { return id; }
    public UUID owner() { return owner; }
    public String name() { return name; }
    public ResourceLocation dimension() { return dimension; }
    public BlockPos center() { return center; }
    public Set<UUID> residents() { return Set.copyOf(residents); }
    public List<StorageLink> storage() { return List.copyOf(storage); }
    public int happiness() { return happiness; }
    public int taxPercent() { return taxPercent; }
    public long lastHappinessDay() { return lastHappinessDay; }
    public void lastHappinessDay(long value) { lastHappinessDay = value; }
    public void happiness(int value) { happiness = Math.max(0, Math.min(100, value)); }
    public void taxPercent(int value) {
        if (value != 0 && value != 5 && value != 10 && value != 15) throw new IllegalArgumentException("Unsupported tax rate");
        taxPercent = value;
    }
    public boolean addStorage(StorageLink link) { return !storage.contains(link) && storage.size() < 64 && storage.add(link); }

    CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("Id", id);
        tag.putUUID("Owner", owner);
        tag.putString("Name", name);
        tag.putString("Dimension", dimension.toString());
        tag.putLong("Center", center.asLong());
        tag.putInt("Happiness", happiness);
        tag.putInt("Tax", taxPercent);
        tag.putLong("LastHappinessDay", lastHappinessDay);
        ListTag residentTags = new ListTag();
        residents.forEach(value -> { CompoundTag row = new CompoundTag(); row.putUUID("Id", value); residentTags.add(row); });
        tag.put("Residents", residentTags);
        ListTag storageTags = new ListTag();
        storage.forEach(value -> storageTags.add(value.save()));
        tag.put("Storage", storageTags);
        return tag;
    }

    static TownRecord load(CompoundTag tag) {
        TownRecord town = new TownRecord(tag.getUUID("Id"), tag.getUUID("Owner"), tag.getString("Name"),
                new ResourceLocation(tag.getString("Dimension")), BlockPos.of(tag.getLong("Center")));
        town.happiness(tag.getInt("Happiness"));
        town.taxPercent(tag.getInt("Tax"));
        town.lastHappinessDay(tag.getLong("LastHappinessDay"));
        ListTag residents = tag.getList("Residents", Tag.TAG_COMPOUND);
        for (int i = 0; i < residents.size(); i++) if (residents.getCompound(i).hasUUID("Id")) town.residents.add(residents.getCompound(i).getUUID("Id"));
        ListTag stores = tag.getList("Storage", Tag.TAG_COMPOUND);
        for (int i = 0; i < stores.size() && town.storage.size() < 64; i++) town.storage.add(StorageLink.load(stores.getCompound(i)));
        return town;
    }
}
