package com.siirio.jemserver.smp.fishing;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FishingXpTest {
    @Test void baseCatchStaysWithinEveryDimensionRange() {
        for (int[] range : new int[][]{{45, 80}, {80, 100}, {100, 200}})
            for (int rarity = 0; rarity < 5; rarity++)
                for (double difficulty : new double[]{0, .5, 1, 10}) {
                    int xp = FishingXp.calculate(range[0], range[1], rarity, difficulty, .5, .5, 1, false);
                    assertTrue(xp >= range[0] && xp <= range[1]);
                }
    }
    @Test void difficultyAndRodBothAffectReward() {
        int baseline = FishingXp.calculate(100, 200, 2, 0, 0, 0, 1, false);
        assertTrue(FishingXp.calculate(100, 200, 2, 1, 0, 0, 1, false) > baseline);
        assertTrue(FishingXp.calculate(100, 200, 2, 0, 1, 0, 1, false) > baseline);
    }
    @Test void voidBonusMayExceedDimensionCapUnlessConfigured() {
        assertEquals(240, FishingXp.calculate(100, 200, 4, 1, 1, 1, 1.2, false));
        assertEquals(200, FishingXp.calculate(100, 200, 4, 1, 1, 1, 1.2, true));
    }
}
