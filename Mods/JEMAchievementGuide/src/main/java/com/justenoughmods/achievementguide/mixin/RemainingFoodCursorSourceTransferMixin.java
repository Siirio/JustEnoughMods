package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.food.NativeFoodSources;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerMenu.class)
public abstract class RemainingFoodCursorSourceTransferMixin {
    @Unique private ItemStack jem$taken = ItemStack.EMPTY;
    @Inject(method = "doClick", at = @At("HEAD"))
    private void jem$begin(int slot, int button, ClickType type, Player player, CallbackInfo callback) { jem$taken = ItemStack.EMPTY; }
    @Inject(method = "doClick", at = @At("RETURN"))
    private void jem$end(int slot, int button, ClickType type, Player player, CallbackInfo callback) { jem$taken = ItemStack.EMPTY; }
    @Redirect(method = "doClick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/Slot;safeTake(IILnet/minecraft/world/entity/player/Player;)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack jem$actualTake(Slot slot, int minimum, int maximum, Player player) {
        jem$taken = slot.safeTake(minimum, maximum, player); return jem$taken;
    }
    @Redirect(method = "doClick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;grow(I)V"))
    private void jem$nativeCursorMerge(ItemStack carried, int count) {
        carried.grow(count);
        if (count == jem$taken.getCount() && carried == ((AbstractContainerMenu)(Object)this).getCarried()
                && ItemStack.isSameItemSameTags(carried, jem$taken)) NativeFoodSources.transfer(carried, NativeFoodSources.claims(jem$taken), count, count);
        jem$taken = ItemStack.EMPTY;
    }
    @Redirect(method = "lambda$doClick$4(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/inventory/Slot;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;grow(I)V"))
    private static void jem$nativePickupMerge(ItemStack target, int count, ItemStack carried, Slot slot, Player player, ItemStack removed) {
        target.grow(count);
        if (target == carried && count == removed.getCount() && ItemStack.isSameItemSameTags(target, removed)) {
            NativeFoodSources.transfer(target, NativeFoodSources.claims(removed), count, count);
        }
    }
}
