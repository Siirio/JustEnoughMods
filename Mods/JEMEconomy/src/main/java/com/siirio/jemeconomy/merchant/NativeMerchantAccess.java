package com.siirio.jemeconomy.merchant;

import net.minecraft.world.inventory.MerchantContainer;

public interface NativeMerchantAccess {
    NativeTradeSession jem$session();
    MerchantContainer jem$container();
}
