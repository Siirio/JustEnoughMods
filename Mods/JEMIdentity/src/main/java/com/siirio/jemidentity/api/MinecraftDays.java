package com.siirio.jemidentity.api;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Shared presentation only; saved and network times retain native units. */
public final class MinecraftDays {
    private MinecraftDays() {}

    public static String format(long remainingTicks) {
        if (remainingTicks <= 0L) return "0";
        if (remainingTicks < 24L) return "<0.001";
        return BigDecimal.valueOf(remainingTicks).divide(BigDecimal.valueOf(24_000L), 3, RoundingMode.CEILING)
                .stripTrailingZeros().toPlainString();
    }
}
