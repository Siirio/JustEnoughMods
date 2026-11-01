package com.siirio.jembuilding.construction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;

public final class ConstructionData extends SavedData {
    private static final String NAME = "jem_building_regions";
    private final List<Region> regions = new ArrayList<>();

    public static ConstructionData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(ConstructionData::load, ConstructionData::new, NAME);
    }

    public Region create(UUID owner, ConstructionPlane plane, BlockPos first, BlockPos second) {
        Region region = new Region(UUID.randomUUID(), owner, plane,
                Math.min(first.getX(), second.getX()), Math.min(first.getY(), second.getY()), Math.min(first.getZ(), second.getZ()),
                Math.max(first.getX(), second.getX()), Math.max(first.getY(), second.getY()), Math.max(first.getZ(), second.getZ()), false);
        regions.add(region);
        setDirty();
        return region;
    }

    public List<Region> all() {
        return List.copyOf(regions);
    }

    public Optional<Region> at(BlockPos pos) {
        for (int i = regions.size() - 1; i >= 0; i--) {
            Region region = regions.get(i);
            if (!region.cancelled() && region.contains(pos)) return Optional.of(region);
        }
        return Optional.empty();
    }

    public void cancel(Region region) {
        region.cancelled = true;
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag rows = new ListTag();
        for (Region region : regions) rows.add(region.save());
        tag.put("Regions", rows);
        return tag;
    }

    private static ConstructionData load(CompoundTag tag) {
        ConstructionData data = new ConstructionData();
        ListTag rows = tag.getList("Regions", Tag.TAG_COMPOUND);
        for (Tag value : rows) {
            try { data.regions.add(Region.load((CompoundTag) value)); }
            catch (RuntimeException ignored) { }
        }
        return data;
    }

    public static final class Region {
        private final UUID id;
        private final UUID owner;
        private final ConstructionPlane plane;
        private final int minX;
        private final int minY;
        private final int minZ;
        private final int maxX;
        private final int maxY;
        private final int maxZ;
        private boolean cancelled;

        private Region(UUID id, UUID owner, ConstructionPlane plane, int minX, int minY, int minZ,
                       int maxX, int maxY, int maxZ, boolean cancelled) {
            this.id = id;
            this.owner = owner;
            this.plane = plane;
            this.minX = minX;
            this.minY = minY;
            this.minZ = minZ;
            this.maxX = maxX;
            this.maxY = maxY;
            this.maxZ = maxZ;
            this.cancelled = cancelled;
        }

        public UUID id() { return id; }
        public UUID owner() { return owner; }
        public ConstructionPlane plane() { return plane; }
        public int minX() { return minX; }
        public int minY() { return minY; }
        public int minZ() { return minZ; }
        public int maxX() { return maxX; }
        public int maxY() { return maxY; }
        public int maxZ() { return maxZ; }
        public boolean cancelled() { return cancelled; }
        public ConstructionPlane plane() { return plane; }
        public int minX() { return minX; }
        public int minY() { return minY; }
        public int minZ() { return minZ; }
        public int maxX() { return maxX; }
        public int maxY() { return maxY; }
        public int maxZ() { return maxZ; }
        public boolean cancelled() { return cancelled; }

        public boolean contains(BlockPos pos) {
            return pos.getX() >= minX && pos.getX() <= maxX && pos.getY() >= minY && pos.getY() <= maxY
                    && pos.getZ() >= minZ && pos.getZ() <= maxZ;
        }

        public boolean intersects(ChunkPos chunk) {
            return maxX >= chunk.getMinBlockX() && minX <= chunk.getMaxBlockX()
                    && maxZ >= chunk.getMinBlockZ() && minZ <= chunk.getMaxBlockZ();
        }

        public long width() { return (long) maxX - minX + 1L; }
        public long height() { return (long) maxY - minY + 1L; }
        public long depth() { return (long) maxZ - minZ + 1L; }
        public long cells() {
            return switch (plane) {
                case XY -> width() * height();
                case ZY -> depth() * height();
                case XZ -> width() * depth();
            };
        }

        public BlockPos position(long index) {
            return switch (plane) {
                case XY -> new BlockPos((int) (minX + index % width()), (int) (minY + index / width()), minZ);
                case ZY -> new BlockPos(minX, (int) (minY + index / depth()), (int) (minZ + index % depth()));
                case XZ -> new BlockPos((int) (minX + index % width()), minY, (int) (minZ + index / width()));
            };
        }

        public long index(BlockPos pos) {
            return switch (plane) {
                case XY -> ((long) pos.getY() - minY) * width() + pos.getX() - minX;
                case ZY -> ((long) pos.getY() - minY) * depth() + pos.getZ() - minZ;
                case XZ -> ((long) pos.getZ() - minZ) * width() + pos.getX() - minX;
            };
        }

        private CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putUUID("Id", id);
            tag.putUUID("Owner", owner);
            tag.putString("Plane", plane.name());
            tag.putInt("MinX", minX); tag.putInt("MinY", minY); tag.putInt("MinZ", minZ);
            tag.putInt("MaxX", maxX); tag.putInt("MaxY", maxY); tag.putInt("MaxZ", maxZ);
            tag.putBoolean("Cancelled", cancelled);
            return tag;
        }

        private static Region load(CompoundTag tag) {
            return new Region(tag.getUUID("Id"), tag.getUUID("Owner"), ConstructionPlane.valueOf(tag.getString("Plane")),
                    tag.getInt("MinX"), tag.getInt("MinY"), tag.getInt("MinZ"),
                    tag.getInt("MaxX"), tag.getInt("MaxY"), tag.getInt("MaxZ"), tag.getBoolean("Cancelled"));
        }
    }
}
