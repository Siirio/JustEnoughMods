package com.siirio.jemeconomy.mixin;

import com.siirio.jemeconomy.merchant.NativeTradeContainer;
import com.siirio.jemeconomy.merchant.NativeTradeSession;
import net.minecraft.core.NonNullList;
import net.minecraft.world.inventory.MerchantContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MerchantContainer.class)
public abstract class MerchantContainerMixin implements NativeTradeContainer {
    @Shadow @Final private NonNullList<ItemStack> itemStacks;
    @Shadow private MerchantOffer activeOffer;
    @Shadow private int futureXp;
    @Unique private NativeTradeSession jem$session;
    @Override public void jem$bind(NativeTradeSession session) { jem$session = session; }
    @Override public void jem$projection(MerchantOffer offer, ItemStack result, int xp) {
        activeOffer = offer; itemStacks.set(2, result); futureXp = xp;
    }
    @Override public void jem$input(int slot, ItemStack stack) { itemStacks.set(slot, stack); }
    @Inject(method = "updateSellItem", at = @At("HEAD"), cancellable = true)
    private void jem$quote(CallbackInfo callback) {
        if (jem$session != null && jem$session.updateResult()) callback.cancel();
    }
}
