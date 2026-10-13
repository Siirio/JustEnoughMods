package com.siirio.jemworldbosstiers.compat.irons;

public final class SpellTierFactors {
    private static final float[] FACTORS = {0.65F, 0.80F, 0.95F, 1.10F, 1.20F};

    private SpellTierFactors() {
    }

    public static float at(int worldTier) {
        return FACTORS[Math.max(1, Math.min(FACTORS.length, worldTier)) - 1];
    }
}
