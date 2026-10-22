package com.siirio.jemeconomy.merchant;

import net.minecraft.world.item.trading.MerchantOffers;

/** NPC-owned JEM extensions are separate from the original native offers. */
public interface NativeMerchantOffersAccess {
    MerchantOffers jem$extensions();
    boolean jem$legacyMixedOffers();
    void jem$generatingOffers(boolean generating);
}
