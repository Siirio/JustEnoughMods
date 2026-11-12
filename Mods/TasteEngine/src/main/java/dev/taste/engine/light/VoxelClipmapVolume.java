package dev.taste.engine.light;

import dev.taste.engine.light.VoxelOccupancyPass.Cells;
import dev.taste.engine.material.MaterialResolver;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;

/** Maintains a snapped world-cell window over toroidal GPU occupancy. */
final class VoxelClipmapVolume implements AutoCloseable {
    private record Dirty(Cells cells, int axis) { }
    private final VoxelOccupancyPass occupancyPass;
    private final VoxelOccupancyPass.Volume occupancy;
    private final VoxelRadiancePass.Volume radiance;
    private final float voxelSize;
    private final Deque<Dirty> pending = new ArrayDeque<>();
    private Map<Long, LevelChunk> chunks = Map.of();
    private boolean checkedChunks;
    private Cells clip;
    private boolean needsRadiance;

    VoxelClipmapVolume(VoxelOccupancyPass occupancyPass, VoxelRadiancePass radiancePass,
            int resolution, float voxelSize, boolean secondBounce) {
        this.occupancyPass = occupancyPass;
        this.voxelSize = voxelSize;
        occupancy = occupancyPass.createVolume(resolution);
        try {
            radiance = radiancePass.createVolume(resolution, secondBounce);
        } catch (RuntimeException failure) {
            occupancy.close();
            throw failure;
        }
    }

    Cells clip() { return clip; }
    float voxelSize() { return voxelSize; }
    VoxelOccupancyPass.Volume occupancy() { return occupancy; }
    VoxelRadiancePass.Volume radiance() { return radiance; }
    boolean dirtyRadiance() { return needsRadiance; }
    void radianceReady() { needsRadiance = false; }
    boolean pending() { return !pending.isEmpty(); }

    void reset() {
        pending.clear();
        chunks = Map.of();
        checkedChunks = false;
        clip = null;
        needsRadiance = true;
    }

    void move(VoxelClipmaps.Clipmap layout) {
        int half = layout.resolution() / 2;
        int x = layout.originX() - half;
        int y = layout.originY() - half;
        int z = layout.originZ() - half;
        Cells next = new Cells(x, y, z, x + layout.resolution(), y + layout.resolution(), z + layout.resolution());
        if (next.equals(clip)) return;
        Cells common = clip == null ? null : intersect(clip, next);
        clip = next;
        needsRadiance = true;
        if (common == null) {
            pending.clear();
            mark(next, 0, false);
            return;
        }
        int tasks = pending.size();
        for (int index = 0; index < tasks; index++) {
            Dirty dirty = pending.removeFirst();
            Cells remaining = intersect(dirty.cells(), common);
            if (remaining != null) pending.addLast(new Dirty(remaining, dirty.axis()));
        }
        mark(cells(next.x0(), next.y0(), next.z0(), common.x0(), next.y1(), next.z1()), 0, false);
        mark(cells(common.x1(), next.y0(), next.z0(), next.x1(), next.y1(), next.z1()), 0, false);
        mark(cells(common.x0(), next.y0(), next.z0(), common.x1(), common.y0(), next.z1()), 1, false);
        mark(cells(common.x0(), common.y1(), next.z0(), common.x1(), next.y1(), next.z1()), 1, false);
        mark(cells(common.x0(), common.y0(), next.z0(), common.x1(), common.y1(), common.z0()), 2, false);
        mark(cells(common.x0(), common.y0(), common.z1(), common.x1(), common.y1(), next.z1()), 2, false);
    }

    void dirty(BlockPos position) {
        if (clip == null) return;
        Cells changed = new Cells(cell(position.getX() - 2), cell(position.getY() - 2), cell(position.getZ() - 2),
                cell(position.getX() + 3) + 1, cell(position.getY() + 3) + 1, cell(position.getZ() + 3) + 1);
        mark(intersect(clip, changed), 0, true);
    }

    void refreshChunks(ClientLevel level) {
        int minX = section(clip.x0() * (double) voxelSize);
        int maxX = section(Math.nextDown(clip.x1() * (double) voxelSize));
        int minZ = section(clip.z0() * (double) voxelSize);
        int maxZ = section(Math.nextDown(clip.z1() * (double) voxelSize));
        Map<Long, LevelChunk> current = new HashMap<>();
        for (int z = minZ; z <= maxZ; z++) {
            for (int x = minX; x <= maxX; x++) {
                long key = ChunkPos.asLong(x, z);
                LevelChunk chunk = level.getChunkSource().getChunk(x, z, false);
                if (chunk != null) current.put(key, chunk);
                if (!checkedChunks || chunk == chunks.get(key)) continue;
                Cells bounds = new Cells(cell(x * 16), clip.y0(), cell(z * 16),
                        cell((x + 1) * 16) + 1, clip.y1(), cell((z + 1) * 16) + 1);
                mark(intersect(clip, bounds), 0, true);
            }
        }
        chunks = current;
        checkedChunks = true;
    }

