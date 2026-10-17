package com.siirio.jemcurrency;

import net.minecraft.world.entity.Entity;

/** Existing native merchant markers, shared without loading full Market runtime. */
public final class NativeMerchantTags {
    private NativeMerchantTags() {}
    public static boolean isBuyer(Entity entity) { return entity.getTags().contains("jem_market_buyer"); }
    public static boolean isPreview(Entity entity) { return entity.getTags().contains("jem_market_preview"); }
}
