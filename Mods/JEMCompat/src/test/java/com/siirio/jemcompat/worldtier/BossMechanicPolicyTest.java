package com.siirio.jemcompat.worldtier;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BossMechanicPolicyTest {
    @Test
    void staggerThresholdTracksScaledMaximumHealth() {
        assertEquals(155.0f, BossMechanicPolicy.scaledAbsoluteThreshold(100.0f, 500.0f, 775.0f));
        assertEquals(70.0f, BossMechanicPolicy.scaledAbsoluteThreshold(25.0f, 150.0f, 420.0f));
    }

    @Test
    void emergencyHealingTriggerTracksScaledMaximumHealth() {
        assertEquals(26.0f, BossMechanicPolicy.scaledAbsoluteThreshold(20.0f, 300.0f, 390.0f));
    }

    @Test
    void invalidNativeHealthLeavesVerifiedConstantUntouched() {
        assertEquals(20.0f, BossMechanicPolicy.scaledAbsoluteThreshold(20.0f, 0.0f, 390.0f));
    }
}
