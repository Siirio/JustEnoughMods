package com.justenoughmods.achievementguide.integration.food;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/** One pending native recipe result for stations without Container slots. */
public final class CookeryFoodProof {
    public static final String KEY = "jemCookeryRecipeReceiptV1";
    private CookeryFoodProof() {}
    public static void clear(BlockEntity owner) { owner.getPersistentData().remove(KEY); owner.setChanged(); }
    public static void certify(BlockEntity owner, String recipe, ItemStack result) {
        if (owner.getLevel() == null || owner.getLevel().isClientSide || result.isEmpty()) return;
        CompoundTag proof = new CompoundTag();
        proof.putString("recipe", recipe);
        proof.put("result", result.save(new CompoundTag()));
        owner.getPersistentData().put(KEY, proof);
        owner.setChanged();
    }
    public static String recipe(BlockEntity owner, ItemStack result) {
        CompoundTag proof = owner.getPersistentData().getCompound(KEY);
        return !result.isEmpty() && result.save(new CompoundTag()).equals(proof.getCompound("result")) ? proof.getString("recipe") : "";
    }
    public static void save(BlockEntity owner, CompoundTag tag) { tag.put(KEY, owner.getPersistentData().getCompound(KEY).copy()); }
    public static void load(BlockEntity owner, CompoundTag tag) { owner.getPersistentData().put(KEY, tag.getCompound(KEY).copy()); }
}
