package dev.taste.engine.light;

public final class VoxelClipmaps {
    public record Clipmap(int index, int resolution, float voxelSize, int originX, int originY, int originZ) { }
    private final Clipmap[] levels;
    public VoxelClipmaps(int count, int resolution, float nearVoxelSize) {
        if (count < 1 || resolution < 8 || nearVoxelSize <= 0) throw new IllegalArgumentException("Invalid clipmap layout");
        levels = new Clipmap[count];
        for (int i = 0; i < count; i++) levels[i] = new Clipmap(i, resolution, nearVoxelSize * (1 << i), 0, 0, 0);
    }
    public Clipmap[] center(double cameraX, double cameraY, double cameraZ) {
        for (int i = 0; i < levels.length; i++) {
            Clipmap current = levels[i];
            float size = current.voxelSize();
            levels[i] = new Clipmap(i, current.resolution(), size,
                    (int) Math.floor(cameraX / size), (int) Math.floor(cameraY / size), (int) Math.floor(cameraZ / size));
        }
        return levels.clone();
    }
}
