package dev.taste.engine.light;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class TiledLightGrid {
    public record ProjectedLight(int index, float centerX, float centerY, float radiusPixels, float influence) { }
    private final int tileSize;
    private final int maximumPerTile;
    private List<Integer>[] tiles = new List[0];
    private int columns;
    private int rows;
    public TiledLightGrid(int tileSize, int maximumPerTile) {
        this.tileSize = Math.max(4, tileSize);
        this.maximumPerTile = Math.max(1, maximumPerTile);
    }
    @SuppressWarnings("unchecked")
    public void build(int width, int height, List<ProjectedLight> lights) {
        columns = Math.max(1, (width + tileSize - 1) / tileSize);
        rows = Math.max(1, (height + tileSize - 1) / tileSize);
        tiles = new List[columns * rows];
        for (int i = 0; i < tiles.length; i++) tiles[i] = new ArrayList<>();
        List<ProjectedLight> ordered = lights.stream().sorted(Comparator.comparing(ProjectedLight::influence).reversed()).toList();
        for (ProjectedLight light : ordered) {
            int minX = clamp((int) ((light.centerX() - light.radiusPixels()) / tileSize), 0, columns - 1);
            int maxX = clamp((int) ((light.centerX() + light.radiusPixels()) / tileSize), 0, columns - 1);
            int minY = clamp((int) ((light.centerY() - light.radiusPixels()) / tileSize), 0, rows - 1);
            int maxY = clamp((int) ((light.centerY() + light.radiusPixels()) / tileSize), 0, rows - 1);
            for (int y = minY; y <= maxY; y++) for (int x = minX; x <= maxX; x++) {
                List<Integer> tile = tiles[y * columns + x];
                if (tile.size() < maximumPerTile) tile.add(light.index());
            }
        }
    }
    public List<Integer> tile(int x, int y) { return List.copyOf(tiles[y * columns + x]); }
    public int columns() { return columns; }
    public int rows() { return rows; }
    private static int clamp(int value, int minimum, int maximum) { return Math.max(minimum, Math.min(maximum, value)); }
}
