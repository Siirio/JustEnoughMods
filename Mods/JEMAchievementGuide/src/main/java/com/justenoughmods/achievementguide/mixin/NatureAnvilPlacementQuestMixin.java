package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.nature.NatureActors;
import com.justenoughmods.achievementguide.integration.nature.NatureFallingProof;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public abstract class NatureAnvilPlacementQuestMixin {
    @Shadow public abstract Block getBlock();
    @Unique private int jem$anvilBefore;

    @Inject(method = "place", at = @At("HEAD"))
    private void jem$beforeAnvil(BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> callback) {
        jem$anvilBefore = context.getItemInHand().getCount();
    }

    @Inject(method = "place", at = @At("RETURN"))
    private void jem$placedAnvil(BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> callback) {
        if (!callback.getReturnValue().consumesAction() || !(context.getPlayer() instanceof ServerPlayer actor)
                || !NatureActors.eligible(actor) || jem$anvilBefore - context.getItemInHand().getCount() != 1) return;
        var state = context.getLevel().getBlockState(context.getClickedPos());
        if (state.getBlock() == getBlock() && state.is(BlockTags.ANVIL)) {
            NatureFallingProof.attributed(actor, context.getClickedPos(), state, "Anvil");
        }
    }
}
