package com.siirio.jemserver.smp.fishing;

import java.util.Arrays;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FishingChancesTest {
    @Test void missingRaritiesStayZeroAndDisplayedPercentagesSumToOneHundred() {
        double[] odds = FishingChances.percentages(new double[]{1, 1, 1, 0, 0});
        assertEquals(100, Arrays.stream(odds).sum(), .000001);
        assertEquals(0, odds[3]);
        assertEquals(0, odds[4]);
        assertEquals(33.34, odds[0]);
    }
    @Test void emptyPoolDoesNotInventFish() {
        assertArrayEquals(new double[5], FishingChances.percentages(new double[5]));
    }
    @Test void allFiveRaritiesRemainDistinct() {
        assertArrayEquals(new double[]{20, 20, 20, 20, 20}, FishingChances.percentages(new double[]{1, 1, 1, 1, 1}));
    }
}
