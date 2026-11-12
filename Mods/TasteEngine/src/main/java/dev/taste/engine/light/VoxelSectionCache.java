package dev.taste.engine.light;

import dev.taste.engine.material.MaterialResolver;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.chunk.LevelChunkSection;

/** Bounded immutable section snapshots shared by consumers on the render thread. */
public final class VoxelSectionCache {
    private record Entry(LevelChunkSection section, VoxelSectionGeometry.Mesh mesh) { }
    private final Map<Long, Entry> entries = new LinkedHashMap<>(32, 0.75F, true);
    private final VoxelSectionGeometry geometry = new VoxelSectionGeometry();
    private final long maximumBytes;
    private ClientLevel owner;
    private long bytes;

    public VoxelSectionCache(long maximumBytes) {
        if (maximumBytes <= 0) throw new IllegalArgumentException("Invalid section mesh budget");
        this.maximumBytes = maximumBytes;
    }

    public VoxelSectionGeometry.Mesh get(ClientLevel level, VoxelSectionGeometry.Coordinate coordinate,
            MaterialResolver materials) {
        if (owner != level) {
            clear();
            owner = level;
        }
        int index = coordinate.y() - level.getMinSection();
        if (index < 0 || index >= level.getSectionsCount()) return null;
        long key = coordinate.key();
        var chunk = level.getChunkSource().getChunk(coordinate.x(), coordinate.z(), false);
        if (chunk == null) {
            remove(key);
            return null;
        }
        LevelChunkSection section = chunk.getSections()[index];
        Entry cached = entries.get(key);
        if (cached != null && cached.section() == section) return cached.mesh();
        remove(key);
        VoxelSectionGeometry.Mesh mesh = geometry.emit(level, coordinate, materials);
        long size = sizeOf(mesh);
        if (size > maximumBytes) return mesh;
        while (bytes + size > maximumBytes || entries.size() >= 2048) {
            remove(entries.keySet().iterator().next());
        }
        entries.put(key, new Entry(section, mesh));
        bytes += size;
        return mesh;
    }

    public void dirty(BlockPos position) {
        int x = SectionPos.blockToSectionCoord(position.getX());
        int y = SectionPos.blockToSectionCoord(position.getY());
        int z = SectionPos.blockToSectionCoord(position.getZ());
        int inX = position.getX() & 15;
        int inY = position.getY() & 15;
        int inZ = position.getZ() & 15;
        for (int dz = inZ == 0 ? -1 : 0; dz <= (inZ == 15 ? 1 : 0); dz++) {
            for (int dy = inY == 0 ? -1 : 0; dy <= (inY == 15 ? 1 : 0); dy++) {
                for (int dx = inX == 0 ? -1 : 0; dx <= (inX == 15 ? 1 : 0); dx++) {
                    remove(SectionPos.asLong(x + dx, y + dy, z + dz));
                }
            }
        }
    }

    public void clear() {
        entries.clear();
        owner = null;
        bytes = 0;
    }

    private void remove(long key) {
        Entry removed = entries.remove(key);
        if (removed != null) bytes -= sizeOf(removed.mesh());
    }

    private static long sizeOf(VoxelSectionGeometry.Mesh mesh) { return mesh == null ? 64L : 64L + mesh.estimatedBytes(); }
}
