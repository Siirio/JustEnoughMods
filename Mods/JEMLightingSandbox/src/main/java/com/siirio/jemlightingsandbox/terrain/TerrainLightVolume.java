package com.siirio.jemlightingsandbox.terrain;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/** Captures loaded client blocks into a stable camera-local volume for the terrain shader. */
public final class TerrainLightVolume {
    private static final int SIZE_X = 48;
    private static final int SIZE_Y = 40;
    private static final int SIZE_Z = 48;
    private static final int VOLUME = SIZE_X * SIZE_Y * SIZE_Z;
    private static final int CELLS_PER_TICK = 8192;
    private static final int MAX_LIGHTS = 8;
    private static final int RECENTER_DISTANCE = 8;
    private static final int DISCARD_DISTANCE = 18;
    private static final int REFRESH_TICKS = 100;
    private static final byte OPAQUE = (byte) 255;
    private static final int WARM_WHITE = 0xFFE4B8;
    private static final Comparator<RankedLight> LIGHT_PRIORITY = Comparator
            .comparingDouble(RankedLight::distancePerEmission)
            .thenComparingInt(RankedLight::index);

    private ClientLevel level;
    private Snapshot snapshot;
    private Build build;
    private boolean rebuildRequested;
    private int ticksSincePublication;

    public void tick(ClientLevel currentLevel, Vec3 anchor) {
        if (level != currentLevel) {
            clear();
            level = currentLevel;
        }

        int x = Mth.floor(anchor.x);
        int y = Mth.floor(anchor.y);
        int z = Mth.floor(anchor.z);
        if (snapshot != null && snapshot.bounds().awayFrom(x, y, z, DISCARD_DISTANCE)) {
            snapshot = null;
        }
        if (build != null && build.bounds.awayFrom(x, y, z, DISCARD_DISTANCE)) {
            build = null;
        }

        ticksSincePublication++;
        if (build == null && (snapshot == null || rebuildRequested || ticksSincePublication >= REFRESH_TICKS
                || snapshot.bounds().awayFrom(x, y, z, RECENTER_DISTANCE))) {
            build = new Build(new Bounds(x - SIZE_X / 2, y - SIZE_Y / 2, z - SIZE_Z / 2), anchor);
            rebuildRequested = false;
        }

        if (build != null && build.advance(currentLevel)) {
            snapshot = build.finish(currentLevel);
            build = null;
            ticksSincePublication = 0;
        }
    }

    public void invalidate(BlockPos position) {
        int x = position.getX();
        int y = position.getY();
        int z = position.getZ();
        if (build != null && build.bounds.contains(x, y, z)) {
            build.mark(build.bounds.index(x, y, z));
        }
        if (snapshot != null && snapshot.bounds().contains(x, y, z)) {
            rebuildRequested = true;
        }
    }

    public void invalidateChunk(ChunkPos chunk) {
        if (build != null) {
            build.markChunk(chunk);
        }
        if (snapshot != null && snapshot.bounds().intersects(chunk)) {
            rebuildRequested = true;
        }
    }

    public void clear() {
        level = null;
        snapshot = null;
        build = null;
        rebuildRequested = false;
        ticksSincePublication = 0;
    }

    @Nullable
    public Snapshot snapshot() {
        return snapshot;
    }

    public record Snapshot(ClientLevel level, int minX, int minY, int minZ, int sizeX, int sizeY, int sizeZ,
            ByteBuffer opacity, List<Light> lights) {
        private Bounds bounds() {
            return new Bounds(minX, minY, minZ);
        }
    }

    /** The coordinates identify a block. The shader uses its center and linear RGB. */
    public record Light(int x, int y, int z, float red, float green, float blue, int emission) {
    }

    private record Bounds(int minX, int minY, int minZ) {
        private boolean contains(int x, int y, int z) {
            return x >= minX && x < minX + SIZE_X && y >= minY && y < minY + SIZE_Y
                    && z >= minZ && z < minZ + SIZE_Z;
        }

