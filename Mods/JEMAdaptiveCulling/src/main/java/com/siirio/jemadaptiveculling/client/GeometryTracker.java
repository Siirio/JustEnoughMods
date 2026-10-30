package com.siirio.jemadaptiveculling.client;

import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;

public final class GeometryTracker {
    private static final int MAX_ENTRIES = 8192;
    private static final Long2LongOpenHashMap SECTIONS = new Long2LongOpenHashMap();
    private static final Long2LongOpenHashMap CHUNKS = new Long2LongOpenHashMap();
    private static long revision;
    private static long epoch;

    private GeometryTracker() {
    }

    public static void changed(BlockPos position) {
        ensureCapacity();
        long stamp = ++revision;
        int lowX = (position.getX() - 1) >> 4, highX = (position.getX() + 1) >> 4;
        int lowY = (position.getY() - 1) >> 4, highY = (position.getY() + 1) >> 4;
        int lowZ = (position.getZ() - 1) >> 4, highZ = (position.getZ() + 1) >> 4;
        for (int x = lowX; x <= highX; x++) {
            for (int y = lowY; y <= highY; y++) {
                for (int z = lowZ; z <= highZ; z++) SECTIONS.put(SectionPos.asLong(x, y, z), stamp);
            }
        }
    }

    public static void changed(ChunkPos position) {
        ensureCapacity();
        CHUNKS.put(position.toLong(), ++revision);
    }

    public static long stamp(AABB solid) {
        long stamp = 0;
        for (int x = Mth.floor(solid.minX) >> 4; x <= Mth.floor(Math.nextDown(solid.maxX)) >> 4; x++) {
            for (int z = Mth.floor(solid.minZ) >> 4; z <= Mth.floor(Math.nextDown(solid.maxZ)) >> 4; z++) {
                stamp = Math.max(stamp, CHUNKS.get(ChunkPos.asLong(x, z)));
                for (int y = Mth.floor(solid.minY) >> 4; y <= Mth.floor(Math.nextDown(solid.maxY)) >> 4; y++) {
                    stamp = Math.max(stamp, SECTIONS.get(SectionPos.asLong(x, y, z)));
                }
            }
        }
        return stamp;
    }

    public static long revision() {
        return revision;
    }

    public static long epoch() {
        return epoch;
    }

    public static void clear() {
        SECTIONS.clear();
        CHUNKS.clear();
        revision++;
        epoch++;
    }

    private static void ensureCapacity() {
        if (SECTIONS.size() >= MAX_ENTRIES - 8 || CHUNKS.size() >= MAX_ENTRIES) clear();
    }
}
