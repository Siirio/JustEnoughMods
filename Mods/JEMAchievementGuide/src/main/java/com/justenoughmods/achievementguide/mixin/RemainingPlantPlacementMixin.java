package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.food.NativeFoodSources;
import com.justenoughmods.achievementguide.integration.food.NativePlantProof;
import com.justenoughmods.achievementguide.integration.food.RemainingFoodActors;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public abstract class RemainingPlantPlacementMixin {
    @Unique private final ThreadLocal<ItemStack> jem$source = new ThreadLocal<>();
    @Inject(method = "place", at = @At("HEAD"))
    private void jem$before(BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> callback) {
        jem$source.remove();
        if (RemainingFoodActors.real(context.getPlayer()) != null) jem$source.set(context.getItemInHand().copy());
    }
    @Inject(method = "place", at = @At("RETURN"))
    private void jem$after(BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> callback) {
        ItemStack before = jem$source.get(); jem$source.remove();
        var actor = RemainingFoodActors.real(context.getPlayer());
        if (actor != null && before != null && callback.getReturnValue().consumesAction()
                && before.getCount() - context.getItemInHand().getCount() == 1) {
            var state = actor.level().getBlockState(context.getClickedPos());
            if (state.is(((BlockItem)(Object)this).getBlock())) NativePlantProof.placed(actor, context.getClickedPos(), state, before);
        }
    }
}
