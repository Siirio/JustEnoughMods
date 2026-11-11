package dev.taste.engine.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import java.util.EnumMap;
import java.util.Map;

public final class FabulousLayerRouter {
    public enum Layer { ITEM_ENTITY, TRANSLUCENT, PARTICLES, WEATHER, CLOUDS }
    private final Map<Layer, RenderTarget> layers = new EnumMap<>(Layer.class);
    public void bind(Layer layer, RenderTarget target) { layers.put(layer, target); }
    public RenderTarget target(Layer layer) { return layers.get(layer); }
    public Map<Layer, RenderTarget> snapshot() { return Map.copyOf(layers); }
    public void clear() { layers.clear(); }
}
