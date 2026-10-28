package com.justenoughmods.achievementguide.integration.food;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/** At most 81 pending native silo outputs. No item or recipe is created here. */
public final class SiloFoodProof {
    public static final String KEY = "jemQuestSiloFoodV1";
    private static final String ENVELOPE = "jemQuestNativeSiloEnvelopeV1";
    private SiloFoodProof() {}

    public static void invalidate(BlockEntity owner, int slot) {
        if (slot < 81 || slot >= 162) return;
        owner.getPersistentData().getCompound(KEY).remove(Integer.toString(slot));
    }
    public static void certify(BlockEntity owner, int slot, ItemStack output) {
        if (owner.getLevel() == null || owner.getLevel().isClientSide || slot < 81 || slot >= 162
                || !FoodOutputProof.item(output, "brewery:dried_wheat")) return;
        CompoundTag rows = owner.getPersistentData().getCompound(KEY);
        rows.put(Integer.toString(slot), output.save(new CompoundTag()));
        owner.getPersistentData().put(KEY, rows);
        owner.setChanged();
    }
    /** Native dropStack splits and consumes this same physical slot stack. */
    public static void prepare(BlockEntity owner, int slot, ItemStack output) {
        CompoundTag expected = owner.getPersistentData().getCompound(KEY).getCompound(Integer.toString(slot));
        invalidate(owner, slot);
        if (!output.isEmpty() && output.save(new CompoundTag()).equals(expected)) {
            output.getOrCreateTag().put(ENVELOPE, expected.copy());
            owner.setChanged();
        }
    }
    public static void clean(ItemStack output) {
        if (output.hasTag()) {
            output.getTag().remove(ENVELOPE);
            if (output.getTag().isEmpty()) output.setTag(null);
        }
    }
    /** Every split is a portion of a completely certified native slot, never a copied inventory item. */
    public static void beforeInsertion(Entity entity) {
        if (!(entity instanceof ItemEntity drop) || !drop.getItem().hasTag()
                || !drop.getItem().getTag().contains(ENVELOPE)) return;
        ItemStack clean = drop.getItem().copy();
        ItemStack original = ItemStack.of(clean.getTag().getCompound(ENVELOPE));
        clean(clean);
        drop.setItem(clean);
        if (!FoodOutputProof.item(clean, "brewery:dried_wheat") || clean.isEmpty()
                || clean.getCount() > original.getCount() || !ItemStack.isSameItemSameTags(clean, original)) return;
        FoodDroppedProof.update(drop, "food/grain_dried", clean.getCount());
    }
    public static void save(BlockEntity owner, CompoundTag tag) { tag.put(KEY, owner.getPersistentData().getCompound(KEY).copy()); }
    public static void load(BlockEntity owner, CompoundTag tag) { owner.getPersistentData().put(KEY, tag.getCompound(KEY).copy()); }
}
