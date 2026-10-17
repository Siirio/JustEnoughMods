package com.siirio.jemprofessions.sandbox.core;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/** Effect settings and world provenance only. Native Skills owns all skill and XP state. */
final class SandboxCoreData extends SavedData {
    final Map<UUID, Settings> settings = new HashMap<>();
    final Map<String, Greenhouse> greenhouses = new HashMap<>();

    static SandboxCoreData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(SandboxCoreData::load, SandboxCoreData::new, "jem_professions_sandbox_core");
    }
    Settings settings(UUID player) { return settings.computeIfAbsent(player, ignored -> new Settings()); }
    @Override public CompoundTag save(CompoundTag tag) {
        CompoundTag people = new CompoundTag();
        settings.forEach((id, value) -> {
            CompoundTag row = new CompoundTag(); row.putString("Fish", value.fish); row.putBoolean("Bait", value.bait);
            row.putString("Dimension", value.planeDimension); row.putLong("Plane", value.plane); row.putInt("Face", value.face); row.putInt("Size", value.size);
            row.putString("Supply", value.supply); people.put(id.toString(), row);
        }); tag.put("Settings", people);
        ListTag houses = new ListTag(); greenhouses.forEach((key, value) -> {
            CompoundTag row = new CompoundTag(); row.putString("Key", key); row.putUUID("Owner", value.owner); row.putString("Dimension", value.dimension);
            row.putLong("Pos", value.pos.asLong()); row.putInt("Fuel", value.fuel); houses.add(row);
        }); tag.put("Greenhouses", houses); return tag;
    }
    private static SandboxCoreData load(CompoundTag tag) {
        SandboxCoreData data = new SandboxCoreData();
        CompoundTag people = tag.getCompound("Settings"); people.getAllKeys().forEach(key -> {
            try {
                CompoundTag row = people.getCompound(key); Settings value = new Settings();
                value.fish = row.getString("Fish"); value.bait = row.getBoolean("Bait"); value.planeDimension = row.getString("Dimension");
                value.plane = row.getLong("Plane"); value.face = Math.floorMod(row.getInt("Face"), 6); value.size = Math.max(0, Math.min(7, row.getInt("Size")));
                value.supply = row.getString("Supply"); data.settings.put(UUID.fromString(key), value);
            } catch (IllegalArgumentException ignored) { }
        });
        ListTag houses = tag.getList("Greenhouses", Tag.TAG_COMPOUND); for (int i = 0; i < Math.min(128, houses.size()); i++) {
            CompoundTag row = houses.getCompound(i); if (!row.hasUUID("Owner")) continue;
            Greenhouse value = new Greenhouse(row.getUUID("Owner"), row.getString("Dimension"), BlockPos.of(row.getLong("Pos")));
            value.fuel = Math.max(0, Math.min(4096, row.getInt("Fuel"))); data.greenhouses.put(row.getString("Key"), value);
        }
        return data;
    }
    static final class Settings {
        String fish = "minecraft:cod";
        boolean bait;
        String planeDimension = "";
        long plane;
        int face;
        int size;
        String supply = "";
    }
    static final class Greenhouse {
        final UUID owner;
        final String dimension;
        final BlockPos pos;
        int fuel;
        long checked = Long.MIN_VALUE;
        boolean enclosed;
        Greenhouse(UUID owner, String dimension, BlockPos pos) { this.owner = owner; this.dimension = dimension; this.pos = pos.immutable(); }
    }
}
