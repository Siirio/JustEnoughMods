package dev.taste.engine.render.forward;

import com.mojang.blaze3d.vertex.VertexConsumer;

/** Stores material bits above the native overlay coordinates and below native lightmap indices. */
final class EntityMaterialVertices implements VertexConsumer {
    private final VertexConsumer output;
    private final int overlayU;
    private final int overlayV;
    private final int lightU;
    private final int lightV;

    EntityMaterialVertices(VertexConsumer output, float roughness, float metallic, float f0, float wetness,
            float subsurface, float emission, boolean skin, boolean cutout, boolean rim, boolean armorReflection) {
        this.output = output;
        overlayU = quantize(roughness) << 4 | quantize(metallic) << 8 | bit(skin, 12) | bit(cutout, 13);
        overlayV = quantize(f0 / 0.25F) << 4 | quantize(wetness) << 8 | bit(rim, 12) | bit(armorReflection, 13);
        lightU = quantize(subsurface);
        lightV = quantize(emission / 3.0F);
    }

    @Override public VertexConsumer vertex(double x, double y, double z) { output.vertex(x, y, z); return this; }

    @Override public VertexConsumer color(int r, int g, int b, int a) { output.color(r, g, b, a); return this; }

    @Override public VertexConsumer uv(float u, float v) { output.uv(u, v); return this; }

    @Override public VertexConsumer overlayCoords(int u, int v) {
        EntityForwardLighting.markOverlay(u, v);
        output.overlayCoords((u & 15) | overlayU, (v & 15) | overlayV);
        return this;
    }

    @Override public VertexConsumer uv2(int u, int v) {
        output.uv2((u & ~15) | lightU, (v & ~15) | lightV);
        return this;
    }

    @Override public VertexConsumer normal(float x, float y, float z) { output.normal(x, y, z); return this; }

    @Override public void endVertex() { output.endVertex(); }

    @Override public void defaultColor(int r, int g, int b, int a) { output.defaultColor(r, g, b, a); }

    @Override public void unsetDefaultColor() { output.unsetDefaultColor(); }

    private static int bit(boolean value, int index) { return value ? 1 << index : 0; }

    private static int quantize(float value) { return Math.round(Math.max(0.0F, Math.min(1.0F, value)) * 15.0F); }
}
