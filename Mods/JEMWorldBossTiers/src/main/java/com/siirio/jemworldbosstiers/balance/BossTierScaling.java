package com.siirio.jemworldbosstiers.balance;

public final class BossTierScaling {
    private static final double PER_TIER = 0.15;
    private BossTierScaling() {}
    public static double boost(int tier) { return 1 + PER_TIER * Math.max(1, Math.min(TierValues.TIER_COUNT, tier)); }
}
