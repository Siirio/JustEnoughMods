package com.siirio.jemeconomy.client;

import com.siirio.jemeconomy.physical.CoinDenomination;
import java.util.ArrayList;
import java.util.List;

/** Exact denomination projection shared by monetary renderers. No game/client dependencies. */
public final class CoinAmounts {
    private CoinAmounts() { }
    public record Part(CoinDenomination denomination, long count) { }
    public static List<Part> parts(long copper) {
        if (copper < 0) throw new IllegalArgumentException("Negative monetary display");
        List<Part> result = new ArrayList<>();
        for (var denomination : CoinDenomination.descending()) {
            long count = copper / denomination.value(); copper %= denomination.value();
            if (count > 0) result.add(new Part(denomination, count));
        }
        return List.copyOf(result);
    }
}
