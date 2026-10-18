package com.siirio.jemcompat.feature.backpack;

import com.mojang.serialization.Codec;
import com.mrcrayfish.backpacked.common.augment.Augment;
import com.mrcrayfish.backpacked.common.augment.AugmentType;
import net.minecraft.resources.ResourceLocation;

/** Native codec/network augment; crafting contents stay on the native backpack stack. */
public record WorkbenchAugment() implements Augment<WorkbenchAugment> {
    public static final WorkbenchAugment INSTANCE = new WorkbenchAugment();
    public static final AugmentType<WorkbenchAugment> TYPE = new AugmentType<>(
            new ResourceLocation("jemcompat", "workbench"), Codec.unit(INSTANCE), Augment.noOp(), Augment.unit(INSTANCE), () -> INSTANCE);

    @Override public AugmentType<WorkbenchAugment> type() { return TYPE; }
}
