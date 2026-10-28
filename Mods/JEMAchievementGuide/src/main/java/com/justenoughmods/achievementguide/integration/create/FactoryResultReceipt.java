package com.justenoughmods.achievementguide.integration.create;

import net.minecraft.nbt.CompoundTag;

/** Temporary native return-value handoff only, never ItemStack NBT or equality. */
public interface FactoryResultReceipt {
    CompoundTag jem$factoryResult();
    void jem$factoryResult(CompoundTag proof);
}
