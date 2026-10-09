package com.siirio.jemworldbosstiers.balance;

import java.util.List;

public record TierValues(List<Double> values) {
    public static final int TIER_COUNT = 5;

    public TierValues(double tierOne, double tierTwo, double tierThree, double tierFour, double tierFive) {
        this(List.of(tierOne, tierTwo, tierThree, tierFour, tierFive));
    }

    public TierValues {
        values = List.copyOf(values);
        if (values.size() != TIER_COUNT || values.stream().anyMatch(value -> value == null || !Double.isFinite(value))) {
            throw new IllegalArgumentException("Exactly five finite tier values are required");
        }
    }

    public double valueAt(int tier) {
        if (tier < 1 || tier > TIER_COUNT) {
            throw new IllegalArgumentException("Tier must be between one and five");
        }
        return values.get(tier - 1);
    }
}
