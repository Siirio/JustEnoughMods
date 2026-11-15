package com.siirio.jemlightingsandbox.terrain;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.LinkedHashSet;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL21;
import org.lwjgl.opengl.GL30;

final class ChunkLightTexture {
    private static final int LIGHTS_PER_SECTION = 8;
    private static final int MASK_OFFSET = 1 + LIGHTS_PER_SECTION * 2;
    private static final int SECTION_TEXELS = MASK_OFFSET + 2;
    private static final double BOUND_PADDING = 1.0 / 64.0;
    private static final int COLUMNS_PER_UPLOAD = 10;
    private static final long UPLOAD_BUDGET_NANOS = 750_000;
    private LinkedHashSet<ChunkPos> dirty = new LinkedHashSet<>();
    private int texture;
    private int side;
    private int sections;
    private int minSection;
    private int sourceReach;
    private int lastX;
    private int lastZ;
    private int lastRadius = -1;
    private FloatBuffer upload;
    private float[] scores;

    void mark(ChunkPos changed) {
        int cells = sourceColumnRadius();
        for (int z = changed.z - cells; z <= changed.z + cells; z++) {
            for (int x = changed.x - cells; x <= changed.x + cells; x++) {
                dirty.add(new ChunkPos(x, z));
            }
        }
    }

    private int sourceColumnRadius() {
        // A source at the edge still has to cross a full intervening chunk at reaches below 16 blocks.
        return sourceReach >= 16 ? 2 : 1;
    }

    void reachChanged(int reach, Vec3 camera, int chunks) {
        int cx = SectionPos.blockToSectionCoord(camera.x);
        int cz = SectionPos.blockToSectionCoord(camera.z);
        boolean all = sourceReach != reach || lastRadius != chunks;
        if (!all && lastX == cx && lastZ == cz && lastRadius == chunks) {
            return;
        }
        sourceReach = reach;
        var previousDirty = dirty;
        dirty = new LinkedHashSet<>();
        queueNewColumn(cx, cz, all, previousDirty);
        for (int radius = 1; radius <= chunks; radius++) {
            for (int offset = -radius; offset <= radius; offset++) {
                queueNewColumn(cx + offset, cz - radius, all, previousDirty);
                queueNewColumn(cx + offset, cz + radius, all, previousDirty);
            }
            for (int offset = 1 - radius; offset < radius; offset++) {
                queueNewColumn(cx - radius, cz + offset, all, previousDirty);
                queueNewColumn(cx + radius, cz + offset, all, previousDirty);
            }
        }
        lastX = cx;
        lastZ = cz;
        lastRadius = chunks;
    }

    private void queueNewColumn(int x, int z, boolean all, LinkedHashSet<ChunkPos> previousDirty) {
        var position = new ChunkPos(x, z);
        if (all || Math.max(Math.abs(x - lastX), Math.abs(z - lastZ)) > lastRadius || previousDirty.contains(position)) {
            dirty.add(position);
        }
    }

    int side() {
        return side;
    }

