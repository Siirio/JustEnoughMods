package com.siirio.jemeconomy.physical;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public final class CoinFormatter {
    private CoinFormatter() { }
    public static Component format(long copper) {
        if (copper <= 0L) return Component.literal("0");
        List<MutableComponent> parts = new ArrayList<>();
        long remainder = copper;
        for (CoinDenomination denomination : CoinDenomination.descending()) {
            long count = remainder / denomination.value();
            remainder %= denomination.value();
            if (count > 0L) parts.add(iconAmount(denomination, count));
        }
        MutableComponent result = Component.empty();
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) result.append(Component.literal(" + "));
            result.append(parts.get(i));
        }
        return result;
    }
    public static Component formatAll(long copper) {
        return format(copper);
    }
    /** Uses the registered coin faces, also in chat/actionbar and native Component tooltips. */
    public static MutableComponent iconAmount(CoinDenomination denomination, long count) {
        return Component.literal(String.valueOf((char) (0xE100 + denomination.ordinal())))
                .withStyle(net.minecraft.network.chat.Style.EMPTY.withFont(new net.minecraft.resources.ResourceLocation("jem_economy", "coins")))
                .append(Component.literal(" " + count).withStyle(net.minecraft.network.chat.Style.EMPTY.withFont(new net.minecraft.resources.ResourceLocation("minecraft", "default"))));
    }
}
