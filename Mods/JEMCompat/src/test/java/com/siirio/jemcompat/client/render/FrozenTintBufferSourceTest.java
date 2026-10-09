package com.siirio.jemcompat.client.render;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FrozenTintBufferSourceTest {
    @Test
    void appliesPaleCyanTintWithoutCrushingEntityColors() {
        assertEquals(115, FrozenTintBufferSource.tintColor(255, 0.45F));
        assertEquals(209, FrozenTintBufferSource.tintColor(255, 0.82F));
        assertEquals(255, FrozenTintBufferSource.tintColor(255, 1.0F));
    }

    @Test
    void softensAlphaForTranslucentRenderLayers() {
        assertEquals(209, FrozenTintBufferSource.tintAlpha(255));
    }
}