        private boolean intersects(ChunkPos chunk) {
            return chunk.getMinBlockX() < minX + SIZE_X && chunk.getMaxBlockX() >= minX
                    && chunk.getMinBlockZ() < minZ + SIZE_Z && chunk.getMaxBlockZ() >= minZ;
        }

        private boolean awayFrom(int x, int y, int z, int distance) {
            return Math.abs(x - (minX + SIZE_X / 2)) > distance
                    || Math.abs(y - (minY + SIZE_Y / 2)) > distance
                    || Math.abs(z - (minZ + SIZE_Z / 2)) > distance;
        }

        private int index(int x, int y, int z) {
            return x - minX + SIZE_X * (y - minY + SIZE_Y * (z - minZ));
        }

        private int x(int index) {
            return minX + index % SIZE_X;
        }

        private int y(int index) {
            return minY + index / SIZE_X % SIZE_Y;
        }

        private int z(int index) {
            return minZ + index / (SIZE_X * SIZE_Y);
        }
    }

    private static final class Build {
        private final Bounds bounds;
        private final Vec3 anchor;
        private final ByteBuffer opacity = ByteBuffer.allocateDirect(VOLUME);
        private final Int2IntOpenHashMap emitters = new Int2IntOpenHashMap();
        private final BitSet dirty = new BitSet(VOLUME);
        private final BlockPos.MutableBlockPos position = new BlockPos.MutableBlockPos();
        private int cursor;
        private int cachedChunkX;
        private int cachedChunkZ;
        private boolean hasCachedChunk;
        private LevelChunk cachedChunk;

        private Build(Bounds bounds, Vec3 anchor) {
            this.bounds = bounds;
            this.anchor = anchor;
        }

        private boolean advance(ClientLevel level) {
            // Packet replacement is visible on the next tick, including for our last cached coordinates.
            hasCachedChunk = false;
            int budget = CELLS_PER_TICK;
            while (cursor < VOLUME && budget > 0) {
                read(level, cursor++);
                budget--;
            }
            for (int index = dirty.nextSetBit(0); budget > 0 && index >= 0; index = dirty.nextSetBit(index + 1)) {
                dirty.clear(index);
                read(level, index);
                budget--;
            }
            cachedChunk = null;
            return cursor == VOLUME && dirty.isEmpty();
        }

        private void read(ClientLevel level, int index) {
            int x = bounds.x(index);
            int y = bounds.y(index);
            int z = bounds.z(index);
            if (level.isOutsideBuildHeight(y)) {
                opacity.put(index, (byte) 0);
                emitters.remove(index);
                return;
            }
            LevelChunk chunk = chunkAt(level, x >> 4, z >> 4);
            if (chunk == null) {
                opacity.put(index, OPAQUE);
                emitters.remove(index);
                return;
            }
            position.set(x, y, z);
            BlockState state = chunk.getBlockState(position);
            opacity.put(index, state.isSolidRender(level, position) ? OPAQUE : 0);
            int emission = Mth.clamp(state.getLightEmission(level, position), 0, 15);
            if (emission == 0) {
                emitters.remove(index);
            } else {
                emitters.put(index, emission << 24 | emissionColor(state, level, position));
            }
        }

        @Nullable
        private LevelChunk chunkAt(ClientLevel level, int x, int z) {
            if (!hasCachedChunk || x != cachedChunkX || z != cachedChunkZ) {
                cachedChunk = level.getChunkSource().getChunk(x, z, ChunkStatus.FULL, false);
                cachedChunkX = x;
                cachedChunkZ = z;
                hasCachedChunk = true;
            }
            return cachedChunk;
        }

        private void mark(int index) {
            if (index < cursor) {
                dirty.set(index);
            }
        }

        private void markChunk(ChunkPos chunk) {
            if (!bounds.intersects(chunk)) {
                return;
            }
            int startX = Math.max(bounds.minX, chunk.getMinBlockX());
            int endX = Math.min(bounds.minX + SIZE_X, chunk.getMaxBlockX() + 1);
            int startZ = Math.max(bounds.minZ, chunk.getMinBlockZ());
            int endZ = Math.min(bounds.minZ + SIZE_Z, chunk.getMaxBlockZ() + 1);
            for (int z = startZ; z < endZ; z++) {
                for (int y = bounds.minY; y < bounds.minY + SIZE_Y; y++) {
                    int start = bounds.index(startX, y, z);
                    if (start < cursor) {
                        dirty.set(start, Math.min(cursor, start + endX - startX));
                    }
                }
            }
        }

