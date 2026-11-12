package dev.taste.engine.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import dev.taste.engine.mixin.client.LevelRendererTransparencyAccessor;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.PostChain;

/** Temporarily routes native Fabulous render states to targets at the current world resolution. */
public final class FabulousLayerRouter implements AutoCloseable {
    enum Layer {
        TRANSLUCENT("Translucent"), ITEM_ENTITY("ItemEntity"), PARTICLES("Particles"),
        WEATHER("Weather"), CLOUDS("Clouds");

        final String sampler;
        Layer(String sampler) { this.sampler = sampler; }
        String targetName() { return "fabulous-" + name().toLowerCase(java.util.Locale.ROOT); }
    }

    private final Map<Layer, RenderTarget> layers = new EnumMap<>(Layer.class);
    private final HdrLayerCompositor compositor = new HdrLayerCompositor();
    private LevelRendererTransparencyAccessor access;
    private RenderTarget[] nativeLayers;
    private RenderTarget composite;

    public void route(LevelRenderer renderer, TargetPool targets, int width, int height) throws IOException {
        restore();
        LevelRendererTransparencyAccessor candidate = (LevelRendererTransparencyAccessor) renderer;
        if (candidate.taste$transparencyChain() == null) {
            for (Layer layer : Layer.values()) targets.release(layer.targetName());
            targets.release(HdrLayerCompositor.TARGET);
            return;
        }
        compositor.prepare();
        for (Layer layer : Layer.values()) {
            layers.put(layer, targets.acquire(descriptor(layer.targetName(), width, height, true)));
        }
        composite = targets.acquire(descriptor(HdrLayerCompositor.TARGET, width, height, false));
        nativeLayers = new RenderTarget[] {renderer.getTranslucentTarget(), renderer.getItemEntityTarget(),
                renderer.getParticlesTarget(), renderer.getWeatherTarget(), renderer.getCloudsTarget()};
        access = candidate;
        for (Layer layer : Layer.values()) set(layer, layers.get(layer));
    }

    public boolean compose(PostChain chain, RenderTarget world, Runnable draw) {
        if (access == null || access.taste$transparencyChain() != chain) return false;
        compositor.compose(world, composite, layers, draw);
        return true;
    }

    public void restore() {
        if (access != null) {
            for (Layer layer : Layer.values()) set(layer, nativeLayers[layer.ordinal()]);
        }
        access = null;
        nativeLayers = null;
        composite = null;
        layers.clear();
    }

    private void set(Layer layer, RenderTarget target) {
        switch (layer) {
            case TRANSLUCENT -> access.taste$setTranslucentTarget(target);
            case ITEM_ENTITY -> access.taste$setItemEntityTarget(target);
            case PARTICLES -> access.taste$setParticlesTarget(target);
            case WEATHER -> access.taste$setWeatherTarget(target);
            case CLOUDS -> access.taste$setCloudsTarget(target);
        }
    }

    private static TargetDescriptor descriptor(String name, int width, int height, boolean depth) {
        return new TargetDescriptor(name, width, height, TargetDescriptor.Format.RGBA16F, depth, 1, 1, 1);
    }

    @Override public void close() {
        restore();
        compositor.close();
    }
}
