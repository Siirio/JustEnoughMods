package com.justenoughmods.achievementguide.mixin;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.trading.Merchant;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(MerchantMenu.class)
public interface PetUtilityQuestMerchantAccess {@Accessor("trader")Merchant jem$petMerchant();@Accessor("tradeContainer")MerchantContainer jem$petTrades();}
