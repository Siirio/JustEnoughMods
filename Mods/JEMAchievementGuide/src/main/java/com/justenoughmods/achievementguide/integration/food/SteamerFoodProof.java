package com.justenoughmods.achievementguide.integration.food;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Eight pending physical steamer results, consumed before their native transfer. */
public final class SteamerFoodProof {
    public static final String KEY = "jemQuestSteamerFoodV1";
    private SteamerFoodProof() {}
    public static void invalidate(BlockEntity owner, int slot) {
        if (slot >= 0 && slot < 8) owner.getPersistentData().getCompound(KEY).remove(Integer.toString(slot));
    }
    public static void certify(BlockEntity owner, int slot, ItemStack output) {
        if (owner.getLevel() == null || owner.getLevel().isClientSide || slot < 0 || slot >= 8
                || !FoodOutputProof.item(output, "kaleidoscope_cookery:mantou")) return;
        CompoundTag rows = owner.getPersistentData().getCompound(KEY);
        rows.put(Integer.toString(slot), output.save(new CompoundTag()));
        owner.getPersistentData().put(KEY, rows);
        owner.setChanged();
    }
    public static boolean consume(BlockEntity owner, int slot, ItemStack output) {
        boolean matches = !output.isEmpty() && output.save(new CompoundTag())
                .equals(owner.getPersistentData().getCompound(KEY).getCompound(Integer.toString(slot)));
        invalidate(owner, slot);
        owner.setChanged();
        return matches;
    }
    public static void save(BlockEntity owner, CompoundTag tag) { tag.put(KEY, owner.getPersistentData().getCompound(KEY).copy()); }
    public static void load(BlockEntity owner, CompoundTag tag) { owner.getPersistentData().put(KEY, tag.getCompound(KEY).copy()); }
}
