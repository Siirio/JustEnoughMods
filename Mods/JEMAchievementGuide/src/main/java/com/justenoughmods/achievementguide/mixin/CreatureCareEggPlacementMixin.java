package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.creaturecare.CreatureCareEggs;
import com.justenoughmods.achievementguide.integration.creaturecare.CreatureCareProof;
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
public abstract class CreatureCareEggPlacementMixin {
    @Unique private final ThreadLocal<ItemStack> jem$egg = new ThreadLocal<>();
    @Unique private final ThreadLocal<Integer> jem$count = new ThreadLocal<>();
    @Inject(method = "place", at = @At("HEAD"))
    private void jem$before(BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> callback) {
        jem$egg.remove(); jem$count.remove();
        if (CreatureCareProof.real(context.getPlayer()) != null && CreatureCareProof.item(context.getItemInHand()).equals("primal:eagle_egg")) {
            jem$egg.set(context.getItemInHand().copy()); jem$count.set(CreatureCareEggs.count(context.getLevel().getBlockState(context.getClickedPos())));
        }
    }
    @Inject(method = "place", at = @At("RETURN"))
    private void jem$after(BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> callback) {
        ItemStack before = jem$egg.get(); Integer count = jem$count.get(); jem$egg.remove(); jem$count.remove();
        var actor = CreatureCareProof.real(context.getPlayer());
        if (actor != null && before != null && count != null && callback.getReturnValue().consumesAction()
                && CreatureCareProof.debited(before, context.getItemInHand())) CreatureCareEggs.placed(actor, context.getClickedPos(), before, count);
    }
}
