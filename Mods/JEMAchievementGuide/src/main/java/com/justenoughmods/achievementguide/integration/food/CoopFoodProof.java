package com.justenoughmods.achievementguide.integration.food;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.satisfy.farm_and_charm.core.block.entity.ChickenCoopBlockEntity;

/** Native produced portions remain bounded by the actual coop egg count. */
public final class CoopFoodProof {
    private static final String KEY = "jemNativeCoopEggsV1";
    private static final String RUN = "jemNativeCoopReceiveV1";
    private CoopFoodProof() {}
    public static int known(ChickenCoopBlockEntity owner) {
        CompoundTag proof = owner.getPersistentData().getCompound(KEY);
        return proof.getInt("total") == owner.getEggCount() ? Math.max(0, Math.min(owner.getEggCount(), proof.getInt("known"))) : 0;
    }
    public static void clear(BlockEntity owner) { owner.getPersistentData().remove(KEY); }
    public static void certify(ChickenCoopBlockEntity owner, int known) {
        if (owner.getLevel() == null || owner.getLevel().isClientSide || known < 1) return;
        CompoundTag proof = new CompoundTag();
        proof.putInt("total", owner.getEggCount());
        proof.putInt("known", Math.min(known, owner.getEggCount()));
        owner.getPersistentData().put(KEY, proof);
        owner.setChanged();
    }
    public static boolean deliver(ChickenCoopBlockEntity owner, ServerPlayer actor, ItemStack nativeEggs) {
        owner.getPersistentData().remove(RUN);
        int total = owner.getEggCount(), known = known(owner);
        ItemStack expected = nativeEggs.copy();
        int held = FoodOutputProof.held(actor, actor.containerMenu, expected);
        boolean result = actor.addItem(nativeEggs);
        int received = FoodOutputProof.held(actor, actor.containerMenu, expected) - held;
        if (known > 0 && expected.getCount() == total && FoodOutputProof.item(expected, "minecraft:egg")
                && Math.max(0, received - (total - known)) > 0) {
            CompoundTag run = new CompoundTag();
            run.putUUID("actor", actor.getUUID());
            owner.getPersistentData().put(RUN, run);
        }
        return result;
    }
    public static boolean consumeDelivery(ChickenCoopBlockEntity owner, ServerPlayer actor) {
        CompoundTag run = owner.getPersistentData().getCompound(RUN);
        boolean valid = run.hasUUID("actor") && run.getUUID("actor").equals(actor.getUUID());
        owner.getPersistentData().remove(RUN);
        return valid;
    }
    public static void save(BlockEntity owner, CompoundTag tag) { tag.put(KEY, owner.getPersistentData().getCompound(KEY).copy()); }
    public static void load(BlockEntity owner, CompoundTag tag) {
        owner.getPersistentData().put(KEY, tag.getCompound(KEY).copy());
        owner.getPersistentData().remove(RUN);
    }
}
