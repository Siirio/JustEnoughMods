package com.siirio.jemeconomy.merchant;

import com.siirio.jemcurrency.CurrencyAuthority;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class CoinMerchantRuntime {
    private CoinMerchantRuntime() { }

    @SubscribeEvent
    public static void interact(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(event.getTarget() instanceof AbstractVillager merchant)
                || event.getHand() != net.minecraft.world.InteractionHand.MAIN_HAND) return;
        if (!CurrencyAuthority.merchantActive(player)) {
            merchant.getOffers();
            if (((NativeMerchantOffersAccess) merchant).jem$legacyMixedOffers()) {
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable("jem.economy.trade.disabled_legacy"), true);
                event.setCancellationResult(net.minecraft.world.InteractionResult.CONSUME);
                event.setCanceled(true);
            }
            return;
        }
        // Let the NPC open its real MerchantMenu and preserve its native interaction lifecycle.
        if (merchant.getTradingPlayer() == null) ProfessionTradePools.extend(merchant);
    }
}
