package com.siirio.jembuilding.blueprint;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

public final class BlueprintSavedData extends SavedData {
    private static final String NAME = "jem_building_blueprints";
    private final Map<UUID, Blueprint> blueprints = new HashMap<>();
    private final Map<UUID, Integer> counters = new HashMap<>();
    private final Map<CellKey, BlueprintCell> activeCells = new HashMap<>();
    private boolean cellsIndexed;

    public static BlueprintSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(BlueprintSavedData::load, BlueprintSavedData::new, NAME);
    }

    public int nextNumber(UUID owner) {
        int number = counters.getOrDefault(owner, 0) + 1;
        counters.put(owner, number);
        setDirty();
        return number;
    }

    public Blueprint create(UUID owner, int number, BlueprintMode mode, ResourceKey<Level> dimension,
                            BlockPos sourceMin, int width, int height, int depth,
                            List<CompoundTag> palette, List<Entry> entries) {
        Blueprint blueprint = new Blueprint(UUID.randomUUID(), owner, number, mode, dimension, sourceMin,
                width, height, depth, palette, entries, mode == BlueprintMode.MOVE ? BlueprintStatus.REMOVING_SOURCE : BlueprintStatus.SAVED,
                0, null, BlueprintRotation.NONE, new BitSet(), new BitSet());
        blueprints.put(blueprint.id(), blueprint);
        setDirty();
        return blueprint;
    }

    public Optional<Blueprint> get(UUID id) {
        return Optional.ofNullable(blueprints.get(id));
    }

    public List<Blueprint> all() {
        return List.copyOf(blueprints.values());
    }

    public Optional<BlueprintCell> cellAt(ResourceKey<Level> dimension, BlockPos pos) {
        if (!cellsIndexed) reindexCells();
        BlueprintCell cell = activeCells.get(new CellKey(dimension, pos.asLong()));
        if (cell == null || cell.blueprint().status() != BlueprintStatus.CONSTRUCTING_COPY
                || cell.blueprint().completed(cell.index())) return Optional.empty();
        return Optional.of(cell);
    }

    public void reindexCells() {
        activeCells.clear();
        for (Blueprint blueprint : blueprints.values()) {
            if (blueprint.destination() == null || blueprint.status() != BlueprintStatus.CONSTRUCTING_COPY) continue;
            for (int index = 0; index < blueprint.entries().size(); index++) {
                if (!blueprint.completed(index)) {
                    activeCells.put(new CellKey(blueprint.dimension(), blueprint.target(index).asLong()), new BlueprintCell(blueprint, index));
                }
            }
        }
        cellsIndexed = true;
    }

    public void changed() {
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag rows = new ListTag();
        for (Blueprint blueprint : blueprints.values()) rows.add(blueprint.save());
        tag.put("Blueprints", rows);
        ListTag counts = new ListTag();
        counters.forEach((owner, number) -> {
            CompoundTag row = new CompoundTag();
            row.putUUID("Owner", owner); row.putInt("Number", number); counts.add(row);
        });
        tag.put("Counters", counts);
        return tag;
    }

    private static BlueprintSavedData load(CompoundTag tag) {
        BlueprintSavedData data = new BlueprintSavedData();
        for (Tag row : tag.getList("Blueprints", Tag.TAG_COMPOUND)) {
            try {
                Blueprint blueprint = Blueprint.load((CompoundTag) row);
                data.blueprints.put(blueprint.id(), blueprint);
            } catch (RuntimeException ignored) { }
        }
        for (Tag row : tag.getList("Counters", Tag.TAG_COMPOUND)) {
            CompoundTag count = (CompoundTag) row;
            if (count.hasUUID("Owner")) data.counters.put(count.getUUID("Owner"), count.getInt("Number"));
        }
        return data;
    }

    public record Entry(int x, int y, int z, int paletteIndex) {
        public BlockPos relative() { return new BlockPos(x, y, z); }
        private CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putInt("X", x); tag.putInt("Y", y); tag.putInt("Z", z); tag.putInt("State", paletteIndex);
            return tag;
        }
        private static Entry load(CompoundTag tag) {
            return new Entry(tag.getInt("X"), tag.getInt("Y"), tag.getInt("Z"), tag.getInt("State"));
        }
    }

    public record BlueprintCell(Blueprint blueprint, int index) { }
    private record CellKey(ResourceKey<Level> dimension, long pos) { }

    public static final class Blueprint {
        private final UUID id;
        private final UUID owner;
        private final int number;
        private final BlueprintMode mode;
        private final ResourceKey<Level> dimension;
        private final BlockPos sourceMin;
        private final int width;
        private final int height;
        private final int depth;
        private final List<CompoundTag> palette;
        private final List<Entry> entries;
        private BlueprintStatus status;
        private int cursor;
        private BlockPos destination;
        private BlueprintRotation rotation;
        private final BitSet completed;
        private final BitSet placeholders;

        private Blueprint(UUID id, UUID owner, int number, BlueprintMode mode, ResourceKey<Level> dimension,
                          BlockPos sourceMin, int width, int height, int depth, List<CompoundTag> palette,
                          List<Entry> entries, BlueprintStatus status, int cursor, BlockPos destination,
                          BlueprintRotation rotation, BitSet completed, BitSet placeholders) {
            this.id = id; this.owner = owner; this.number = number; this.mode = mode; this.dimension = dimension;
            this.sourceMin = sourceMin; this.width = width; this.height = height; this.depth = depth;
            this.palette = List.copyOf(palette); this.entries = List.copyOf(entries); this.status = status;
            this.cursor = cursor; this.destination = destination; this.rotation = rotation;
            this.completed = completed; this.placeholders = placeholders;
        }

        public UUID id() { return id; }
        public UUID owner() { return owner; }
        public int number() { return number; }
        public BlueprintMode mode() { return mode; }
        public ResourceKey<Level> dimension() { return dimension; }
        public BlockPos sourceMin() { return sourceMin; }
        public int width() { return width; }
        public int height() { return height; }
        public int depth() { return depth; }
        public List<CompoundTag> palette() { return palette; }
        public List<Entry> entries() { return entries; }
        public BlueprintStatus status() { return status; }
        public int cursor() { return cursor; }
        public BlockPos destination() { return destination; }
        public BlueprintRotation rotation() { return rotation; }
        public boolean completed(int index) { return completed.get(index); }
        public boolean placeholder(int index) { return placeholders.get(index); }
        public void status(BlueprintStatus value) { status = value; }
        public void cursor(int value) { cursor = value; }
        public void destination(BlockPos value) { destination = value == null ? null : value.immutable(); }
        public void rotation(BlueprintRotation value) { rotation = value; }
        public void completed(int index, boolean value) { completed.set(index, value); }
        public void placeholder(int index, boolean value) { placeholders.set(index, value); }
        public void clearProgress() { completed.clear(); placeholders.clear(); }

        public BlockPos target(int index) {
            Entry entry = entries.get(index);
            return destination.offset(rotation.apply(entry.relative(), width, depth));
        }

        private CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putUUID("Id", id); tag.putUUID("Owner", owner); tag.putInt("Number", number);
            tag.putString("Mode", mode.name()); tag.putString("Dimension", dimension.location().toString());
            tag.putLong("SourceMin", sourceMin.asLong());
            tag.putInt("Width", width); tag.putInt("Height", height); tag.putInt("Depth", depth);
            ListTag paletteRows = new ListTag(); palette.forEach(value -> paletteRows.add(value.copy())); tag.put("Palette", paletteRows);
            ListTag entryRows = new ListTag(); entries.forEach(value -> entryRows.add(value.save())); tag.put("Entries", entryRows);
            tag.putString("Status", status.name()); tag.putInt("Cursor", cursor);
            if (destination != null) tag.putLong("Destination", destination.asLong());
            tag.putString("Rotation", rotation.name());
            tag.putLongArray("Completed", completed.toLongArray()); tag.putLongArray("Placeholders", placeholders.toLongArray());
            return tag;
        }

        private static Blueprint load(CompoundTag tag) {
            List<CompoundTag> palette = new ArrayList<>();
            for (Tag value : tag.getList("Palette", Tag.TAG_COMPOUND)) palette.add(((CompoundTag) value).copy());
            List<Entry> entries = new ArrayList<>();
            for (Tag value : tag.getList("Entries", Tag.TAG_COMPOUND)) entries.add(Entry.load((CompoundTag) value));
            return new Blueprint(tag.getUUID("Id"), tag.getUUID("Owner"), tag.getInt("Number"),
                    BlueprintMode.valueOf(tag.getString("Mode")),
                    ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, new ResourceLocation(tag.getString("Dimension"))),
                    BlockPos.of(tag.getLong("SourceMin")), tag.getInt("Width"), tag.getInt("Height"), tag.getInt("Depth"),
                    palette, entries, BlueprintStatus.valueOf(tag.getString("Status")), tag.getInt("Cursor"),
                    tag.contains("Destination", Tag.TAG_LONG) ? BlockPos.of(tag.getLong("Destination")) : null,
                    tag.contains("Rotation", Tag.TAG_STRING) ? BlueprintRotation.valueOf(tag.getString("Rotation")) : BlueprintRotation.NONE,
                    BitSet.valueOf(tag.getLongArray("Completed")), BitSet.valueOf(tag.getLongArray("Placeholders")));
        }
    }
}
