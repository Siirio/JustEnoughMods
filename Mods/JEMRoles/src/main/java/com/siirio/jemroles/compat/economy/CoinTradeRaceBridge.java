package com.siirio.jemroles.compat.economy;

import com.siirio.jemeconomy.api.CoinTradeContext;
import com.siirio.jemeconomy.api.EconomyApi;
import com.siirio.jemeconomy.api.CoinTradePriceEvent;
import com.siirio.jemroles.api.RaceApi;
import com.siirio.jemroles.race.RaceId;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Isolated optional Economy bridge; Roles remains the discount owner. */
public final class CoinTradeRaceBridge {
    private CoinTradeRaceBridge() {}
    public static boolean coinPaid(ServerPlayer player) { return EconomyApi.isEnabled(player.server) && CoinTradeContext.active(player); }
    @SubscribeEvent public static void quote(CoinTradePriceEvent event) {
        var player = event.player();
        if (EconomyApi.isEnabled(player.server) && RaceApi.hasRace(player, RaceId.ANCESTORS)) {
            // Keep the full offer rational until final settlement rounding.
            event.discountPercent(20);
        }
    }
}
