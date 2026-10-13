package com.siirio.jemsettlements.resident;

import java.util.LinkedHashSet;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

public record AmenitySnapshot(int points, Set<String> categories, long verifiedGameTime, boolean stale) {
    public AmenitySnapshot { points = Math.max(0, Math.min(20, points)); categories = Set.copyOf(categories == null ? Set.of() : categories); }
    public static AmenitySnapshot empty() { return new AmenitySnapshot(0, Set.of(), 0L, true); }
    public CompoundTag save() { CompoundTag tag = new CompoundTag(); tag.putInt("Points", points); tag.putLong("Verified", verifiedGameTime); tag.putBoolean("Stale", stale); ListTag rows = new ListTag(); categories.forEach(value -> rows.add(StringTag.valueOf(value))); tag.put("Categories", rows); return tag; }
    public static AmenitySnapshot load(CompoundTag tag) { Set<String> categories = new LinkedHashSet<>(); ListTag rows = tag.getList("Categories", Tag.TAG_STRING); for (int i = 0; i < rows.size(); i++) categories.add(rows.getString(i)); return new AmenitySnapshot(tag.getInt("Points"), categories, tag.getLong("Verified"), tag.getBoolean("Stale")); }
}
