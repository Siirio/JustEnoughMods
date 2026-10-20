package com.siirio.jemcompat.feature.backpack;

import com.mojang.serialization.Codec;
import com.mrcrayfish.backpacked.common.augment.Augment;
import com.mrcrayfish.backpacked.common.augment.AugmentType;
import net.minecraft.resources.ResourceLocation;

/** Native augment selection, enable state and unlocks remain Backpacked-owned. */
public record AutoFeedAugment() implements Augment<AutoFeedAugment> {
    public static final AutoFeedAugment INSTANCE = new AutoFeedAugment();
    public static final AugmentType<AutoFeedAugment> TYPE = new AugmentType<>(
            new ResourceLocation("jemcompat", "auto_feed"), Codec.unit(INSTANCE), Augment.noOp(), Augment.unit(INSTANCE), () -> INSTANCE);
    @Override public AugmentType<AutoFeedAugment> type() { return TYPE; }
}
