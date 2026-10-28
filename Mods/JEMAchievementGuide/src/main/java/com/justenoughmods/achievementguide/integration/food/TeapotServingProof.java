package com.justenoughmods.achievementguide.integration.food;

import com.github.ysbbbbbb.kaleidoscopecookery.block.drink.EmptyCupBlock;
import com.github.ysbbbbbb.kaleidoscopecookery.block.drink.TeacupBlock;
import com.github.ysbbbbbb.kaleidoscopecookery.item.TeapotItem;
import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

/** No pot pickup credit: completed native recipe, real one-portion debit and real cup change. */
public final class TeapotServingProof {
    private static final String RUN = "jemNativeTeapotServingRunV1";
    private TeapotServingProof() {}

    public static void begin(ItemStack pot, ServerPlayer actor) {
        if (!(pot.getItem() instanceof TeapotItem)) return;
        if (pot.hasTag()) pot.getTag().remove(RUN);
        CompoundTag nativeData = BlockItem.getBlockEntityData(pot);
        if (nativeData == null) return;
        CompoundTag proof = nativeData.getCompound(CookeryFoodProof.KEY);
        ItemStack result = TeapotItem.getPourOut(pot);
        if (result.isEmpty() || proof.getString("recipe").isEmpty()
                || !result.save(new CompoundTag()).equals(proof.getCompound("result"))) return;
        CompoundTag run = proof.copy();
        run.putUUID("actor", actor.getUUID());
        pot.getOrCreateTag().put(RUN, run);
    }

    public static void finish(ItemStack pot, ServerPlayer actor, BlockState before, BlockState after, boolean nativeSuccess) {
        if (!(pot.getItem() instanceof TeapotItem) || !pot.hasTag()) return;
        CompoundTag run = pot.getTag().getCompound(RUN).copy();
        pot.getTag().remove(RUN);
        if (pot.getTag().isEmpty()) pot.setTag(null);
        if (!run.hasUUID("actor") || !run.getUUID("actor").equals(actor.getUUID())) return;
        ItemStack served = ItemStack.of(run.getCompound("result"));
        ItemStack remaining = TeapotItem.getPourOut(pot);
        ItemStack expectedRemaining = served.copy();
        expectedRemaining.shrink(1);
        if (!ItemStack.matches(expectedRemaining, remaining)) return;
        CompoundTag nativeData = BlockItem.getBlockEntityData(pot);
        if (nativeData != null) {
            if (remaining.isEmpty()) nativeData.remove(CookeryFoodProof.KEY);
            else {
                run.remove("actor");
                run.put("result", remaining.save(new CompoundTag()));
                nativeData.put(CookeryFoodProof.KEY, run);
            }
        }
        if (nativeSuccess && !before.equals(after) && expectedCup(before, served).equals(after))
            QuestActionTrigger.INSTANCE.emit(actor, "v3_food_little_teapot");
    }

    private static BlockState expectedCup(BlockState before, ItemStack served) {
        Block block = Block.byItem(served.getItem());
        if (!(block instanceof TeacupBlock cup)) return before;
        if (before.getBlock() instanceof EmptyCupBlock) {
            return cup.defaultBlockState()
                    .setValue(cup.getCupCountProperty(), Math.min(before.getValue(EmptyCupBlock.CUP_COUNT), cup.getMaxCount()))
                    .setValue(cup.getTeaCountProperty(), 1)
                    .setValue(HorizontalDirectionalBlock.FACING, before.getValue(HorizontalDirectionalBlock.FACING));
        }
        if (before.is(cup) && before.getValue(cup.getTeaCountProperty()) < before.getValue(cup.getCupCountProperty()))
            return before.setValue(cup.getTeaCountProperty(), before.getValue(cup.getTeaCountProperty()) + 1);
        return before;
    }
}
