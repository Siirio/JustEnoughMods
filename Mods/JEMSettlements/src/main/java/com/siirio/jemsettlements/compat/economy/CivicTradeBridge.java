package com.siirio.jemsettlements.compat.economy;

import com.siirio.jemeconomy.sandbox.NativeTradePaymentEvent;
import com.siirio.jemsettlements.sandbox.SandboxCivicRuntime;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Optional event signature must never be reflected when Economy is absent. */
public final class CivicTradeBridge {
    private CivicTradeBridge() { }
    @SubscribeEvent public static void payment(NativeTradePaymentEvent event) {
        SandboxCivicRuntime.payment(event.player, event.merchant, event::town);
    }
}
