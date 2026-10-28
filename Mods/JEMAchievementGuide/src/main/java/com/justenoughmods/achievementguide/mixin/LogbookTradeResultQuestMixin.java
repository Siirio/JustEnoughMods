package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.LogbookTradeProof;
import net.minecraft.world.inventory.MerchantResultSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(MerchantResultSlot.class)
public abstract class LogbookTradeResultQuestMixin {
    @Shadow @Final private Merchant merchant;
    @Redirect(method = "onTake", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/world/item/trading/MerchantOffer;take(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)Z"), require = 2)
    private boolean jem$nativeTradeDebit(MerchantOffer offer, ItemStack a, ItemStack b) {
        var beforeA = a.copy();
        var beforeB = b.copy();
        boolean accepted = offer.take(a, b);
        LogbookTradeProof.consumed(merchant, offer, a, b, beforeA, beforeB, accepted);
        return accepted;
    }
}
