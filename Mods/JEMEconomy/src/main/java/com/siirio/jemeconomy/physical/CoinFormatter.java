package com.siirio.jemeconomy.physical;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public final class CoinFormatter {
    private CoinFormatter() { }
    public static Component format(long copper) {
        if (copper <= 0L) return Component.translatable("jem.economy.amount.none");
        List<MutableComponent> parts = new ArrayList<>();
        long remainder = copper;
        for (CoinDenomination denomination : CoinDenomination.descending()) {
            long count = remainder / denomination.value();
            remainder %= denomination.value();
            if (count > 0L) parts.add(Component.translatable("jem.economy.amount." + denomination.id(), count));
        }
        MutableComponent result = Component.empty();
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) result.append(Component.literal(" + "));
            result.append(parts.get(i));
        }
        return result;
    }
}
