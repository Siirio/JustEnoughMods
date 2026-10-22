package com.siirio.jemsettlements.compat.economy;

import com.siirio.jemsettlements.client.SettlementPriceRenderer.Unit;
import java.util.List;
import net.minecraft.network.chat.Component;

public final class CoinPresentationAccess {
    private CoinPresentationAccess() { }
    public static List<Unit> units() {
        return java.util.Arrays.stream(com.siirio.jemeconomy.physical.CoinDenomination.values())
                .map(coin -> new Unit(coin.id(), coin.value())).toList();
    }
    public static Component formatAll(long amount) { return com.siirio.jemeconomy.physical.CoinFormatter.formatAll(amount); }
}
