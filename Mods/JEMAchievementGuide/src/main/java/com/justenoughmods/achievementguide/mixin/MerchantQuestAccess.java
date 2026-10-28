package com.justenoughmods.achievementguide.mixin;

import net.minecraft.world.inventory.MerchantContainer;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.trading.Merchant;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MerchantMenu.class)
public interface MerchantQuestAccess {
    @Accessor("trader") Merchant jem$nativeMerchant();
    @Accessor("tradeContainer") MerchantContainer jem$nativeTradeContainer();
}
