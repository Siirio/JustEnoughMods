package com.justenoughmods.achievementguide.mixin;

import com.github.ysbbbbbb.kaleidoscopecookery.block.drink.EmptyCupBlock;
import com.github.ysbbbbbb.kaleidoscopecookery.block.drink.TeacupBlock;
import com.justenoughmods.achievementguide.integration.food.TeapotServingProof;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = {EmptyCupBlock.class, TeacupBlock.class}, remap = false)
public abstract class CookeryTeacupFoodQuestMixin {
    @Inject(method = "m_6227_", at = @At("HEAD"))
    private void jem$before(BlockState state, Level level, BlockPos pos, Player actor, InteractionHand hand,
                            BlockHitResult hit, CallbackInfoReturnable<InteractionResult> callback) {
        if (actor instanceof ServerPlayer player) TeapotServingProof.begin(actor.getItemInHand(hand), player);
    }

    @Inject(method = "m_6227_", at = @At("RETURN"))
    private void jem$after(BlockState state, Level level, BlockPos pos, Player actor, InteractionHand hand,
                           BlockHitResult hit, CallbackInfoReturnable<InteractionResult> callback) {
        if (actor instanceof ServerPlayer player)
            TeapotServingProof.finish(actor.getItemInHand(hand), player, state, level.getBlockState(pos),
                    callback.getReturnValue() == InteractionResult.SUCCESS);
    }
}
