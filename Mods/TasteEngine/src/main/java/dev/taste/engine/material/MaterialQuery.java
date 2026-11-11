package dev.taste.engine.material;

import java.util.Set;
import net.minecraft.resources.ResourceLocation;

/** Stable identifiers only. World and renderer objects do not escape into material caches. */
public record MaterialQuery(ResourceLocation block, ResourceLocation fluid, ResourceLocation sprite,
        Set<ResourceLocation> tags, boolean translucent, boolean cutout, int tintIndex) {
    public MaterialQuery { tags = Set.copyOf(tags); }
}
