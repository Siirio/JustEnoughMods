package com.siirio.jemeconomy.merchant;

import com.siirio.jemidentity.api.DedicatedSmp;
import com.siirio.jemidentity.api.IdentityPolicy;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.NetworkHooks;

public final class CoinMerchantRuntime {
    public static final int SCHEMA = 1;
    private CoinMerchantRuntime() { }

    @SubscribeEvent
    public static void interact(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(event.getTarget() instanceof AbstractVillager merchant)
                || com.siirio.jemeconomy.market.MarketRuntime.isBuyer(merchant)
                || !DedicatedSmp.active(player) || event.getHand() != net.minecraft.world.InteractionHand.MAIN_HAND) return;
        event.setCanceled(true); event.setCancellationResult(net.minecraft.world.InteractionResult.CONSUME);
        if (IdentityPolicy.resolve(player).isEmpty()) { player.displayClientMessage(Component.translatable("jem.economy.shipping.identity_required"), true); return; }
        merchant.getPersistentData().putInt("JemCoinTradeSchema", SCHEMA);
        NetworkHooks.openScreen(player, new CoinMerchantProvider(merchant), buffer -> buffer.writeVarInt(merchant.getId()));
    }
}
