package com.justenoughmods.achievementguide.integration.create;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

public interface CreateTransportProof {
    CompoundTag jem$transportProof();
    void jem$transportProof(CompoundTag proof);
    UUID jem$supplier();
    void jem$nativeAdvance(ItemStack output, String recipe, int step, boolean completed);
    void jem$applyAdvance(CreateTransportProof output, ItemStack item);
    boolean jem$precisionCompleted();
    void jem$clearAdvance();
}
