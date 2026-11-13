package com.siirio.jemlightingsandbox.terrain;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.material.DefaultMaterials;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.material.Material;

public final class WaterMaterial extends Material {
    // The stock shader reads bits 0–2. Both Embeddium encoders reserve a full byte.
    private static final int WATER_BIT = 0x80;
    private static final Material TRANSLUCENT = new WaterMaterial(DefaultMaterials.TRANSLUCENT);
    private static final Map<Material, Material> OTHER_LAYERS = new ConcurrentHashMap<>();

    private final int encoded;

    private WaterMaterial(Material original) {
        super(original.pass, original.alphaCutoff, original.mipped);
        encoded = original.bits() | WATER_BIT;
    }

    public static Material flagged(Material original) {
        if (original == DefaultMaterials.TRANSLUCENT) {
            return TRANSLUCENT;
        }
        if ((original.bits() & WATER_BIT) != 0) {
            return original;
        }
        return OTHER_LAYERS.computeIfAbsent(original, WaterMaterial::new);
    }

    @Override
    public int bits() {
        return encoded;
    }
}
