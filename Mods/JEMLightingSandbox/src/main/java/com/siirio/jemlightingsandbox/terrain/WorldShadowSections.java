package com.siirio.jemlightingsandbox.terrain;

import com.mojang.logging.LogUtils;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.BannerBlock;
import net.minecraft.world.level.block.WallBannerBlock;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL21;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;
import org.lwjgl.system.MemoryStack;
import org.slf4j.Logger;

/** Geometry pages are keyed by world section. The camera only sets the loaded window. */
final class WorldShadowSections {
    private static final Logger LOGGER = LogUtils.getLogger();
    static final int RING = 69;
    private static final int CELLS = 16 * 16 * 16;
    private static final int STRIDE = CELLS * Integer.BYTES;
    private static final int BANNER_OVERHANG = 1 << 30;
    private static final int CUTOUT_OVERHANG = 1 << 31;
    private static final int OFFSET_PRESENT = 1 << 14;
    private static final int MAX_BUFFER_BYTES = 256 * 1024 * 1024;
    private static final int UNIFORM = -1;
    private final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
    private final Map<SectionPos, int[]> pending = new LinkedHashMap<>();
    private final Map<SectionPos, Integer> pages = new HashMap<>();
    private final ArrayDeque<Integer> freePages = new ArrayDeque<>();

    private BlockShadowShapes shapes;
    private int table;
    private int palette;
    private int voxels;
    private int backing;
    private int capacity;
    private int maxPages;
    private int nextPage;
    private long revision;
    private boolean reportedCapacity;

    void capture(ClientLevel level, LevelChunk chunk, int sectionIndex) {
        var section = chunk.getSection(sectionIndex);
        int sectionY = chunk.getSectionYFromSectionIndex(sectionIndex);
        SectionPos key = SectionPos.of(chunk.getPos(), sectionY);
        int minX = key.minBlockX();
        int minY = key.minBlockY();
        int minZ = key.minBlockZ();
        if (shapes == null) {
            shapes = new BlockShadowShapes();
        }
        int[] data = new int[CELLS];
        if (!section.hasOnlyAir()) {
            for (int i = 0; i < CELLS; i++) {
                int x = i & 15;
                int y = i >> 8;
                int z = (i >> 4) & 15;
                var state = section.getBlockState(x, y, z);
                if (state.isAir()) {
                    continue;
                }
                pos.set(minX + x, minY + y, minZ + z);
                int shape = shapes.id(state, level, pos);
                int emission = Mth.clamp(state.getLightEmission(level, pos), 0, 15);
                data[i] |= shape | emission << 10;
                if (isWallBanner(shape) && y > 0) {
                    data[i - 256] |= BANNER_OVERHANG;
                } else if (isStandingBanner(shape)) {
                    if (y > 0) {
                        data[i - 256] |= BANNER_OVERHANG;
                    }
                    if (y < 15) {
                        data[i + 256] |= BANNER_OVERHANG;
                    }
                } else if (shapes.isCutout(shape) && state.hasOffsetFunction()) {
                    Vec3 offset = state.getOffset(level, pos);
                    int packed = Mth.clamp((int) Math.round((offset.x + 0.25) * 126), 0, 63)
                            | Mth.clamp((int) Math.round((offset.y + 0.2) * 35), 0, 7) << 6
                            | Mth.clamp((int) Math.round((offset.z + 0.25) * 126), 0, 63) << 9;
                    data[i] |= OFFSET_PRESENT | packed << 15;
                    flagNeighbors(data, x, y, z, offset);
                }
            }
        }
        captureOverhangs(level, chunk, sectionIndex, key, data);
        int value = data[0];
        for (int i = 1; i < CELLS; i++) {
            if (data[i] != value) {
                pending.put(key, data);
                return;
            }
        }
        pending.put(key, new int[]{value});
    }

