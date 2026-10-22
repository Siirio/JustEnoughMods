package com.siirio.jemeconomy.physical;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public final class PhysicalCoinItem extends Item {
    private final CoinDenomination denomination;

    public PhysicalCoinItem(CoinDenomination denomination) {
        super(new Item.Properties().stacksTo(64));
        this.denomination = denomination;
    }

    public CoinDenomination denomination() {
        return denomination;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("jem.economy.coin.value", denomination.value()).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("jem.economy.coin.backed").withStyle(ChatFormatting.DARK_GRAY));
    }
}
