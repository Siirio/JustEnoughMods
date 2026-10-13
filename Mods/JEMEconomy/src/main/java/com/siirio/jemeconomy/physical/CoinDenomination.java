package com.siirio.jemeconomy.physical;

import java.util.Arrays;
import java.util.Optional;

public enum CoinDenomination {
    COPPER("copper_coin", 1L),
    SILVER("silver_coin", 10L),
    GOLD("gold_coin", 100L),
    AETHER("aether_coin", 1_000L),
    ARCANE("arcane_coin", 10_000L);

    private final String id;
    private final long value;

    CoinDenomination(String id, long value) {
        this.id = id;
        this.value = value;
    }

    public String id() {
        return id;
    }

    public long value() {
        return value;
    }

    public static CoinDenomination[] descending() {
        return new CoinDenomination[]{ARCANE, AETHER, GOLD, SILVER, COPPER};
    }

    public static Optional<CoinDenomination> byId(String id) {
        return Arrays.stream(values()).filter(value -> value.id.equals(id)).findFirst();
    }
}