        private Snapshot finish(ClientLevel level) {
            PriorityQueue<RankedLight> nearest = new PriorityQueue<>(MAX_LIGHTS, LIGHT_PRIORITY.reversed());
            var iterator = emitters.int2IntEntrySet().fastIterator();
            while (iterator.hasNext()) {
                var entry = iterator.next();
                int index = entry.getIntKey();
                int encoded = entry.getIntValue();
                int emission = encoded >>> 24;
                double distance = anchor.distanceToSqr(bounds.x(index) + 0.5, bounds.y(index) + 0.5,
                        bounds.z(index) + 0.5) / (emission * emission);
                RankedLight weakest = nearest.peek();
                if (nearest.size() < MAX_LIGHTS || distance < weakest.distancePerEmission
                        || (distance == weakest.distancePerEmission && index < weakest.index)) {
                    if (nearest.size() == MAX_LIGHTS) {
                        nearest.poll();
                    }
                    nearest.add(new RankedLight(index, encoded, distance));
                }
            }
            var ranked = new ArrayList<>(nearest);
            ranked.sort(LIGHT_PRIORITY);
            List<Light> lights = ranked.stream().map(candidate -> {
                int rgb = candidate.encoded;
                return new Light(bounds.x(candidate.index), bounds.y(candidate.index), bounds.z(candidate.index),
                        linear(rgb >>> 16), linear(rgb >>> 8), linear(rgb), rgb >>> 24);
            }).toList();
            return new Snapshot(level, bounds.minX, bounds.minY, bounds.minZ, SIZE_X, SIZE_Y, SIZE_Z,
                    opacity.asReadOnlyBuffer(), lights);
        }
    }

    private record RankedLight(int index, int encoded, double distancePerEmission) {
    }

    private static int emissionColor(BlockState state, ClientLevel level, BlockPos pos) {
        if (state.is(Blocks.SOUL_TORCH) || state.is(Blocks.SOUL_WALL_TORCH) || state.is(Blocks.SOUL_LANTERN)
                || state.is(Blocks.SOUL_FIRE) || state.is(Blocks.SOUL_CAMPFIRE)) {
            return 0x62D8FF;
        }
        if (state.is(Blocks.REDSTONE_LAMP) || state.is(Blocks.REDSTONE_TORCH) || state.is(Blocks.REDSTONE_WALL_TORCH)) {
            return 0xFF6450;
        }
        if (state.is(Blocks.SEA_LANTERN)) {
            return 0xB9F4EE;
        }
        if (state.is(Blocks.TORCH) || state.is(Blocks.WALL_TORCH) || state.is(Blocks.LANTERN)
                || state.is(Blocks.GLOWSTONE) || state.is(BlockTags.CAMPFIRES)) {
            return WARM_WHITE;
        }
        if (state.is(Blocks.LAVA) || state.is(Blocks.FIRE) || state.is(Blocks.MAGMA_BLOCK)) {
            return 0xFF9762;
        }
        int color = state.getMapColor(level, pos).col;
        int red = color >> 16 & 255;
        int green = color >> 8 & 255;
        int blue = color & 255;
        int brightest = Math.max(red, Math.max(green, blue));
        int darkest = Math.min(red, Math.min(green, blue));
        if (brightest < 64 || brightest - darkest < 40) {
            return WARM_WHITE;
        }
        return red * 255 / brightest << 16 | green * 255 / brightest << 8 | blue * 255 / brightest;
    }

    private static float linear(int component) {
        float srgb = (component & 255) / 255.0F;
        return srgb <= 0.04045F ? srgb / 12.92F : (float) Math.pow((srgb + 0.055F) / 1.055F, 2.4);
    }
}
