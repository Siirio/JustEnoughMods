package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.food.NativeFoodSources;
import com.justenoughmods.achievementguide.integration.food.NativePreparedMealProof;
import com.justenoughmods.achievementguide.integration.food.FoodTransferSnapshots.MenuDestination;
import com.justenoughmods.achievementguide.integration.food.FoodTransferSnapshots.MenuTransfer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerMenu.class)
public abstract class RemainingFoodMenuSourceTransferMixin {
    @Unique private MenuTransfer jem$transfer;
    @Inject(method = "moveItemStackTo", at = @At("HEAD"))
    private void jem$begin(ItemStack source, int start, int end, boolean reverse, CallbackInfoReturnable<Boolean> callback) {
        NativePreparedMealProof.moving((AbstractContainerMenu)(Object)this, source);
        jem$transfer = null;
        var claims = NativeFoodSources.claims(source); var menu = (AbstractContainerMenu)(Object)this;
        if (claims.isEmpty() || start < 0 || end < start || end > menu.slots.size() || end - start > 256) return;
        List<MenuDestination> before = new ArrayList<>();
        for (int i = start; i < end; i++) {
            ItemStack target = menu.slots.get(i).getItem();
            if (target != source) before.add(new MenuDestination(i, target.copy(), NativeFoodSources.claims(target)));
        }
        jem$transfer = new MenuTransfer(source, source.copy(), claims, before);
    }
    @Inject(method = "moveItemStackTo", at = @At("RETURN"))
    private void jem$end(ItemStack source, int start, int end, boolean reverse, CallbackInfoReturnable<Boolean> callback) {
        MenuTransfer run = jem$transfer; jem$transfer = null;
        if (run == null || run.source() != source || !callback.getReturnValue()) return;
        int moved = run.before().getCount() - source.getCount(); var menu = (AbstractContainerMenu)(Object)this;
        int gained = 0;
        for (MenuDestination target : run.destinations()) {
            ItemStack current = menu.slots.get(target.slot()).getItem();
            if (ItemStack.isSameItemSameTags(current, run.before())) gained += Math.max(0, current.getCount() - target.before().getCount());
        }
        if (moved < 1 || gained != moved) return;
        List<NativeFoodSources.Claim> remaining = run.claims(); int remainingCount = run.before().getCount();
        List<MenuDestination> nativeOrder = new ArrayList<>(run.destinations());
        if (reverse) java.util.Collections.reverse(nativeOrder);
        // Vanilla merges existing slots before placing into empty slots, in its selected direction
        nativeOrder.sort(java.util.Comparator.comparing(target -> target.before().isEmpty()));
        for (MenuDestination target : nativeOrder) {
            ItemStack current = menu.slots.get(target.slot()).getItem();
            if (!ItemStack.isSameItemSameTags(current, run.before())) continue;
            int amount = Math.max(0, current.getCount() - target.before().getCount());
            if (amount < 1) continue;
            NativeFoodSources.claims(current, target.claims());
            NativeFoodSources.transfer(current, remaining, remainingCount, amount);
            remaining = NativeFoodSources.remainder(remaining, NativeFoodSources.portion(remaining, remainingCount, amount));
            remainingCount -= amount;
        }
        NativeFoodSources.claims(source, remaining);
    }
}
