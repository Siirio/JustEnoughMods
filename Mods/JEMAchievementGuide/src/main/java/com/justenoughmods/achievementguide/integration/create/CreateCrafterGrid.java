package com.justenoughmods.achievementguide.integration.create;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/** Proof belongs to one native grouped ingredient job, including native save/load. */
public interface CreateCrafterGrid {
    UUID jem$supplier();
    boolean jem$bounded();
    boolean jem$hasIngredients();
    void jem$supplier(UUID actor);
    void jem$restore(CompoundTag proof);
    boolean jem$containsOutput(ItemStack output);
}
