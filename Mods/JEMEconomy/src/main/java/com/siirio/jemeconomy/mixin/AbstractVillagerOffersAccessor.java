package com.siirio.jemeconomy.mixin;

import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.item.trading.MerchantOffers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Native server offer owner. Merchant.overrideOffers is a client-merchant hook
 * and AbstractVillager deliberately implements it as a no-op in 1.20.1. */
@Mixin(AbstractVillager.class)
public interface AbstractVillagerOffersAccessor {
    @Accessor("offers")
    void jem$setOffers(MerchantOffers offers);
}
