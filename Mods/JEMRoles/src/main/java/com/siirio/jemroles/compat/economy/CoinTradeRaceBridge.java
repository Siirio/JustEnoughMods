package com.siirio.jemroles.compat.economy;

import com.siirio.jemeconomy.api.CoinTradeContext;
import com.siirio.jemeconomy.api.CoinTradePriceEvent;
import com.siirio.jemroles.api.RaceApi;
import com.siirio.jemroles.race.RaceId;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Isolated optional Economy bridge; Roles remains the discount owner. */
public final class CoinTradeRaceBridge {
    private CoinTradeRaceBridge() {}
    public static boolean coinPaid(ServerPlayer player) { return CoinTradeContext.active(player); }
    @SubscribeEvent public static void quote(CoinTradePriceEvent event) {
        var player = event.player();
        if (RaceApi.effectiveRace(player.getServer(), player.getUUID()) == RaceId.HUMAN) {
            long amount = event.copper();
            // ceil(90%) without floating-point loss or multiplication overflow.
            event.setCopper(Math.max(1L, amount - amount / 10L));
        }
    }
}
