package com.siirio.jemworldbosstiers.balance;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TierValuesTest {
    @Test
    void returnsTheExplicitValueForEachWorldTier() {
        TierValues values = new TierValues(80.0D, 95.0D, 115.0D, 130.0D, 145.0D);

        assertEquals(80.0D, values.valueAt(1));
        assertEquals(95.0D, values.valueAt(2));
        assertEquals(115.0D, values.valueAt(3));
        assertEquals(130.0D, values.valueAt(4));
        assertEquals(145.0D, values.valueAt(5));
    }

    @Test
    void rejectsTiersOutsideTheFiveTierContract() {
        TierValues values = new TierValues(1.0D, 2.0D, 3.0D, 4.0D, 5.0D);

        assertThrows(IllegalArgumentException.class, () -> values.valueAt(0));
        assertThrows(IllegalArgumentException.class, () -> values.valueAt(6));
    }

    @Test
    void permitsNegativeRewardContributionsButRejectsNonFiniteValues() {
        TierValues values = new TierValues(-2.0D, 0.0D, 1.0D, 2.0D, 3.0D);

        assertEquals(-2.0D, values.valueAt(1));
        assertThrows(IllegalArgumentException.class, () -> new TierValues(1.0D, Double.NaN, 3.0D, 4.0D, 5.0D));
    }
}
