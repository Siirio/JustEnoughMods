package com.justenoughmods.achievementguide.integration.create;

import net.minecraft.nbt.CompoundTag;

/** Native stack carrier extension, deliberately outside item tags and capability equality. */
public interface CreateParcelCarrier {
    CompoundTag jem$parcelProof();
    void jem$parcelProof(CompoundTag proof);
}
