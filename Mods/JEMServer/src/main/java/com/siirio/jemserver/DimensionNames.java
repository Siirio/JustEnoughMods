package com.siirio.jemserver;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** Reuse dimension language keys and retain the actual ID when none is provided. */
public final class DimensionNames {
    private DimensionNames() {}

    public static Component name(String value) {
        ResourceLocation id = ResourceLocation.tryParse(value);
        return id == null ? Component.literal(value) : name(id);
    }

    public static Component name(ResourceLocation id) {
        return Component.translatableWithFallback("dimension." + id.getNamespace() + "." + id.getPath(), id.toString());
    }
}
