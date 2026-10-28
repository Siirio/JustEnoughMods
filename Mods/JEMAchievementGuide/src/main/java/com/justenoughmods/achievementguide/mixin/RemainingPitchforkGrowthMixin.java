package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import com.justenoughmods.achievementguide.integration.food.RemainingFoodActors;
import com.justenoughmods.achievementguide.integration.food.FoodTransferSnapshots.PitchforkRun;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.satisfy.farm_and_charm.core.block.FertilizedSoilBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = FertilizedSoilBlock.class, remap = false)
public abstract class RemainingPitchforkGrowthMixin {
    @Unique private static final ThreadLocal<PitchforkRun> jem$nativeIntervention = new ThreadLocal<>();
    @Inject(method = "m_6227_", at = @At("HEAD"))
    private void jem$begin(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> callback) {
        jem$nativeIntervention.remove();
        ServerPlayer actor = RemainingFoodActors.real(player);
        if (actor != null && RemainingFoodActors.item(actor.getItemInHand(hand)).equals("farm_and_charm:pitchfork")) jem$nativeIntervention.set(new PitchforkRun(actor));
    }
    @Redirect(method = "lambda$applyBoneMealEffect$2", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/BonemealableBlock;m_214148_(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/util/RandomSource;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V"))
    private static void jem$selectedCrop(BonemealableBlock target, ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        target.performBonemeal(level, random, pos, state);
        PitchforkRun run = jem$nativeIntervention.get();
        BlockState after = level.getBlockState(pos);
        if (run != null && run.actor().level() == level && state.getBlock() instanceof CropBlock crop && after.is(state.getBlock())
                && crop.getAge(after) > crop.getAge(state)) run.growth();
    }
    @Inject(method = "m_6227_", at = @At("RETURN"))
    private void jem$end(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> callback) {
        PitchforkRun run = jem$nativeIntervention.get(); jem$nativeIntervention.remove();
        BlockState after = level.getBlockState(pos);
        if (run != null && run.actor() == player && run.grew() && callback.getReturnValue().consumesAction()
                && (!after.is(state.getBlock()) || after.getValue(FertilizedSoilBlock.SIZE) == state.getValue(FertilizedSoilBlock.SIZE) - 1)) {
            QuestActionTrigger.INSTANCE.emit(run.actor(), "v3_food_living_soil");
        }
    }
}
