package com.justenoughmods.achievementguide.integration.create;

import java.util.Map;
import net.minecraft.nbt.CompoundTag;

/** Observational receipt on the exact native grouped crafting job. */
public interface FactoryGridReceipt {
    CompoundTag jem$factoryReceipt();
    void jem$factoryReceipt(CompoundTag proof);
    Map<String, Long> jem$factoryContents();
    boolean jem$factoryHasItems();
}