    private void captureOverhangs(ClientLevel level, LevelChunk chunk, int sectionIndex, SectionPos section,
            int[] data) {
        int minX = section.minBlockX();
        int minY = section.minBlockY();
        int minZ = section.minBlockZ();
        if (sectionIndex + 1 < chunk.getSectionsCount()
                && chunk.getSection(sectionIndex + 1).maybeHas(state -> state.getBlock() instanceof WallBannerBlock
                        || state.getBlock() instanceof BannerBlock)) {
            flagBannerRim(chunk, data, minX, minY + 16, minZ, 15, true);
        }
        if (sectionIndex > 0
                && chunk.getSection(sectionIndex - 1).maybeHas(state -> state.getBlock() instanceof BannerBlock)) {
            flagBannerRim(chunk, data, minX, minY - 1, minZ, 0, false);
        }
        // Check the one-cell rim for offset cutout faces that cross a section boundary.
        for (int dz = -1; dz <= 1; dz++) {
            for (int dx = -1; dx <= 1; dx++) {
                var neighbor = dx == 0 && dz == 0 ? chunk :
                        level.getChunkSource().getChunk(section.x() + dx, section.z() + dz, ChunkStatus.FULL, false);
                if (neighbor == null) continue;
                for (int dy = 0; dy <= 1; dy++) {
                    if ((dx | dy | dz) == 0) continue;
                    int sy = sectionIndex + dy;
                    if (sy >= neighbor.getSectionsCount() || !neighbor.getSection(sy).maybeHas(state -> state.hasOffsetFunction())) {
                        continue;
                    }
                    int startX = dx < 0 ? -1 : dx > 0 ? 16 : 0;
                    int endX = dx == 0 ? 15 : startX;
                    int startZ = dz < 0 ? -1 : dz > 0 ? 16 : 0;
                    int endZ = dz == 0 ? 15 : startZ;
                    int startY = dy > 0 ? 16 : 0;
                    int endY = dy > 0 ? 16 : 15;
                    for (int z = startZ; z <= endZ; z++) {
                        for (int y = startY; y <= endY; y++) {
                            for (int x = startX; x <= endX; x++) {
                                var state = neighbor.getBlockState(pos.set(minX + x, minY + y, minZ + z));
                                if (state.hasOffsetFunction() && shapes.isCutout(shapes.id(state, level, pos))) {
                                    flagNeighbors(data, x, y, z, state.getOffset(level, pos));
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void flagBannerRim(LevelChunk chunk, int[] data, int minX, int y, int minZ, int localY, boolean above) {
        for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
                var block = chunk.getBlockState(pos.set(minX + x, y, minZ + z)).getBlock();
                if (block instanceof BannerBlock || above && block instanceof WallBannerBlock) {
                    data[cell(x, localY, z)] |= BANNER_OVERHANG;
                }
            }
        }
    }

    private static void flagNeighbors(int[] data, int x, int y, int z, Vec3 offset) {
        int dx = (int) Math.signum(offset.x);
        int dy = (int) Math.signum(offset.y);
        int dz = (int) Math.signum(offset.z);
        for (int a = Math.min(0, dx); a <= Math.max(0, dx); a++) {
            for (int b = Math.min(0, dy); b <= Math.max(0, dy); b++) {
                for (int c = Math.min(0, dz); c <= Math.max(0, dz); c++) {
                    int neighborX = x + a;
                    int neighborY = y + b;
                    int neighborZ = z + c;
                    if ((a != 0 || b != 0 || c != 0) && ((neighborX | neighborY | neighborZ) >>> 4) == 0) {
                        data[cell(neighborX, neighborY, neighborZ)] |= CUTOUT_OVERHANG;
                    }
                }
            }
        }
    }

    private static int cell(int x, int y, int z) {
        return x + (z << 4) + (y << 8);
    }

    private static boolean isWallBanner(int shape) {
        return shape >= BlockShadowShapes.WALL_BANNER && shape < BlockShadowShapes.STANDING_BANNER;
    }

    private static boolean isStandingBanner(int shape) {
        return shape >= BlockShadowShapes.STANDING_BANNER && shape < BlockShadowShapes.FIRST_CUSTOM;
    }

    void remove(ChunkPos position, int firstSection, int lastSection) {
        for (int y = firstSection; y < lastSection; y++) {
            pending.put(SectionPos.of(position, y), null);
        }
    }

    Texture bind(ClientLevel level, int tableUnit, int paletteUnit, int voxelUnit) {
        if (level.getSectionsCount() * RING > GL11.glGetInteger(GL11.GL_MAX_TEXTURE_SIZE)) {
            return null;
        }
        int buffer = GL11.glGetInteger(GL21.GL_PIXEL_UNPACK_BUFFER_BINDING);
        int rowLength = GL11.glGetInteger(GL11.GL_UNPACK_ROW_LENGTH);
        int skipRows = GL11.glGetInteger(GL11.GL_UNPACK_SKIP_ROWS);
        int skipPixels = GL11.glGetInteger(GL11.GL_UNPACK_SKIP_PIXELS);
        try {
            GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER, 0);
            GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, 0);
            GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_ROWS, 0);
            GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_PIXELS, 0);
            GL13.glActiveTexture(GL13.GL_TEXTURE0 + paletteUnit);
            bindPalette();
            GL13.glActiveTexture(GL13.GL_TEXTURE0 + tableUnit);
            bindTable(level.getSectionsCount());
            GL13.glActiveTexture(GL13.GL_TEXTURE0 + voxelUnit);
            if (voxels == 0) {
                maxPages = Math.min(MAX_BUFFER_BYTES / STRIDE, GL11.glGetInteger(GL31.GL_MAX_TEXTURE_BUFFER_SIZE) / CELLS);
                voxels = GL11.glGenTextures();
                GL11.glBindTexture(GL31.GL_TEXTURE_BUFFER, voxels);
                reserve(Math.min(64, maxPages));
            } else {
                GL11.glBindTexture(GL31.GL_TEXTURE_BUFFER, voxels);
            }
            GL13.glActiveTexture(GL13.GL_TEXTURE0 + tableUnit);
            if (!pending.isEmpty()) {
                try (var stack = MemoryStack.stackPush()) {
                    upload(level, stack.mallocInt(4));
                }
            }
        } finally {
            GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER, buffer);
            GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, rowLength);
            GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_ROWS, skipRows);
            GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_PIXELS, skipPixels);
        }
        return new Texture(table, palette, voxels, level.getMinSection(), level.getSectionsCount(), revision);
    }

    private void bindPalette() {
        if (palette == 0) {
            palette = makeTexture();
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_RGBA32F, BlockShadowShapes.TEXTURE_WIDTH,
                    BlockShadowShapes.TEXTURE_HEIGHT, 0, GL11.GL_RGBA, GL11.GL_FLOAT, (FloatBuffer) null);
        } else {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, palette);
        }
        if (shapes != null && shapes.dirtyRows() > 0) {
            GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, shapes.firstDirty(), BlockShadowShapes.TEXTURE_WIDTH,
                    shapes.dirtyRows(), GL11.GL_RGBA, GL11.GL_FLOAT, shapes.dirtyData());
            shapes.uploaded();
        }
    }

    private void bindTable(int sections) {
        if (table == 0) {
            table = makeTexture();
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_RGBA32I, RING, RING * sections,
                    0, GL30.GL_RGBA_INTEGER, GL11.GL_INT, new int[RING * RING * sections * 4]);
            return;
        }
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, table);
    }

    private static int makeTexture() {
        int id = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, id);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        return id;
    }

    private void upload(ClientLevel level, IntBuffer header) {
        var removals = pending.entrySet().iterator();
        boolean changed = false;
        header.put(0, 0).put(1, 0).put(2, 0).put(3, 0);
        while (removals.hasNext()) {
            var entry = removals.next();
            if (entry.getValue() == null) {
                releasePage(entry.getKey());
                writeEntry(level, entry.getKey(), header);
                removals.remove();
                changed = true;
            }
        }
        var iterator = pending.entrySet().iterator();
        int previousUpload = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
        try {
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, backing);
            changed |= uploadPages(level, header);
        } finally {
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, previousUpload);
        }
        if (changed) {
            revision++;
        }
    }

    private boolean uploadPages(ClientLevel level, IntBuffer header) {
        var iterator = pending.entrySet().iterator();
        boolean changed = false;
        while (iterator.hasNext()) {
            var entry = iterator.next();
            SectionPos section = entry.getKey();
            int[] data = entry.getValue();
            int page;
            int uniform = 0;
            if (data.length == 1) {
                releasePage(section);
                page = UNIFORM;
                uniform = data[0];
            } else {
                Integer slot = pages.get(section);
                if (slot == null) {
                    if (!freePages.isEmpty()) {
                        slot = freePages.removeFirst();
                    } else if (nextPage < maxPages) {
                        slot = nextPage++;
                        if (slot >= capacity) {
                            reserve(Math.min(maxPages, capacity * 2));
                            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, backing);
                        }
                    } else {
                        if (!reportedCapacity) {
                            LOGGER.warn("JEM shadow sections reached the GPU page limit");
                            reportedCapacity = true;
                        }
                        iterator.remove();
                        continue;
                    }
                    pages.put(section, slot);
                }
                GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, (long) slot * STRIDE, data);
                page = slot + 1;
            }
            header.put(0, section.x()).put(1, section.z()).put(2, page).put(3, uniform);
            writeEntry(level, section, header);
            iterator.remove();
            changed = true;
        }
        return changed;
    }

    private void releasePage(SectionPos section) {
        Integer released = pages.remove(section);
        if (released != null) freePages.addLast(released);
    }

    private static void writeEntry(ClientLevel level, SectionPos section, IntBuffer entry) {
        int x = Math.floorMod(section.x(), RING);
        int y = Math.floorMod(section.z(), RING) * level.getSectionsCount() + section.y() - level.getMinSection();
        GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, x, y, 1, 1, GL30.GL_RGBA_INTEGER, GL11.GL_INT, entry);
    }

    private void reserve(int pages) {
        int previousRead = GL11.glGetInteger(GL21.GL_PIXEL_PACK_BUFFER_BINDING);
        int previousWrite = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
        int previousTexture = GL11.glGetInteger(GL31.GL_TEXTURE_BINDING_BUFFER);
        int next = GL15.glGenBuffers();
        try {
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, next);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, (long) pages * STRIDE, GL15.GL_DYNAMIC_DRAW);
            if (backing != 0) {
                GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER, backing);
                GL31.glCopyBufferSubData(GL21.GL_PIXEL_PACK_BUFFER, GL15.GL_ARRAY_BUFFER,
                        0, 0, (long) capacity * STRIDE);
            }
            GL11.glBindTexture(GL31.GL_TEXTURE_BUFFER, voxels);
            GL31.glTexBuffer(GL31.GL_TEXTURE_BUFFER, GL30.GL_R32UI, next);
        } finally {
            GL11.glBindTexture(GL31.GL_TEXTURE_BUFFER, previousTexture);
            GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER, previousRead);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, previousWrite);
        }
        if (backing != 0) GL15.glDeleteBuffers(backing);
        backing = next;
        capacity = pages;
    }

    void clear() {
        pending.clear();
        pages.clear();
        freePages.clear();
        shapes = null;
        reportedCapacity = false;
        if (table != 0) GL11.glDeleteTextures(table);
        if (palette != 0) GL11.glDeleteTextures(palette);
        if (voxels != 0) GL11.glDeleteTextures(voxels);
        if (backing != 0) GL15.glDeleteBuffers(backing);
        table = palette = voxels = backing = capacity = nextPage = 0;
        revision++;
    }

    record Texture(int table, int palette, int voxels, int minSection, int sections, long revision) {}
}
