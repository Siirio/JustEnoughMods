package dev.taste.engine.environment;

import java.util.List;

public final class CloudLayers {
    public record Layer(float baseHeight, float thickness, float coverage, float density, float speed,
            float red, float green, float blue) {
        public Layer { if (thickness < 0 || coverage < 0 || coverage > 1 || density < 0 || speed < 0) throw new IllegalArgumentException("Invalid cloud layer"); }
    }
    private List<Layer> layers = List.of();
    public void configure(List<Layer> next) { if (next.size() > 3) throw new IllegalArgumentException("At most three cloud layers"); layers = List.copyOf(next); }
    public List<Layer> layers() { return layers; }
    public boolean inside(float cameraY, Layer layer) { return cameraY >= layer.baseHeight() && cameraY <= layer.baseHeight() + layer.thickness(); }
}
