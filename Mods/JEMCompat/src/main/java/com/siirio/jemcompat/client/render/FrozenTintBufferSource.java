package com.siirio.jemcompat.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

public final class FrozenTintBufferSource implements MultiBufferSource {
    private static final float RED_MULTIPLIER = 0.45F;
    private static final float GREEN_MULTIPLIER = 0.82F;
    private static final float BLUE_MULTIPLIER = 1.0F;
    private static final float ALPHA_MULTIPLIER = 0.82F;

    private final MultiBufferSource delegate;

    public FrozenTintBufferSource(MultiBufferSource delegate) {
        this.delegate = delegate;
    }

    @Override
    public VertexConsumer getBuffer(RenderType renderType) {
        if (renderType.format() != DefaultVertexFormat.NEW_ENTITY) {
            return delegate.getBuffer(renderType);
        }
        return new FrozenTintVertexConsumer(delegate.getBuffer(renderType));
    }

    private static final class FrozenTintVertexConsumer implements VertexConsumer {
        private final VertexConsumer delegate;

        private FrozenTintVertexConsumer(VertexConsumer delegate) {
            this.delegate = delegate;
        }

        @Override
        public VertexConsumer vertex(double x, double y, double z) {
            delegate.vertex(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer color(int red, int green, int blue, int alpha) {
            delegate.color(tintColor(red, RED_MULTIPLIER), tintColor(green, GREEN_MULTIPLIER),
                    tintColor(blue, BLUE_MULTIPLIER), tintAlpha(alpha));
            return this;
        }

        @Override
        public VertexConsumer uv(float u, float v) {
            delegate.uv(u, v);
            return this;
        }

        @Override
        public VertexConsumer overlayCoords(int u, int v) {
            delegate.overlayCoords(u, v);
            return this;
        }

        @Override
        public VertexConsumer uv2(int u, int v) {
            delegate.uv2(u, v);
            return this;
        }

        @Override
        public VertexConsumer normal(float x, float y, float z) {
            delegate.normal(x, y, z);
            return this;
        }

        @Override
        public void endVertex() {
            delegate.endVertex();
        }

        @Override
        public void defaultColor(int red, int green, int blue, int alpha) {
            delegate.defaultColor(tintColor(red, RED_MULTIPLIER), tintColor(green, GREEN_MULTIPLIER),
                    tintColor(blue, BLUE_MULTIPLIER), tintAlpha(alpha));
        }

        @Override
        public void unsetDefaultColor() {
            delegate.unsetDefaultColor();
        }

    }

    static int tintColor(int color, float multiplier) {
        return Math.min(255, Math.max(0, Math.round(color * multiplier)));
    }

    static int tintAlpha(int alpha) {
        return tintColor(alpha, ALPHA_MULTIPLIER);
    }
}
