package com.justenoughmods.achievementguide.integration.food;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.util.FakePlayer;
import net.satisfy.vinery.core.block.AppleLeavesBlock;
import net.satisfy.vinery.core.block.DarkCherryLeavesBlock;

/** A single native harvest call ties its exact spawned fruit to its committed leaf reset. */
public final class OrchardFoodProof {
    private static final String RUN = "jemNativeOrchardRunV1";
    private static final String ENVELOPE = "jemNativeOrchardDropV1";
    private OrchardFoodProof() {}

    public static void begin(ServerPlayer actor, BlockPos pos, boolean emptyHand) {
        actor.getPersistentData().remove(RUN);
        if (!emptyHand || actor instanceof FakePlayer || actor.isSpectator() || actor.getAbilities().instabuild) return;
        CompoundTag run = new CompoundTag();
        run.putUUID("token", UUID.randomUUID());
        run.putLong("pos", pos.asLong());
        run.putLong("time", actor.serverLevel().getGameTime());
        actor.getPersistentData().put(RUN, run);
    }
    public static ItemStack outgoing(Player actor, BlockPos pos, BlockState before, ItemStack fruit) {
        if (!(actor instanceof ServerPlayer player) || !validFruit(before, fruit)) return fruit;
        CompoundTag run = player.getPersistentData().getCompound(RUN);
        if (!run.hasUUID("token") || run.getLong("pos") != pos.asLong()
                || run.getLong("time") != player.serverLevel().getGameTime()) return fruit;
        CompoundTag envelope = new CompoundTag();
        envelope.putUUID("actor", player.getUUID());
        envelope.putUUID("token", run.getUUID("token"));
        envelope.put("fruit", fruit.save(new CompoundTag()));
        ItemStack outgoing = fruit.copy();
        outgoing.getOrCreateTag().put(ENVELOPE, envelope);
        return outgoing;
    }
    public static boolean insert(Level level, Entity entity) {
        CompoundTag envelope = null;
        if (entity instanceof ItemEntity drop && drop.getItem().hasTag() && drop.getItem().getTag().contains(ENVELOPE)) {
            ItemStack clean = drop.getItem().copy();
            envelope = clean.getTag().getCompound(ENVELOPE).copy();
            clean.getTag().remove(ENVELOPE);
            if (clean.getTag().isEmpty()) clean.setTag(null);
            drop.setItem(clean);
            if (!clean.save(new CompoundTag()).equals(envelope.getCompound("fruit"))) envelope = null;
        }
        boolean added = level.addFreshEntity(entity);
        if (added && envelope != null && envelope.hasUUID("actor") && envelope.hasUUID("token")) {
            Player actor = level.getPlayerByUUID(envelope.getUUID("actor"));
            if (actor instanceof ServerPlayer player) {
                CompoundTag run = player.getPersistentData().getCompound(RUN);
                if (run.hasUUID("token") && run.getUUID("token").equals(envelope.getUUID("token"))
                        && run.getLong("time") == level.getGameTime()) {
                    run.putUUID("drop", entity.getUUID());
                    run.put("fruit", envelope.getCompound("fruit").copy());
                }
            }
        }
        return added;
    }
    public static void finish(ServerPlayer actor, BlockPos pos, BlockState before, BlockState after, boolean success) {
        CompoundTag run = actor.getPersistentData().getCompound(RUN).copy();
        actor.getPersistentData().remove(RUN);
        if (!success || !run.hasUUID("drop") || run.getLong("pos") != pos.asLong()
                || run.getLong("time") != actor.serverLevel().getGameTime()) return;
        ItemStack fruit = ItemStack.of(run.getCompound("fruit"));
        if (!validFruit(before, fruit) || !reset(before).equals(after)) return;
        Entity entity = actor.serverLevel().getEntity(run.getUUID("drop"));
        if (entity instanceof ItemEntity drop && drop.isAlive() && ItemStack.matches(drop.getItem(), fruit))
            FoodDroppedProof.update(drop, "v3_food_orchard", fruit.getCount(), actor.getUUID());
    }
    private static boolean validFruit(BlockState state, ItemStack fruit) {
        if (state.getBlock() instanceof AppleLeavesBlock)
            return state.getValue(AppleLeavesBlock.HAS_APPLES) && state.getValue(AppleLeavesBlock.AGE) == 3
                    && FoodOutputProof.item(fruit, "minecraft:apple");
        if (state.getBlock() instanceof DarkCherryLeavesBlock)
            return state.getValue(DarkCherryLeavesBlock.HAS_CHERRIES) && state.getValue(DarkCherryLeavesBlock.AGE) == 3
                    && FoodOutputProof.item(fruit, "vinery:cherry");
        return false;
    }
    private static BlockState reset(BlockState before) {
        if (before.getBlock() instanceof AppleLeavesBlock)
            return before.setValue(AppleLeavesBlock.HAS_APPLES, false).setValue(AppleLeavesBlock.AGE, 1);
        return before.setValue(DarkCherryLeavesBlock.HAS_CHERRIES, false).setValue(DarkCherryLeavesBlock.AGE, 1);
    }
}
