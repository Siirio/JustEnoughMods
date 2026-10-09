package com.siirio.jemworldbosstiers.encounter;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class HostedScalingTest {
    @Test void participantsAloneDetermineBothFactorsWithoutCap() {
        assertEquals(new HostedScaling(1, 1), HostedScaling.calculate(1, false, 2, 2));
        assertEquals(new HostedScaling(1.2, 1.2), HostedScaling.calculate(2, false, 2, 2));
        assertEquals(4.8, HostedScaling.calculate(20, false, 2, 2).health(), 0.00001);
        assertEquals(20.8, HostedScaling.calculate(100, false, 2, 2).damage(), 0.00001);
    }
    @Test void raidMultiplierStacksAfterParticipantFactor() {
        var normal = HostedScaling.calculate(8, false, 2, 3);
        var raid = HostedScaling.calculate(8, true, 2, 3);
        assertEquals(normal.health() * 2, raid.health());
        assertEquals(normal.damage() * 3, raid.damage());
    }
    @Test void emptyPartyCannotStart() {
        assertThrows(IllegalArgumentException.class, () -> HostedScaling.calculate(0, false, 2, 2));
        assertThrows(IllegalArgumentException.class, () -> HostedScaling.calculate(-1, false, 2, 2));
    }
}
