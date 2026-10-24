package com.siirio.jemserver.smp.events;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class RaidGeometryRegressionTest {
    private final AABB boss = new AABB(4, 1, 4, 6, 5, 6);
    private final Vec3 feet = new Vec3(5, 1, 5);

    @Test void approvedEnlargementIsTheMaximumEvenWithUnlimitedSpace() {
        assertEquals(1.35, RaidEvent.safeScaleFactor(boss, feet, new AABB(0, 0, 0, 20, 20, 20), box -> true), .00001);
    }

    @Test void ceilingLimitsSizeBeforeCollisionOrSuffocation() {
        double factor = RaidEvent.safeScaleFactor(boss, feet, new AABB(0, 0, 0, 10, 5.5, 10), box -> true);
        assertEquals(1.125, factor, .00001);
    }

    @Test void fullWidthAndNativeBlockCollisionBothConstrainEnlargement() {
        assertEquals(1.1, RaidEvent.safeScaleFactor(boss, feet, new AABB(3.9, 0, 0, 6.1, 20, 20), box -> true), .00001);
        assertEquals(1.2, RaidEvent.safeScaleFactor(boss, feet, new AABB(0, 0, 0, 20, 20, 20), box -> box.maxX <= 6.2), .00001);
    }

    @Test void unsafeNativeBaselineNeverGetsEnlarged() {
        assertEquals(1, RaidEvent.safeScaleFactor(boss, feet, new AABB(0, 0, 0, 20, 20, 20), box -> false));
        assertEquals(1, RaidEvent.safeScaleFactor(boss, feet, new AABB(0, 0, 0, 20, 4, 20), box -> true));
    }
}
