package com.justenoughmods.achievementguide.integration.food;

import com.github.ysbbbbbb.kaleidoscopecookery.blockentity.kitchen.MillstoneBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Four physical native output slots, certified only after actual completed grain processing. */
public final class MillstoneFoodProof {
    private static final String KEY = "jemNativeMillstoneFlourV1";
    private MillstoneFoodProof() {}
    public static void clear(BlockEntity owner) { owner.getPersistentData().remove(KEY); }
    public static void certify(MillstoneBlockEntity owner) {
        if (owner.getLevel() == null || owner.getLevel().isClientSide) return;
        CompoundTag rows = new CompoundTag();
        for (int slot = 0; slot < Math.min(4, owner.getOutputs().getSlots()); slot++) {
            ItemStack output = owner.getOutputs().getStackInSlot(slot);
            if (FoodOutputProof.item(output, "kaleidoscope_cookery:flour")) rows.put(Integer.toString(slot), output.save(new CompoundTag()));
        }
        owner.getPersistentData().put(KEY, rows);
        owner.setChanged();
    }
    public static void beforeInsertion(MillstoneBlockEntity owner, Entity nativeEntity) {
        if (!(nativeEntity instanceof ItemEntity drop)) return;
        CompoundTag rows = owner.getPersistentData().getCompound(KEY);
        for (int slot = 0; slot < Math.min(4, owner.getOutputs().getSlots()); slot++) {
            ItemStack output = owner.getOutputs().getStackInSlot(slot);
            if (output != drop.getItem()) continue;
            CompoundTag expected = rows.getCompound(Integer.toString(slot));
            rows.remove(Integer.toString(slot));
            if (!output.isEmpty() && output.save(new CompoundTag()).equals(expected))
                FoodDroppedProof.update(drop, "food_mill_flour", output.getCount(), null);
            owner.setChanged();
            return;
        }
    }
    public static void save(BlockEntity owner, CompoundTag tag) { tag.put(KEY, owner.getPersistentData().getCompound(KEY).copy()); }
    public static void load(BlockEntity owner, CompoundTag tag) {
        CompoundTag saved = tag.getCompound(KEY), bounded = new CompoundTag();
        for (int slot = 0; slot < 4; slot++) {
            String key = Integer.toString(slot);
            if (saved.contains(key)) bounded.put(key, saved.getCompound(key).copy());
        }
        owner.getPersistentData().put(KEY, bounded);
    }
}