    int bindAndUpdate(ClientLevel level, Vec3 camera, int chunks, ChunkLightIndex index, boolean update) {
        int cx = SectionPos.blockToSectionCoord(camera.x);
        int cz = SectionPos.blockToSectionCoord(camera.z);
        int height = level.getSectionsCount();
        int requestedSide = chunks * 2 + 1;
        if (texture != 0 && side != requestedSide) {
            GL11.glDeleteTextures(texture);
            texture = 0;
        }
        if (texture == 0) {
            int limit = GL11.glGetInteger(GL11.GL_MAX_TEXTURE_SIZE);
            if (height * requestedSide > limit || SECTION_TEXELS * requestedSide > limit) {
                return 0;
            }
            side = requestedSide;
            sections = height;
            minSection = level.getMinSection();
            upload = directFloats(sections * SECTION_TEXELS * 4);
            scores = new float[sections * LIGHTS_PER_SECTION];
            texture = GL11.glGenTextures();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
            int unpack = GL11.glGetInteger(GL21.GL_PIXEL_UNPACK_BUFFER_BINDING);
            int rowLength = GL11.glGetInteger(GL11.GL_UNPACK_ROW_LENGTH);
            int skipRows = GL11.glGetInteger(GL11.GL_UNPACK_SKIP_ROWS);
            int skipPixels = GL11.glGetInteger(GL11.GL_UNPACK_SKIP_PIXELS);
            GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER, 0);
            try {
                GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, 0);
                GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_ROWS, 0);
                GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_PIXELS, 0);
                GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_RGBA32F, side * SECTION_TEXELS, side * sections,
                        0, GL11.GL_RGBA, GL11.GL_FLOAT, (FloatBuffer) null);
                // Initialize only the headers. Empty rows have no samples to read.
                float[] emptyHeaders = new float[side * sections * 4];
                for (int x = 0; x < side; x++) {
                    GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, x * SECTION_TEXELS, 0, 1, side * sections,
                            GL11.GL_RGBA, GL11.GL_FLOAT, emptyHeaders);
                }
            } finally {
                GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, rowLength);
                GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_ROWS, skipRows);
                GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_PIXELS, skipPixels);
                GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER, unpack);
            }
        } else {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
        }
        if (update && !dirty.isEmpty()) {
            uploadChanges(cx, cz, chunks, index);
        }
        return sections;
    }

    private void uploadChanges(int centerX, int centerZ, int chunks, ChunkLightIndex index) {
        int buffer = GL11.glGetInteger(GL21.GL_PIXEL_UNPACK_BUFFER_BINDING);
        int alignment = GL11.glGetInteger(GL11.GL_UNPACK_ALIGNMENT);
        int rowLength = GL11.glGetInteger(GL11.GL_UNPACK_ROW_LENGTH);
        int skipRows = GL11.glGetInteger(GL11.GL_UNPACK_SKIP_ROWS);
        int skipPixels = GL11.glGetInteger(GL11.GL_UNPACK_SKIP_PIXELS);
        try {
            GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER, 0);
            GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);
            GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, 0);
            GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_ROWS, 0);
            GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_PIXELS, 0);
            int remaining = COLUMNS_PER_UPLOAD;
            long deadline = System.nanoTime() + UPLOAD_BUDGET_NANOS;
            var pending = dirty.iterator();
            while (pending.hasNext() && remaining > 0
                    && (remaining == COLUMNS_PER_UPLOAD || System.nanoTime() < deadline)) {
                ChunkPos position = pending.next();
                pending.remove();
                if (Math.max(Math.abs(position.x - centerX), Math.abs(position.z - centerZ)) > chunks) {
                    continue;
                }
                uploadColumn(position, index);
                remaining--;
            }
        } finally {
            GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, alignment);
            GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, rowLength);
            GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_ROWS, skipRows);
            GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_PIXELS, skipPixels);
            GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER, buffer);
        }
    }

    private void uploadColumn(ChunkPos position, ChunkLightIndex index) {
        for (int section = 0; section < sections; section++) {
            int header = section * SECTION_TEXELS * 4;
            upload.put(header, position.x).put(header + 1, position.z).put(header + 2, 0F).put(header + 3, 1F);
        }
        int x = position.getMinBlockX();
        int z = position.getMinBlockZ();
        int neighbors = sourceColumnRadius();
        for (int neighborZ = position.z - neighbors; neighborZ <= position.z + neighbors; neighborZ++) {
            for (int neighborX = position.x - neighbors; neighborX <= position.x + neighbors; neighborX++) {
                for (var light : index.sources(neighborX, neighborZ)) {
                    float radius = light.emission() * sourceReach / 15F + 0.5F;
                    double dx = distanceToInterval(light.x(), x, x + 16);
                    double dz = distanceToInterval(light.z(), z, z + 16);
                    double horizontalSquared = dx * dx + dz * dz;
                    if (horizontalSquared >= radius * radius) {
                        continue;
                    }
                    int from = Math.max(0, SectionPos.blockToSectionCoord(light.y() - radius) - minSection);
                    int to = Math.min(sections - 1, SectionPos.blockToSectionCoord(light.y() + radius) - minSection);
                    for (int section = from; section <= to; section++) {
                        int y = (minSection + section) * 16;
                        double dy = distanceToInterval(light.y(), y, y + 16);
                        double distanceSquared = horizontalSquared + dy * dy;
                        if (distanceSquared < radius * radius) {
                            insert(section, light, radius, (float) ((1 + distanceSquared) / (light.emission() * light.emission())), x, y, z);
                        }
                    }
                }
            }
        }
        writeReceiverMasks();
        int u = Math.floorMod(position.x, side) * SECTION_TEXELS;
        int v = Math.floorMod(position.z, side) * sections;
        GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, u, v, SECTION_TEXELS, sections, GL11.GL_RGBA, GL11.GL_FLOAT, upload);
    }

    private void writeReceiverMasks() {
        for (int section = 0; section < sections; section++) {
            int header = section * SECTION_TEXELS * 4;
            int count = (int) upload.get(header + 2);
            if (count == 0) {
                continue;
            }

            int areaMask = 0;
            for (int i = 0; i < count; i++) {
                if (upload.get(header + 11 + i * 8) < 0F) {
                    areaMask |= 1 << i;
                }
            }
            upload.put(header + 3, 1F + areaMask);
            for (int octant = 0; octant < 8; octant++) {
                int x = (octant & 1) * 8;
                int z = (octant & 2) * 4;
                int y = (octant & 4) * 2;
                int candidates = 0;
                for (int i = 0; i < count; i++) {
                    int first = header + 4 + i * 8;
                    double dx = distanceToInterval(upload.get(first), x, x + 8);
                    double dy = distanceToInterval(upload.get(first + 1), y, y + 8);
                    double dz = distanceToInterval(upload.get(first + 2), z, z + 8);
                    // Include the surface bias and position rounding on octant boundaries.
                    double radius = upload.get(first + 3) + BOUND_PADDING;
                    if (dx * dx + dy * dy + dz * dz <= radius * radius) {
                        candidates |= 1 << i;
                    }
                }
                upload.put(header + MASK_OFFSET * 4 + octant, candidates);
            }
        }
    }

    private void insert(int section, BlockLight light, float radius, float score, int x, int y, int z) {
        int header = section * SECTION_TEXELS * 4;
        int count = (int) upload.get(header + 2);
        int row = section * LIGHTS_PER_SECTION;
        if (count == LIGHTS_PER_SECTION && scores[row + count - 1] <= score) {
            return;
        }
        int slot = count;
        while (slot > 0 && scores[row + slot - 1] > score) {
            slot--;
        }
        int last = Math.min(count, LIGHTS_PER_SECTION - 1);
        for (int entry = last; entry > slot; entry--) {
            scores[row + entry] = scores[row + entry - 1];
            int target = header + 4 + entry * 8;
            for (int component = 0; component < 8; component++) {
                upload.put(target + component, upload.get(target - 8 + component));
            }
        }
        scores[row + slot] = score;
        int first = header + 4 + slot * 8;
        upload.put(first, (float) (light.x() - x)).put(first + 1, (float) (light.y() - y))
                .put(first + 2, (float) (light.z() - z)).put(first + 3, radius)
                .put(first + 4, light.red()).put(first + 5, light.green()).put(first + 6, light.blue())
                .put(first + 7, light.emission() / (light.area() ? -15F : 15F));
        upload.put(header + 2, Math.min(count + 1, LIGHTS_PER_SECTION));
    }

    void clear() {
        dirty.clear();
        sourceReach = 0;
        lastRadius = -1;
        upload = null;
        scores = null;
        if (texture != 0) {
            GL11.glDeleteTextures(texture);
            texture = 0;
        }
    }

    private static double distanceToInterval(double point, int min, int max) {
        return Math.max(0, Math.max(min - point, point - max));
    }

    private static FloatBuffer directFloats(int size) {
        return ByteBuffer.allocateDirect(size * Float.BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer();
    }
}