    int update(int limit, int atlas, ClientLevel level, VoxelSectionCache cache, MaterialResolver materials) {
        if (limit <= 0 || pending.isEmpty()) return 0;
        Dirty dirty = pending.removeFirst();
        Cells area = dirty.cells();
        int slices = Math.min(limit, span(area, dirty.axis()));
        Cells next = partition(area, dirty.axis(), slices, false);
        Cells rest = partition(area, dirty.axis(), slices, true);
        if (rest != null) pending.addFirst(new Dirty(rest, dirty.axis()));
        try (var batch = occupancyPass.begin(occupancy, next, voxelSize, atlas)) {
            int lower = centerCell(level.getMinBuildHeight());
            int upper = centerCell(level.getMaxBuildHeight());
            Cells below = cells(next.x0(), next.y0(), next.z0(), next.x1(), Math.min(next.y1(), lower), next.z1());
            Cells above = cells(next.x0(), Math.max(next.y0(), upper), next.z0(), next.x1(), next.y1(), next.z1());
            if (below != null) batch.loaded(below);
            if (above != null) batch.loaded(above);

            int x0 = section(next.x0() * (double) voxelSize - 1.0);
            int y0 = Math.max(level.getMinSection(), section(next.y0() * (double) voxelSize - 1.0));
            int z0 = section(next.z0() * (double) voxelSize - 1.0);
            int x1 = section(next.x1() * (double) voxelSize + 1.0);
            int y1 = Math.min(level.getMaxSection() - 1, section(next.y1() * (double) voxelSize + 1.0));
            int z1 = section(next.z1() * (double) voxelSize + 1.0);
            for (int z = z0; z <= z1; z++) {
                for (int x = x0; x <= x1; x++) {
                    if (level.getChunkSource().getChunk(x, z, false) == null) continue;
                    for (int y = y0; y <= y1; y++) {
                        var coordinate = new VoxelSectionGeometry.Coordinate(x, y, z);
                        VoxelSectionGeometry.Mesh mesh = cache.get(level, coordinate, materials);
                        if (mesh == null) continue;
                        Cells section = cells(centerCell(x * 16), centerCell(y * 16), centerCell(z * 16),
                                centerCell((x + 1) * 16), centerCell((y + 1) * 16), centerCell((z + 1) * 16));
                        Cells loaded = section == null ? null : intersect(next, section);
                        if (loaded != null) batch.loaded(loaded);
                        batch.draw(coordinate, mesh);
                    }
                }
            }
            batch.commit();
        }
        needsRadiance = true;
        return slices;
    }

    private void mark(Cells region, int axis, boolean immediate) {
        if (region == null) return;
        occupancyPass.invalidate(occupancy, region);
        needsRadiance = true;
        if (immediate) pending.addFirst(new Dirty(region, axis));
        else pending.addLast(new Dirty(region, axis));
    }

    private int cell(double world) { return (int) Math.floor(world / voxelSize); }
    private int centerCell(double world) { return (int) Math.ceil(world / voxelSize - 0.5); }
    private static int section(double world) { return (int) Math.floor(world / 16.0); }

    private static int span(Cells cells, int axis) {
        return axis == 0 ? cells.width() : axis == 1 ? cells.height() : cells.depth();
    }

    private static Cells partition(Cells cells, int axis, int taken, boolean remainder) {
        int x = axis == 0 ? taken : 0;
        int y = axis == 1 ? taken : 0;
        int z = axis == 2 ? taken : 0;
        if (remainder) return cells(cells.x0() + x, cells.y0() + y, cells.z0() + z, cells.x1(), cells.y1(), cells.z1());
        return new Cells(cells.x0(), cells.y0(), cells.z0(),
                axis == 0 ? cells.x0() + taken : cells.x1(),
                axis == 1 ? cells.y0() + taken : cells.y1(),
                axis == 2 ? cells.z0() + taken : cells.z1());
    }

    private static Cells intersect(Cells left, Cells right) {
        return cells(Math.max(left.x0(), right.x0()), Math.max(left.y0(), right.y0()), Math.max(left.z0(), right.z0()),
                Math.min(left.x1(), right.x1()), Math.min(left.y1(), right.y1()), Math.min(left.z1(), right.z1()));
    }

    private static Cells cells(int x0, int y0, int z0, int x1, int y1, int z1) {
        return x0 >= x1 || y0 >= y1 || z0 >= z1 ? null : new Cells(x0, y0, z0, x1, y1, z1);
    }

    @Override public void close() {
        occupancy.close();
        radiance.close();
        pending.clear();
        chunks = Map.of();
    }
}
