package dev.taste.engine.render.forward;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;

/** Uses the native opaque entity state and vertex format with a different shader. */
final class EntityForwardTypes extends RenderStateShard {
    enum Layer {
        SOLID(true, false, true, false),
        CUTOUT(true, false, true, true),
        CUTOUT_NO_CULL(false, false, true, true),
        CUTOUT_NO_OUTLINE(false, false, false, true),
        CUTOUT_OFFSET(false, true, true, true),
        CUTOUT_OFFSET_NO_OUTLINE(false, true, false, true),
        ARMOR(false, true, true, true),
        PLAYER_TRANSLUCENT(false, false, true, true),
        PLAYER_TRANSLUCENT_NO_OUTLINE(false, false, false, true);

        private final boolean cull;
        private final boolean offset;
        private final boolean outline;
        private final boolean cutout;

        Layer(boolean cull, boolean offset, boolean outline, boolean cutout) {
            this.cull = cull;
            this.offset = offset;
            this.outline = outline;
            this.cutout = cutout;
        }

        boolean cutout() { return cutout; }
        boolean translucent() { return this == PLAYER_TRANSLUCENT || this == PLAYER_TRANSLUCENT_NO_OUTLINE; }
    }

    private record Key(ResourceLocation texture, Layer layer, int tint, Integer emissionTint) { }
    private static final int MAX_VARIANTS = 2048;
    private final Map<Key, RenderType> variants = new LinkedHashMap<>();

    EntityForwardTypes() { super("taste_entity_forward_owner", () -> { }, () -> { }); }

    RenderType get(ResourceLocation texture, Layer layer, int tint, Integer emissionTint, Supplier<ShaderInstance> shader) {
        Key key = new Key(texture, layer, tint, emissionTint);
        RenderType variant = variants.get(key);
        if (variant != null) return variant;
        if (variants.size() >= MAX_VARIANTS) return null;
        var state = RenderType.CompositeState.builder()
                .setShaderState(new ShaderStateShard(shader))
                .setTextureState(new TextureStateShard(texture, false, false))
                .setTransparencyState(layer.translucent() ? TRANSLUCENT_TRANSPARENCY : NO_TRANSPARENCY)
                .setCullState(layer.cull ? CULL : NO_CULL)
                .setLightmapState(LIGHTMAP)
                .setOverlayState(OVERLAY)
                .setLayeringState(layer.offset ? VIEW_OFFSET_Z_LAYERING : NO_LAYERING)
                .createCompositeState(layer.outline);
        RenderType created = RenderType.create("taste_entity_forward", DefaultVertexFormat.NEW_ENTITY,
                VertexFormat.Mode.QUADS, 256, true, layer.translucent(), state);
        variants.put(key, created);
        return created;
    }

    void clear() { variants.clear(); }

    static Layer bodyLayer(RenderType source, ResourceLocation texture, boolean knownTranslucent) {
        if (source == RenderType.entitySolid(texture)) return Layer.SOLID;
        if (source == RenderType.entityCutout(texture)) return Layer.CUTOUT;
        if (source == RenderType.entityCutoutNoCull(texture, true)) return Layer.CUTOUT_NO_CULL;
        if (source == RenderType.entityCutoutNoCull(texture, false)) return Layer.CUTOUT_NO_OUTLINE;
        if (source == RenderType.entityCutoutNoCullZOffset(texture, true)) return Layer.CUTOUT_OFFSET;
        if (source == RenderType.entityCutoutNoCullZOffset(texture, false)) return Layer.CUTOUT_OFFSET_NO_OUTLINE;
        if (knownTranslucent && source == RenderType.entityTranslucent(texture, true)) return Layer.PLAYER_TRANSLUCENT;
        if (knownTranslucent && source == RenderType.entityTranslucent(texture, false)) return Layer.PLAYER_TRANSLUCENT_NO_OUTLINE;
        return null;
    }
}
