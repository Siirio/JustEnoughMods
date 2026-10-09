package com.siirio.jemworldbosstiers.revival;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RespawnOwnershipTest {
    @Test
    void preservesNativeRespawnMechanicsPerBossProfile() {
        assertFalse(RespawnOwnership.usesJemScheduler("cataclysm:void_eye"));
        assertFalse(RespawnOwnership.usesJemScheduler("aquamirae:shell_horn"));
        assertFalse(RespawnOwnership.usesJemScheduler("native_egg"));
    }

    @Test
    void schedulesOnlyProfilesWithoutNativeRespawn() {
        assertTrue(RespawnOwnership.usesJemScheduler("arena_offering"));
        assertTrue(RespawnOwnership.usesJemScheduler("none"));
    }
}
