package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.food.BreweryReceipt;
import com.justenoughmods.achievementguide.integration.food.FoodOutputProof;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.satisfy.brewery.core.block.BrewKettleBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Preserves native mug consumption and inventory insertion, crediting only a portion actually received. */
@Mixin(value = BrewKettleBlock.class, remap = false)
public abstract class BreweryCollectionQuestMixin {
    @Inject(method = "m_6227_", at = @At("HEAD"))
    private void jem$beginUse(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                              BlockHitResult hit, CallbackInfoReturnable<InteractionResult> callback) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof BreweryReceipt receipt) receipt.jem$clearDraw();
    }
    @Redirect(method = "m_6227_", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;m_36356_(Lnet/minecraft/world/item/ItemStack;)Z"))
    private boolean jem$realReceipt(Player player, ItemStack stack, BlockState state, Level level, BlockPos pos,
                                   Player actor, InteractionHand hand, BlockHitResult hit) {
        ItemStack original = stack.copy();
        int before = player instanceof ServerPlayer serverPlayer ? FoodOutputProof.held(serverPlayer, serverPlayer.containerMenu, original) : 0;
        boolean inserted = player.addItem(stack);
        if (player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof BreweryReceipt receipt) {
            boolean received = inserted && FoodOutputProof.held(serverPlayer, serverPlayer.containerMenu, original) - before >= original.getCount();
            receipt.jem$received(serverPlayer, original, received);
        }
        return inserted;
    }
}
