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
                || !CurrencyAuthority.active(player) || event.getHand() != net.minecraft.world.InteractionHand.MAIN_HAND) return;
        // Let the NPC open its real MerchantMenu and preserve its native interaction lifecycle.
        if (com.siirio.jemcurrency.NativeMerchantTags.isBuyer(merchant) && merchant.getTradingPlayer() == null)
            BuyerTradeOffers.refresh(merchant, player);
        else if (!com.siirio.jemcurrency.NativeMerchantTags.isBuyer(merchant) && merchant.getTradingPlayer() == null)
            ProfessionTradePools.extend(merchant);
    }
}
