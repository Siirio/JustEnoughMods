package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.food.NativeSoilProof;
import com.justenoughmods.achievementguide.integration.food.RemainingFoodActors;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public abstract class RemainingSprinklerPlacementMixin {
    @Unique private final ThreadLocal<BlockPlaceContext> jem$placed = new ThreadLocal<>();
    @Unique private final ThreadLocal<Integer> jem$count = new ThreadLocal<>();
    @Redirect(method = "place", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/BlockItem;updatePlacementContext(Lnet/minecraft/world/item/context/BlockPlaceContext;)Lnet/minecraft/world/item/context/BlockPlaceContext;"))
    private BlockPlaceContext jem$actualContext(BlockItem item, BlockPlaceContext requested) {
        jem$placed.remove(); jem$count.remove();
        BlockPlaceContext actual = item.updatePlacementContext(requested);
        if (actual != null && RemainingFoodActors.real(actual.getPlayer()) != null
                && String.valueOf(ForgeRegistries.BLOCKS.getKey(item.getBlock())).equals("farm_and_charm:water_sprinkler")) {
            jem$placed.set(actual); jem$count.set(actual.getItemInHand().getCount());
        }
        return actual;
    }
    @Inject(method = "place", at = @At("RETURN"))
    private void jem$committed(BlockPlaceContext requested, CallbackInfoReturnable<InteractionResult> callback) {
        BlockPlaceContext actual = jem$placed.get(); Integer count = jem$count.get();
        jem$placed.remove(); jem$count.remove();
        if (actual == null || count == null || !callback.getReturnValue().consumesAction()
                || count - actual.getItemInHand().getCount() != 1) return;
        var actor = RemainingFoodActors.real(actual.getPlayer());
        var owner = actual.getLevel().getBlockEntity(actual.getClickedPos());
        if (actor != null && owner != null && owner.getBlockState().is(((BlockItem)(Object)this).getBlock())) {
            owner.getPersistentData().putUUID(NativeSoilProof.PLACER, actor.getUUID()); owner.setChanged();
        }
    }
}
