package com.siirio.jembuilding.construction;

import java.util.ArrayList;
import java.util.BitSet;
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

    public Region create(UUID owner, ConstructionShape shape, ConstructionPlane plane, BlockPos first, BlockPos second,
                         long expiresAtGameTime) {
        int radius = switch (shape) {
            case SPHERE -> ConstructionGeometry.sphereRadius(first, second);
            case CYLINDER -> ConstructionGeometry.cylinderRadius(first, second);
            default -> 0;
        };
        boolean radial = shape == ConstructionShape.SPHERE || shape == ConstructionShape.CYLINDER;
        Region region = new Region(UUID.randomUUID(), owner, shape, plane, first.getX(), first.getY(), first.getZ(), radius,
                radial ? first.getX() - radius : Math.min(first.getX(), second.getX()),
                shape == ConstructionShape.SPHERE ? first.getY() - radius : Math.min(first.getY(), second.getY()),
                radial ? first.getZ() - radius : Math.min(first.getZ(), second.getZ()),
                radial ? first.getX() + radius : Math.max(first.getX(), second.getX()),
                shape == ConstructionShape.SPHERE ? first.getY() + radius : Math.max(first.getY(), second.getY()),
                radial ? first.getZ() + radius : Math.max(first.getZ(), second.getZ()),
                expiresAtGameTime, false, new BitSet());
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
            if (!region.cancelled() && region.contains(pos) && !region.settled(pos)) return Optional.of(region);
        }
        return Optional.empty();
    }

    public void settle(Region region, long index) {
        if (region.settle(index)) setDirty();
    }

    public void cancel(Region region) {
        region.cancelled = true;
        setDirty();
    }

    public void initializeMissingExpirations(long expiresAtGameTime) {
        boolean changed = false;
        for (Region region : regions) {
            if (region.cancelled || region.expiresAtGameTime != Long.MIN_VALUE) continue;
            region.expiresAtGameTime = expiresAtGameTime;
            changed = true;
        }
        if (changed) setDirty();
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
        private final ConstructionShape shape;
        private final ConstructionPlane plane;
        private final int originX;
        private final int originY;
        private final int originZ;
        private final int radius;
        private final int minX;
        private final int minY;
        private final int minZ;
        private final int maxX;
        private final int maxY;
        private final int maxZ;
        private long expiresAtGameTime;
        private boolean cancelled;
        private final BitSet settled;
        private List<BlockPos> positions;

        private Region(UUID id, UUID owner, ConstructionShape shape, ConstructionPlane plane,
                       int originX, int originY, int originZ, int radius, int minX, int minY, int minZ,
                       int maxX, int maxY, int maxZ, long expiresAtGameTime, boolean cancelled, BitSet settled) {
            this.id = id;
            this.owner = owner;
            this.shape = shape;
            this.plane = plane;
            this.originX = originX;
            this.originY = originY;
            this.originZ = originZ;
            this.radius = radius;
            this.minX = minX;
            this.minY = minY;
            this.minZ = minZ;
            this.maxX = maxX;
            this.maxY = maxY;
            this.maxZ = maxZ;
            this.expiresAtGameTime = expiresAtGameTime;
            this.cancelled = cancelled;
            this.settled = settled;
        }

        public UUID id() { return id; }
        public UUID owner() { return owner; }
        public ConstructionShape shape() { return shape; }
        public ConstructionPlane plane() { return plane; }
        public int minX() { return minX; }
        public int minY() { return minY; }
        public int minZ() { return minZ; }
        public int maxX() { return maxX; }
        public int maxY() { return maxY; }
        public int maxZ() { return maxZ; }
        public long expiresAtGameTime() { return expiresAtGameTime; }
        public boolean cancelled() { return cancelled; }

        public boolean contains(BlockPos pos) {
            if (pos.getX() < minX || pos.getX() > maxX || pos.getY() < minY || pos.getY() > maxY
                    || pos.getZ() < minZ || pos.getZ() > maxZ) return false;
            return switch (shape) {
                case WALL, FLOOR, COLUMN -> true;
                case SPHERE -> sphereCell(pos.getX() - originX, pos.getY() - originY, pos.getZ() - originZ);
                case CYLINDER -> cylinderCell(pos.getX() - originX, pos.getZ() - originZ);
            };
        }

        public boolean intersects(ChunkPos chunk) {
            return maxX >= chunk.getMinBlockX() && minX <= chunk.getMaxBlockX()
                    && maxZ >= chunk.getMinBlockZ() && minZ <= chunk.getMaxBlockZ();
        }

        public long width() { return (long) maxX - minX + 1L; }
        public long height() { return (long) maxY - minY + 1L; }
        public long depth() { return (long) maxZ - minZ + 1L; }
        public long cells() {
            return positions().size();
        }

        public BlockPos position(long index) {
            return positions().get((int) index);
        }

        public long index(BlockPos pos) {
            return positions().indexOf(pos);
        }

        public boolean settled(long index) {
            return index >= 0L && index <= Integer.MAX_VALUE && settled.get((int) index);
        }

        private boolean settled(BlockPos pos) {
            return settled(index(pos));
        }

        private boolean settle(long index) {
            if (index < 0L || index > Integer.MAX_VALUE || settled.get((int) index)) return false;
            settled.set((int) index);
            return true;
        }

        public List<BlockPos> positions() {
            if (positions != null) return positions;
            List<BlockPos> built = new ArrayList<>();
            if (shape == ConstructionShape.SPHERE) {
                positions = ConstructionGeometry.spherePositions(new BlockPos(originX, originY, originZ), radius);
                return positions;
            } else if (shape == ConstructionShape.CYLINDER) {
                positions = ConstructionGeometry.cylinderPositions(new BlockPos(originX, originY, originZ), radius, minY, maxY);
                return positions;
            } else {
                for (int y = minY; y <= maxY; y++) for (int z = minZ; z <= maxZ; z++)
                    for (int x = minX; x <= maxX; x++) built.add(new BlockPos(x, y, z));
            }
            positions = Collections.unmodifiableList(built);
            return positions;
        }

        private boolean sphereCell(int dx, int dy, int dz) {
            double distance = Math.sqrt((double) dx * dx + (double) dy * dy + (double) dz * dz);
            return distance >= Math.max(0D, radius - 0.5D) && distance <= radius + 0.5D;
        }

        private boolean cylinderCell(int dx, int dz) {
            double distance = Math.sqrt((double) dx * dx + (double) dz * dz);
            return distance >= Math.max(0D, radius - 0.5D) && distance <= radius + 0.5D;
        }

        private CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putUUID("Id", id);
            tag.putUUID("Owner", owner);
            tag.putString("Shape", shape.name());
            tag.putString("Plane", plane.name());
            tag.putInt("OriginX", originX); tag.putInt("OriginY", originY); tag.putInt("OriginZ", originZ);
            tag.putInt("Radius", radius);
            tag.putInt("MinX", minX); tag.putInt("MinY", minY); tag.putInt("MinZ", minZ);
            tag.putInt("MaxX", maxX); tag.putInt("MaxY", maxY); tag.putInt("MaxZ", maxZ);
            tag.putLong("ExpiresAtGameTime", expiresAtGameTime);
            tag.putBoolean("Cancelled", cancelled);
            tag.putLongArray("Settled", settled.toLongArray());
            return tag;
        }

        private static Region load(CompoundTag tag) {
            ConstructionPlane plane = ConstructionPlane.valueOf(tag.getString("Plane"));
            ConstructionShape shape = tag.contains("Shape", Tag.TAG_STRING) ? ConstructionShape.valueOf(tag.getString("Shape"))
                    : plane == ConstructionPlane.XZ ? ConstructionShape.FLOOR : ConstructionShape.WALL;
            return new Region(tag.getUUID("Id"), tag.getUUID("Owner"), shape, plane,
                    tag.contains("OriginX", Tag.TAG_INT) ? tag.getInt("OriginX") : tag.getInt("MinX"),
                    tag.contains("OriginY", Tag.TAG_INT) ? tag.getInt("OriginY") : tag.getInt("MinY"),
                    tag.contains("OriginZ", Tag.TAG_INT) ? tag.getInt("OriginZ") : tag.getInt("MinZ"),
                    tag.getInt("Radius"),
                    tag.getInt("MinX"), tag.getInt("MinY"), tag.getInt("MinZ"),
                    tag.getInt("MaxX"), tag.getInt("MaxY"), tag.getInt("MaxZ"),
                    tag.contains("ExpiresAtGameTime", Tag.TAG_LONG) ? tag.getLong("ExpiresAtGameTime") : Long.MIN_VALUE,
                    tag.getBoolean("Cancelled"), BitSet.valueOf(tag.getLongArray("Settled")));
        }
    }
}
