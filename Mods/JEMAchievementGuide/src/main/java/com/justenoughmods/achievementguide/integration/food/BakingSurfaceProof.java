package com.justenoughmods.achievementguide.integration.food;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.util.FakePlayer;
import net.satisfy.bakery.core.block.BakerStationBlock;
import net.satisfy.bakery.core.block.cake.BlankCakeBlock;
import net.satisfy.bakery.core.registry.ObjectRegistry;
import net.satisfy.bakery.core.registry.TagsRegistry;

/** Ephemeral action context is attached to the real actor, never the singleton surface block. */
public final class BakingSurfaceProof {
    private static final String RUN = "jemNativeBakingSurfaceRunV1";
    private BakingSurfaceProof() {}
    public static void begin(ServerPlayer actor, BlockPos pos, BlockState state, InteractionHand hand) {
        actor.getPersistentData().remove(RUN);
        if (actor instanceof FakePlayer || actor.isSpectator() || actor.getAbilities().instabuild
                || !(actor.level().getBlockState(pos.below()).getBlock() instanceof BakerStationBlock)) return;
        CompoundTag run = new CompoundTag();
        run.putLong("pos", pos.asLong());
        run.putLong("time", actor.serverLevel().getGameTime());
        run.put("input", actor.getItemInHand(hand).save(new CompoundTag()));
        BakingStageReceipt.Stage stage = BakingStageReceipt.current(actor.level(), pos, state);
        if (stage != null && stage.actor().equals(actor.getUUID())) run.putInt("prepared", stage.prepared());
        actor.getPersistentData().put(RUN, run);
    }
    public static boolean commit(Level level, BlockPos pos, BlockState next, int flags, BlockState before,
                                  ServerPlayer actor, BlockPos nativePos, InteractionHand hand) {
        boolean changed = level.setBlock(pos, next, flags);
        CompoundTag run = actor.getPersistentData().getCompound(RUN);
        if (changed && pos.equals(nativePos) && run.getLong("pos") == pos.asLong()
                && run.getLong("time") == level.getGameTime() && run.contains("input") && level.getBlockState(pos).equals(next)) {
            ItemStack input = ItemStack.of(run.getCompound("input"));
            int prepared = run.getInt("prepared");
            if (before.getValue(BlankCakeBlock.CAKE) && input.is(TagsRegistry.KNIVES)
                    && next.equals(before.setValue(BlankCakeBlock.CAKE, false).setValue(BlankCakeBlock.CUPCAKE, true))) {
                BakingStageReceipt.prepared(actor, pos, next, 1);
            } else if (before.getValue(BlankCakeBlock.CUPCAKE) && prepared == 1 && input.is(ObjectRegistry.ROLLING_PIN.get())
                    && next.equals(before.setValue(BlankCakeBlock.CUPCAKE, false).setValue(BlankCakeBlock.COOKIE, true))) {
                BakingStageReceipt.prepared(actor, pos, next, 2);
            } else {
                String action = output(before, next.getBlock(), prepared);
                if (!action.isEmpty()) {
                    run.putString("action", action);
                    run.putString("output", next.toString());
                }
            }
        }
        return changed;
    }
    public static void finish(ServerPlayer actor, BlockPos pos, InteractionHand hand, boolean nativeSuccess) {
        CompoundTag run = actor.getPersistentData().getCompound(RUN).copy();
        actor.getPersistentData().remove(RUN);
        String action = run.getString("action");
        if (!nativeSuccess || action.isEmpty() || run.getLong("pos") != pos.asLong()
                || run.getLong("time") != actor.serverLevel().getGameTime()
                || !run.getString("output").equals(actor.level().getBlockState(pos).toString())) return;
        ItemStack remaining = ItemStack.of(run.getCompound("input"));
        if (remaining.isEmpty()) return;
        remaining.shrink(1);
        if (ItemStack.matches(remaining, actor.getItemInHand(hand))) QuestActionTrigger.INSTANCE.emit(actor, action);
    }
    private static String output(BlockState before, Block result, int prepared) {
        if (before.getValue(BlankCakeBlock.CAKE) && (result == ObjectRegistry.STRAWBERRY_CAKE.get()
                || result == ObjectRegistry.CHOCOLATE_CAKE.get() || result == ObjectRegistry.SWEETBERRY_CAKE.get()
                || result == ObjectRegistry.CHOCOLATE_GATEAU.get())) return "food_surface_cake";
        if (before.getValue(BlankCakeBlock.CUPCAKE) && prepared == 1 && (result == ObjectRegistry.STRAWBERRY_CUPCAKE_BLOCK.get()
                || result == ObjectRegistry.APPLE_CUPCAKE_BLOCK.get() || result == ObjectRegistry.SWEETBERRY_CUPCAKE_BLOCK.get())) return "food_surface_cupcake";
        if (before.getValue(BlankCakeBlock.COOKIE) && prepared == 2 && (result == ObjectRegistry.STRAWBERRY_COOKIE_BLOCK.get()
                || result == ObjectRegistry.CHOCOLATE_COOKIE_BLOCK.get() || result == ObjectRegistry.SWEETBERRY_COOKIE_BLOCK.get())) return "food_surface_cookie";
        return "";
    }
}
