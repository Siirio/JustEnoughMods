package com.siirio.jemworldbosstiers.progression;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProgressionRulesTest {
    private static final List<Integer> THRESHOLDS = List.of(0, 3, 10, 18, 32);
    private static final ResourceLocation ACTIVE_ONE = ResourceLocation.fromNamespaceAndPath("test", "one");
    private static final ResourceLocation ACTIVE_TWO = ResourceLocation.fromNamespaceAndPath("test", "two");
    private static final ResourceLocation REMOVED = ResourceLocation.fromNamespaceAndPath("test", "removed");

    @Test
    void removedHistoricalKeysDoNotIncreaseTheTier() {
        int tier = ProgressionRules.tier(
                Set.of(ACTIVE_ONE, ACTIVE_TWO, REMOVED),
                Set.of(ACTIVE_ONE, ACTIVE_TWO),
                THRESHOLDS
        );

        assertEquals(1, tier);
    }

    @Test
    void thirdActiveQualifyingDefeatReachesTierTwo() {
        int tier = ProgressionRules.tier(
                Set.of(ACTIVE_ONE, ACTIVE_TWO, REMOVED),
                Set.of(ACTIVE_ONE, ACTIVE_TWO, REMOVED),
                THRESHOLDS
        );

        assertEquals(2, tier);
    }

    @Test
    void thresholdsProduceExactlyFiveWorldTiers() {
        assertEquals(1, ProgressionRules.tierForCount(0, THRESHOLDS));
        assertEquals(1, ProgressionRules.tierForCount(2, THRESHOLDS));
        assertEquals(2, ProgressionRules.tierForCount(3, THRESHOLDS));
        assertEquals(2, ProgressionRules.tierForCount(9, THRESHOLDS));
        assertEquals(3, ProgressionRules.tierForCount(10, THRESHOLDS));
        assertEquals(3, ProgressionRules.tierForCount(17, THRESHOLDS));
        assertEquals(4, ProgressionRules.tierForCount(18, THRESHOLDS));
        assertEquals(4, ProgressionRules.tierForCount(28, THRESHOLDS));
        assertEquals(4, ProgressionRules.tierForCount(31, THRESHOLDS));
        assertEquals(5, ProgressionRules.tierForCount(32, THRESHOLDS));
        assertEquals(5, ProgressionRules.tierForCount(48, THRESHOLDS));
    }
}
