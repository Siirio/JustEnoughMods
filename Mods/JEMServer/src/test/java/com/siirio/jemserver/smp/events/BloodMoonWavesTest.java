package com.siirio.jemserver.smp.events;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BloodMoonWavesTest {
    @Test void exactSoloTotalsAndCaps() {
        assertEquals(264, java.util.stream.IntStream.rangeClosed(1, 5).map(w -> BloodMoonWaves.total(1, w, 1)).sum());
        assertEquals(468, java.util.stream.IntStream.rangeClosed(1, 5).map(w -> BloodMoonWaves.total(5, w, 1)).sum());
        assertEquals(22, BloodMoonWaves.maxAlive(1, 5, 1));
        assertEquals(42, BloodMoonWaves.maxAlive(5, 5, 12));
        assertEquals(145, BloodMoonWaves.total(1, 5, 5));
    }
    @Test void compositionAlwaysPreservesTotalsAndSingleBoss() {
        for (int tier = 1; tier <= 5; tier++) for (int wave = 1; wave <= 5; wave++) for (int players = 1; players <= 8; players++) {
            var counts = BloodMoonWaves.composition(tier, wave, players);
            assertEquals(BloodMoonWaves.total(tier, wave, players), counts.values().stream().mapToInt(Integer::intValue).sum());
            assertEquals(wave == 3 || wave == 5 ? 1 : 0, counts.getOrDefault(BloodMoonWaves.Category.MINIBOSS, 0));
        }
    }
}
