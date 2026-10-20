package com.siirio.jemworldbosstiers.diagnostic;

/** Pure aggregation policy: net pool loss, native return values and nested calls are separate facts. */
public final class CombatCaptureTotals {
    private int outerCalls;
    private int returnedTrue;
    private int poolLossCalls;
    private double healthLoss;
    private double absorptionLoss;

    public void observe(boolean nested, boolean returned, double healthBefore, double healthAfter,
                        double absorptionBefore, double absorptionAfter) {
        if (nested) return;
        double health = loss(healthBefore, healthAfter);
        double absorption = loss(absorptionBefore, absorptionAfter);
        outerCalls++;
        if (returned) returnedTrue++;
        if (health + absorption > 0) poolLossCalls++;
        healthLoss += health;
        absorptionLoss += absorption;
    }

    private static double loss(double before, double after) {
        if (!Double.isFinite(before) || !Double.isFinite(after))
            throw new IllegalArgumentException("Combat pools must be finite");
        return Math.max(0, before - after);
    }

    public int outerCalls() { return outerCalls; }
    public int returnedTrue() { return returnedTrue; }
    public int poolLossCalls() { return poolLossCalls; }
    public double healthLoss() { return healthLoss; }
    public double absorptionLoss() { return absorptionLoss; }
    public Double healthLossPerGameSecond(int elapsedTicks) {
        return elapsedTicks > 0 ? healthLoss * 20.0 / elapsedTicks : null;
    }
    public Double poolLossCallsPerGameSecond(int elapsedTicks) {
        return elapsedTicks > 0 ? poolLossCalls * 20.0 / elapsedTicks : null;
    }
}
