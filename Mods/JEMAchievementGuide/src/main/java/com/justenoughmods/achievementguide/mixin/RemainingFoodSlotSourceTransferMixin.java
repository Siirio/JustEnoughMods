package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.food.NativeFoodSources;
import com.justenoughmods.achievementguide.integration.food.FoodTransferSnapshots.SlotTransfer;
import java.util.List;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Slot.class)
public abstract class RemainingFoodSlotSourceTransferMixin {
    @Unique private SlotTransfer jem$transfer;
    @Inject(method = "safeInsert(Lnet/minecraft/world/item/ItemStack;I)Lnet/minecraft/world/item/ItemStack;", at = @At("HEAD"))
    private void jem$before(ItemStack source, int maximum, CallbackInfoReturnable<ItemStack> callback) {
        jem$transfer = null;
        if (!NativeFoodSources.claims(source).isEmpty()) {
            ItemStack to = ((Slot)(Object)this).getItem();
            jem$transfer = new SlotTransfer(source, source.getCount(), to.getCount(), NativeFoodSources.claims(source), NativeFoodSources.claims(to));
        }
    }
    @Inject(method = "safeInsert(Lnet/minecraft/world/item/ItemStack;I)Lnet/minecraft/world/item/ItemStack;", at = @At("RETURN"))
    private void jem$after(ItemStack source, int maximum, CallbackInfoReturnable<ItemStack> callback) {
        SlotTransfer run = jem$transfer; jem$transfer = null;
        if (run == null || run.source() != source) return;
        ItemStack target = ((Slot)(Object)this).getItem(); int moved = run.count() - callback.getReturnValue().getCount();
        if (moved > 0 && target.getCount() - run.destinationCount() == moved) {
            NativeFoodSources.claims(target, run.to());
            NativeFoodSources.transfer(target, run.from(), run.count(), moved);
        }
    }
}
