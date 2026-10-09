package com.siirio.jemcompat.feature.backtobed;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GroupTeleportPolicyTest {
    @Test
    void derivesTheDayFromOverworldGameTime() {
        assertTrue(GroupTeleportPolicy.day(0) == 0);
        assertTrue(GroupTeleportPolicy.day(23_999) == 0);
        assertTrue(GroupTeleportPolicy.day(24_000) == 1);
        assertTrue(GroupTeleportPolicy.day(48_000) == 2);
    }

    @Test
    void blocksOnlyAHostWhoAlreadyActivatedOnTheCurrentDay() {
        assertTrue(GroupTeleportPolicy.canActivate(7, false, 0));
        assertFalse(GroupTeleportPolicy.canActivate(7, true, 7));
        assertTrue(GroupTeleportPolicy.canActivate(8, true, 7));
    }

    @Test
    void includesPlayersInsideTheFormationArea() {
        assertTrue(GroupTeleportPolicy.isNearby(10.0, 64.0, 10.0, 11.75, 65.0, 8.25));
        assertTrue(GroupTeleportPolicy.isNearby(10.0, 64.0, 10.0, 8.25, 63.0, 11.75));
    }

    @Test
    void excludesPlayersOutsideAnyFormationAxis() {
        assertFalse(GroupTeleportPolicy.isNearby(10.0, 64.0, 10.0, 11.751, 64.0, 10.0));
        assertFalse(GroupTeleportPolicy.isNearby(10.0, 64.0, 10.0, 10.0, 65.001, 10.0));
        assertFalse(GroupTeleportPolicy.isNearby(10.0, 64.0, 10.0, 10.0, 64.0, 8.249));
    }
}
