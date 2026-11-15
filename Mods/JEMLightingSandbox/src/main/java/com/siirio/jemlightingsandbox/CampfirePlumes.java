package com.siirio.jemlightingsandbox;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.CampfireBlock;

public final class CampfirePlumes {
    private static final int HORIZONTAL_RADIUS = 3;
    private static final int MAX_NEIGHBORS = 7;
    private static final Map<ClientLevel, NearbyCampfires> NEARBY = new WeakHashMap<>();

    private CampfirePlumes() {
    }

    public static BlockPos source(ClientLevel level, double x, double y, double z) {
        // Vanilla spreads smoke up to two blocks above the source in the same column.
        BlockPos particle = BlockPos.containing(x, y, z);
        if (!level.hasChunkAt(particle)) {
            return null;
        }
        if (CampfireBlock.isLitCampfire(level.getBlockState(particle))) {
            return particle;
        }
        BlockPos below = particle.below();
        return CampfireBlock.isLitCampfire(level.getBlockState(below)) ? below : null;
    }

    public static int neighbors(ClientLevel level, BlockPos source) {
        NearbyCampfires cache = NEARBY.computeIfAbsent(level, key -> new NearbyCampfires());
        long interval = level.getGameTime() / 20;
        if (cache.interval != interval) {
            cache.counts.clear();
            cache.interval = interval;
        }
        int cached = cache.counts.get(source.asLong());
        if (cached >= 0) {
            return cached;
        }
        int count = countNeighbors(level, source);
        cache.counts.put(source.asLong(), count);
        return count;
    }

    private static int countNeighbors(ClientLevel level, BlockPos source) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int count = 0;
        for (int dx = -HORIZONTAL_RADIUS; dx <= HORIZONTAL_RADIUS; dx++) {
            for (int dz = -HORIZONTAL_RADIUS; dz <= HORIZONTAL_RADIUS; dz++) {
                if (dx * dx + dz * dz > HORIZONTAL_RADIUS * HORIZONTAL_RADIUS
                        || !level.hasChunkAt(cursor.setWithOffset(source, dx, 0, dz))) {
                    continue;
                }
                for (int dy = -1; dy <= 1; dy++) {
                    if (dx == 0 && dy == 0 && dz == 0) {
                        continue;
                    }
                    if (CampfireBlock.isLitCampfire(level.getBlockState(cursor.setWithOffset(source, dx, dy, dz)))) {
                        count++;
                    }
                    if (count == MAX_NEIGHBORS) {
                        return count;
                    }
                }
            }
        }
        return count;
    }

    private static final class NearbyCampfires {
        private final Long2IntOpenHashMap counts = new Long2IntOpenHashMap();
        private long interval = Long.MIN_VALUE;

        private NearbyCampfires() {
            counts.defaultReturnValue(-1);
        }
    }
}
