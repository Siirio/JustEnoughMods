package com.siirio.jemeconomy.merchant;

import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

final class CoinMerchantProvider implements MenuProvider {
    private final AbstractVillager merchant;
    CoinMerchantProvider(AbstractVillager merchant) { this.merchant = merchant; }
    @Override public Component getDisplayName() { return Component.translatable("container.jem_economy.coin_merchant", merchant.getDisplayName()); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) { return new CoinMerchantMenu(id, inventory, merchant); }
}
