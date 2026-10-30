package com.siirio.jemserver.client.smp;

import com.siirio.jemserver.smp.events.AttackGeometry;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.shapes.VoxelShape;

final class EventSurfaceSampler {
    private static final float GROUND_OFFSET = .025F;
    private static final int PRIORITY_SEARCH_LIMIT = 32;
    private static final int MAX_DIRTY_COLUMNS = 4096;
    private static final double[] PRIORITY_STEPS = {0, .25, .5, .75, 1};

    private final AttackGeometry geometry;
    private final Long2ObjectLinkedOpenHashMap<EventSurfaceCell> cells = new Long2ObjectLinkedOpenHashMap<>();
    private final LongArrayFIFOQueue dirtyColumns = new LongArrayFIFOQueue();
    private final LongOpenHashSet dirtySet = new LongOpenHashSet();
    private final long[] priorityColumns;
    private final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
    private int priorityIndex;
    private int scanX;
    private int scanZ;
    private boolean initialComplete;
    private boolean cancelled;

    EventSurfaceSampler(AttackGeometry geometry) {
        this.geometry = geometry.atProgress(0);
        scanX = geometry.minX();
        scanZ = geometry.minZ();
        priorityColumns = priorityColumns(geometry);
    }

    boolean matches(AttackGeometry candidate) {
        return geometry.equals(candidate.atProgress(0));
    }

    Collection<EventSurfaceCell> cells() {
        return cells.values();
    }

    int advance(ClientLevel level, long deadline, int columnBudget) {
        if (cancelled || columnBudget <= 0) return 0;
        int processed = 0;
        int priorityLimit = Math.min(PRIORITY_SEARCH_LIMIT, columnBudget);
        while (priorityIndex < priorityColumns.length && processed < priorityLimit) {
            sample(level, priorityColumns[priorityIndex++]);
            processed++;
        }
        while (processed < columnBudget && System.nanoTime() < deadline) {
            if (!dirtyColumns.isEmpty()) {
                long position = dirtyColumns.dequeueLong();
                dirtySet.remove(position);
                sample(level, position);
            } else if (!initialComplete) {
                sample(level, ChunkPos.asLong(scanX, scanZ));
                advanceCursor();
            } else {
                break;
            }
            processed++;
        }
        return processed;
    }

    void invalidate(BlockPos position) {
        invalidate(position.getX(), position.getZ());
    }

    void invalidate(ChunkPos chunk) {
        int minX = Math.max(geometry.minX(), chunk.getMinBlockX());
        int maxX = Math.min(geometry.maxX(), chunk.getMaxBlockX());
        int minZ = Math.max(geometry.minZ(), chunk.getMinBlockZ());
        int maxZ = Math.min(geometry.maxZ(), chunk.getMaxBlockZ());
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) invalidate(x, z);
        }
    }

    void cancel() {
        cancelled = true;
        cells.clear();
        dirtyColumns.clear();
        dirtySet.clear();
    }

    private void invalidate(int x, int z) {
        if (cancelled || x < geometry.minX() || x > geometry.maxX() || z < geometry.minZ() || z > geometry.maxZ()) return;
        long position = ChunkPos.asLong(x, z);
        cells.remove(position);
        if (dirtySet.size() >= MAX_DIRTY_COLUMNS) {
            restart();
            return;
        }
        if (dirtySet.add(position)) dirtyColumns.enqueue(position);
    }

    private void restart() {
        cells.clear();
        dirtyColumns.clear();
        dirtySet.clear();
        priorityIndex = 0;
        scanX = geometry.minX();
        scanZ = geometry.minZ();
        initialComplete = false;
    }

    private void sample(ClientLevel level, long packed) {
        int x = ChunkPos.getX(packed);
        int z = ChunkPos.getZ(packed);
        if (!geometry.contains(x + .5, z + .5)) {
            cells.remove(packed);
            return;
        }
        cursor.set(x, 0, z);
        if (!level.hasChunkAt(cursor)) {
            cells.remove(packed);
            return;
        }
        int height = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        cursor.set(x, height, z);
        VoxelShape overlay = level.getBlockState(cursor).getBlockSupportShape(level, cursor);
        float y = height + (overlay.isEmpty() ? 0 : (float) overlay.max(Direction.Axis.Y)) + GROUND_OFFSET;
        cells.put(packed, new EventSurfaceCell(x, z, y));
    }

    private void advanceCursor() {
        if (scanZ < geometry.maxZ()) {
            scanZ++;
            return;
        }
        scanZ = geometry.minZ();
        if (scanX < geometry.maxX()) scanX++;
        else initialComplete = true;
    }

    private static long[] priorityColumns(AttackGeometry geometry) {
        LongOpenHashSet seen = new LongOpenHashSet();
        List<Long> columns = new ArrayList<>();
        add(columns, seen, geometry, geometry.originX(), geometry.originZ());
        add(columns, seen, geometry, geometry.secondX(), geometry.secondZ());
        add(columns, seen, geometry, geometry.targetX(), geometry.targetZ());
        for (double xStep : PRIORITY_STEPS) {
            double x = Mth.lerp(xStep, geometry.minX(), geometry.maxX());
            for (double zStep : PRIORITY_STEPS) {
                double z = Mth.lerp(zStep, geometry.minZ(), geometry.maxZ());
                add(columns, seen, geometry, x, z);
            }
        }
        long[] result = new long[columns.size()];
        for (int index = 0; index < columns.size(); index++) result[index] = columns.get(index);
        return result;
    }

    private static void add(List<Long> columns, LongOpenHashSet seen, AttackGeometry geometry, double x, double z) {
        int blockX = Mth.clamp(Mth.floor(x), geometry.minX(), geometry.maxX());
        int blockZ = Mth.clamp(Mth.floor(z), geometry.minZ(), geometry.maxZ());
        long packed = ChunkPos.asLong(blockX, blockZ);
        if (seen.add(packed)) columns.add(packed);
    }
}
