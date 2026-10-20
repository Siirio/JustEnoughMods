package com.siirio.jemcompat.feature.backpack;

import com.mojang.serialization.Codec;
import com.mrcrayfish.backpacked.common.augment.Augment;
import com.mrcrayfish.backpacked.common.augment.AugmentType;
import net.minecraft.resources.ResourceLocation;

/** Native augment selection, enable state and unlocks remain Backpacked-owned. */
public record FurnaceAugment() implements Augment<FurnaceAugment> {
    public static final FurnaceAugment INSTANCE = new FurnaceAugment();
    public static final AugmentType<FurnaceAugment> TYPE = new AugmentType<>(
            new ResourceLocation("jemcompat", "furnace"), Codec.unit(INSTANCE), Augment.noOp(), Augment.unit(INSTANCE), () -> INSTANCE);
    @Override public AugmentType<FurnaceAugment> type() { return TYPE; }
}
