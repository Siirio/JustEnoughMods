package com.siirio.jemroles.trait;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.saveddata.SavedData;

public final class TemporaryPathSavedData extends SavedData {
    private static final String NAME = "jem_race_paths";
    private final Map<String, Entry> entries = new LinkedHashMap<>();

    public static TemporaryPathSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TemporaryPathSavedData::load, TemporaryPathSavedData::new, NAME);
    }

    public void add(ResourceKey<Level> dimension, BlockPos pos, long expires) {
        entries.put(key(dimension, pos), new Entry(dimension.location(), pos.immutable(), expires));
        setDirty();
    }

    public boolean contains(ResourceKey<Level> dimension, BlockPos pos) { return entries.containsKey(key(dimension, pos)); }

    public void tick(MinecraftServer server) {
        long now = server.overworld().getGameTime();
        for (String key : new ArrayList<>(entries.keySet())) {
            Entry entry = entries.get(key);
            if (entry.expires > now) continue;
            var level = server.getLevel(ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, entry.dimension));
            if (level == null || !level.hasChunkAt(entry.pos)) continue;
            if (level.getBlockState(entry.pos).is(Blocks.OAK_LEAVES)) level.removeBlock(entry.pos, false);
            entries.remove(key);
            setDirty();
        }
    }

    @Override public CompoundTag save(CompoundTag tag) {
        CompoundTag rows = new CompoundTag();
        entries.forEach((key, value) -> {
            CompoundTag row = new CompoundTag();
            row.putString("Dimension", value.dimension.toString());
            row.putLong("Position", value.pos.asLong());
            row.putLong("Expires", value.expires);
            rows.put(key, row);
        });
        tag.put("Entries", rows);
        return tag;
    }

    private static TemporaryPathSavedData load(CompoundTag tag) {
        TemporaryPathSavedData data = new TemporaryPathSavedData();
        CompoundTag rows = tag.getCompound("Entries");
        for (String key : rows.getAllKeys()) {
            CompoundTag row = rows.getCompound(key);
            data.entries.put(key, new Entry(new ResourceLocation(row.getString("Dimension")), BlockPos.of(row.getLong("Position")), row.getLong("Expires")));
        }
        return data;
    }

    private static String key(ResourceKey<Level> dimension, BlockPos pos) { return dimension.location() + "@" + pos.asLong(); }
    private record Entry(ResourceLocation dimension, BlockPos pos, long expires) {}
}
