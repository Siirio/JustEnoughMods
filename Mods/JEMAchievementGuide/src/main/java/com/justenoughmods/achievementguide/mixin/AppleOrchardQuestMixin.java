package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.food.OrchardFoodProof;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.satisfy.vinery.core.block.AppleLeavesBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = AppleLeavesBlock.class, remap = false)
public abstract class AppleOrchardQuestMixin {
    @Inject(method = "m_6227_", at = @At("HEAD"))
    private void jem$begin(BlockState state, Level level, BlockPos pos, Player actor, InteractionHand hand,
                            BlockHitResult hit, CallbackInfoReturnable<InteractionResult> callback) {
        if (actor instanceof ServerPlayer player) OrchardFoodProof.begin(player, pos, actor.getItemInHand(hand).isEmpty());
    }
    @Redirect(method = "m_6227_", at = @At(value = "INVOKE", target = "Lnet/satisfy/vinery/core/block/AppleLeavesBlock;m_152435_(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;Lnet/minecraft/world/item/ItemStack;)V"))
    private void jem$nativeFruit(Level level, BlockPos pos, Direction direction, ItemStack fruit, BlockState state,
                                 Level nativeLevel, BlockPos nativePos, Player actor, InteractionHand hand, BlockHitResult hit) {
        Block.popResourceFromFace(level, pos, direction, OrchardFoodProof.outgoing(actor, pos, state, fruit));
    }
    @Inject(method = "m_6227_", at = @At("RETURN"))
    private void jem$committed(BlockState state, Level level, BlockPos pos, Player actor, InteractionHand hand,
                                BlockHitResult hit, CallbackInfoReturnable<InteractionResult> callback) {
        if (actor instanceof ServerPlayer player) OrchardFoodProof.finish(player, pos, state, level.getBlockState(pos), callback.getReturnValue() == InteractionResult.SUCCESS);
    }
}
