package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.food.BakingSurfaceProof;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.satisfy.bakery.core.block.cake.BlankCakeBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = BlankCakeBlock.class, remap = false)
public abstract class BakingSurfaceQuestMixin {
    @Inject(method = "m_6227_", at = @At("HEAD"))
    private void jem$begin(BlockState state, Level level, BlockPos pos, Player actor, InteractionHand hand,
                            BlockHitResult hit, CallbackInfoReturnable<InteractionResult> callback) {
        if (actor instanceof ServerPlayer player) BakingSurfaceProof.begin(player, pos, state, hand);
    }
    @Redirect(method = "m_6227_", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;m_7731_(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    private boolean jem$nativeOutput(Level level, BlockPos pos, BlockState next, int flags, BlockState before,
                                      Level nativeLevel, BlockPos nativePos, Player actor, InteractionHand hand, BlockHitResult hit) {
        return actor instanceof ServerPlayer player
                ? BakingSurfaceProof.commit(level, pos, next, flags, before, player, nativePos, hand)
                : level.setBlock(pos, next, flags);
    }
    @Inject(method = "m_6227_", at = @At("RETURN"))
    private void jem$finished(BlockState state, Level level, BlockPos pos, Player actor, InteractionHand hand,
                               BlockHitResult hit, CallbackInfoReturnable<InteractionResult> callback) {
        if (actor instanceof ServerPlayer player) BakingSurfaceProof.finish(player, pos, hand, callback.getReturnValue().consumesAction());
    }
}
