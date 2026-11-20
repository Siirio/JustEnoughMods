package com.siirio.jemworldbosstiers.balance;

public final class BossTierScaling {
    public static final double HEALTH_SCALE = 0.80D;
    public static final float DAMAGE_SCALE = 0.50F;
    private static final double PER_TIER = 0.15;
    private BossTierScaling() {}
    public static double boost(int tier) { return 1 + PER_TIER * Math.max(1, Math.min(TierValues.TIER_COUNT, tier)); }
}
