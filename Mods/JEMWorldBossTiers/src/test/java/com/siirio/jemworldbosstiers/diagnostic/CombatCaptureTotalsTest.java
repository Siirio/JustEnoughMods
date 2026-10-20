package com.siirio.jemworldbosstiers.diagnostic;

/** Standalone Java 17 regression checks; no Forge launch, fixtures, world or external dependencies. */
public final class CombatCaptureTotalsTest {
    public static void main(String[] args) {
        CombatCaptureTotals totals = new CombatCaptureTotals();
        totals.observe(false, false, 100, 100, 8, 8); // Native rejection.
        totals.observe(false, true, 100, 100, 8, 8); // A true return does not prove damage.
        totals.observe(false, true, 100, 100, 8, 3); // Absorption-only loss is effective damage.
        totals.observe(true, true, 100, 96, 3, 0); // Nested loss already included by outer snapshot.
        totals.observe(false, true, 100, 96, 3, 0);
        totals.observe(false, false, 96, 100, 0, 5); // Healing is not negative damage.
        require(totals.outerCalls() == 5, "nested calls must not inflate frequency");
        require(totals.returnedTrue() == 3, "native return count remains distinct");
        require(totals.poolLossCalls() == 2, "only actual pool decreases count");
        require(totals.healthLoss() == 4 && totals.absorptionLoss() == 8, "nested pools counted once");
        require(totals.healthLossPerGameSecond(100) == 0.8, "rate includes entire five-second capture");
        require(totals.poolLossCallsPerGameSecond(100) == 0.4, "hit rate uses actual pool-loss count");
        require(totals.healthLossPerGameSecond(0) == null, "zero-duration capture has no fabricated rate");
        require(totals.poolLossCallsPerGameSecond(-1) == null, "invalid duration has no rate");
        boolean rejected = false;
        try { new CombatCaptureTotals().observe(false, true, Double.NaN, 0, 0, 0); }
        catch (IllegalArgumentException expected) { rejected = true; }
        require(rejected, "non-finite pools must not fabricate evidence");
        System.out.println("CombatCaptureTotals: all regression checks passed");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
