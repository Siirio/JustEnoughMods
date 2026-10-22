package com.siirio.jemeconomy.mixin;

import com.siirio.jemeconomy.merchant.NativeMerchantOffersAccess;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.WanderingTrader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Native initial/level-up generation appends to the real list, never a presentation projection. */
@Mixin({Villager.class, WanderingTrader.class})
public abstract class NativeOfferGenerationMixin {
    @Inject(method = "updateTrades", at = @At("HEAD"))
    private void jem$beginGeneration(CallbackInfo callback) {
        ((NativeMerchantOffersAccess) this).jem$generatingOffers(true);
    }
    @Inject(method = "updateTrades", at = @At("RETURN"))
    private void jem$endGeneration(CallbackInfo callback) {
        ((NativeMerchantOffersAccess) this).jem$generatingOffers(false);
    }
}
