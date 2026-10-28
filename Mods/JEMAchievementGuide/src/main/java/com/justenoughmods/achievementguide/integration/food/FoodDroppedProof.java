package com.justenoughmods.achievementguide.integration.food;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/** A native method-argument envelope is removed before the actual ItemEntity enters the world. */
public final class FoodDroppedProof {
    public static final String KEY = "jemQuestDroppedFoodV1";
    private static final String ENVELOPE = "jemQuestNativeFoodEnvelopeV1";
    private FoodDroppedProof() {}

    public static ItemStack outgoing(BlockEntity station, ItemStack output) {
        CompoundTag proof = station.getPersistentData().getCompound(FoodOutputProof.KEY);
        ItemStack result = output;
        if (FoodOutputProof.matches(proof, output) && proof.getString("action").equals("food_mincer_flour")) {
            result = output.copy();
            result.getOrCreateTag().put(ENVELOPE, proof.copy());
        }
        station.getPersistentData().remove(FoodOutputProof.KEY);
        station.setChanged();
        return result;
    }

    public static void beforeWorldInsertion(Entity entity) {
        if (!(entity instanceof ItemEntity drop) || !drop.getItem().hasTag() || !drop.getItem().getTag().contains(ENVELOPE)) return;
        ItemStack clean = drop.getItem().copy();
        CompoundTag proof = clean.getTag().getCompound(ENVELOPE).copy();
        clean.getTag().remove(ENVELOPE);
        if (clean.getTag().isEmpty()) clean.setTag(null);
        drop.setItem(clean);
        if (FoodOutputProof.matches(proof, clean) && proof.getString("action").equals("food_mincer_flour")) drop.getPersistentData().put(KEY, proof);
    }

    public static int known(ItemEntity drop) {
        CompoundTag proof = drop.getPersistentData().getCompound(KEY);
        return FoodOutputProof.matches(proof, drop.getItem()) ? proof.getInt("known") : 0;
    }

    public static UUID actor(ItemEntity drop) {
        CompoundTag proof = drop.getPersistentData().getCompound(KEY);
        return proof.hasUUID("actor") ? proof.getUUID("actor") : null;
    }
    public static void emitCollected(ServerPlayer actor, String action) {
        if(RemainingFoodActors.real(actor)==null)return;
        if (action.equals("food_board_noodles")) {
            QuestActionTrigger.INSTANCE.emit(actor, "food_dough_noodles");
            QuestActionTrigger.INSTANCE.emit(actor, "v3_food_knife_board");
        } else QuestActionTrigger.INSTANCE.emit(actor, action);
    }
    public static void update(ItemEntity drop, String action, int known) { update(drop, action, known, actor(drop)); }
    public static void update(ItemEntity drop, String action, int known, UUID actor) {
        if (known < 1 || drop.getItem().isEmpty()) { drop.getPersistentData().remove(KEY); return; }
        CompoundTag proof = new CompoundTag();
        proof.putString("action", action);
        if (actor != null) proof.putUUID("actor", actor);
        proof.putInt("known", Math.min(known, drop.getItem().getCount()));
        proof.put("output", drop.getItem().save(new CompoundTag()));
        drop.getPersistentData().put(KEY, proof);
    }
}
