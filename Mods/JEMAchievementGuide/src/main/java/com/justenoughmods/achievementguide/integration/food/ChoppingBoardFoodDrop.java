package com.justenoughmods.achievementguide.integration.food;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.util.FakePlayer;

/** Only the actor completing a native knife cut can receive its source receipt. */
public final class ChoppingBoardFoodDrop {
    private static final String ENVELOPE = "jemNativeKnifeBoardEnvelopeV1";
    private ChoppingBoardFoodDrop() {}
    public static ItemStack outgoing(BlockEntity station, ItemStack output, ServerPlayer actor) {
        ItemStack result = output;
        if (!(actor instanceof FakePlayer) && !actor.isSpectator() && !actor.getAbilities().instabuild
                && !CookeryFoodProof.recipe(station, output).isEmpty()) {
            result = output.copy();
            CompoundTag proof = new CompoundTag();
            proof.putUUID("actor", actor.getUUID());
            proof.put("output", output.save(new CompoundTag()));
            proof.putString("action", CookeryFoodProof.recipe(station, output)
                    .equals("kaleidoscope_cookery:chopping_board/raw_noodles_from_raw_dough")
                    && FoodOutputProof.item(output, "kaleidoscope_cookery:raw_noodles")
                    ? "food_board_noodles" : "v3_food_knife_board");
            result.getOrCreateTag().put(ENVELOPE, proof);
        }
        CookeryFoodProof.clear(station);
        return result;
    }
    public static void beforeInsertion(Entity entity) {
        if (!(entity instanceof ItemEntity drop) || !drop.getItem().hasTag() || !drop.getItem().getTag().contains(ENVELOPE)) return;
        ItemStack clean = drop.getItem().copy();
        CompoundTag proof = clean.getTag().getCompound(ENVELOPE).copy();
        clean.getTag().remove(ENVELOPE);
        if (clean.getTag().isEmpty()) clean.setTag(null);
        drop.setItem(clean);
        if (proof.hasUUID("actor") && !clean.isEmpty() && clean.save(new CompoundTag()).equals(proof.getCompound("output")))
            FoodDroppedProof.update(drop, proof.getString("action").equals("food_board_noodles")
                    ? "food_board_noodles" : "v3_food_knife_board", clean.getCount(), proof.getUUID("actor"));
    }
}
