package com.siirio.jemserver.smp.events;

import java.util.UUID;
import net.minecraft.world.phys.AABB;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

final class SolidBoundaryRegressionTest extends NativeRegistryFixture {
    private final EventBoundary underground = SolidBoundary.boundary(UUID.randomUUID(), "minecraft:overworld",
            0, 0, 15, 15, -40, -21, 0, false);

    @Test void protectsEveryFaceOfTheNativeVolume() {
        assertTrue(SolidBoundary.intersectsWall(new AABB(-.1, -35, 5, .1, -33, 6), underground));
        assertTrue(SolidBoundary.intersectsWall(new AABB(15.9, -35, 5, 16.1, -33, 6), underground));
        assertTrue(SolidBoundary.intersectsWall(new AABB(5, -35, -.1, 6, -33, .1), underground));
        assertTrue(SolidBoundary.intersectsWall(new AABB(5, -35, 15.9, 6, -33, 16.1), underground));
        assertTrue(SolidBoundary.intersectsWall(new AABB(5, -40.1, 5, 6, -39.9, 6), underground));
        assertTrue(SolidBoundary.intersectsWall(new AABB(5, -20.1, 5, 6, -19.9, 6), underground));
    }

    @Test void undergroundArenaDoesNotBlockTheColumnAboveIt() {
        AABB above = new AABB(-1, 70, 5, -.3, 72, 6);
        assertFalse(SolidBoundary.crossesWall(above, above.move(20, 0, 0), underground));
    }

    @Test void detectsHighSpeedPassageWithBothEndpointsOutside() {
        AABB outside = new AABB(-8, -35, 5, -7, -33, 6);
        assertTrue(SolidBoundary.crossesWall(outside, outside.move(40, 0, 0), underground));
    }

    @Test void detectsCornerAndVerticalPassageWithCompleteBody() {
        AABB outside = new AABB(-3, -35, -3, -2, -33, -2);
        assertTrue(SolidBoundary.crossesWall(outside, outside.move(8, 0, 8), underground));
        AABB below = new AABB(5, -50, 5, 6, -48, 6);
        assertTrue(SolidBoundary.crossesWall(below, below.move(0, 40, 0), underground));
    }

    @Test void doesNotRejectDiagonalMotionThatMissesTheCorner() {
        AABB outside = new AABB(-5, -35, 2, -4, -33, 3);
        assertFalse(SolidBoundary.crossesWall(outside, outside.move(6, 0, -6), underground));
    }

    @Test void rejectsPartialBodyContainment() {
        assertFalse(SolidBoundary.contains(new AABB(5, -22, 5, 6, -19, 6), underground));
        assertFalse(SolidBoundary.contains(new AABB(15.8, -35, 5, 16.4, -33, 6), underground));
        assertTrue(SolidBoundary.contains(new AABB(5, -35, 5, 6, -33, 6), underground));
    }

    @Test void authorizedEntryOpensOnlyTheApproachedFaceAndKeepsFarFaceSolid() {
        EventBoundary entry = SolidBoundary.boundary(underground.id(), underground.dimension(), 0, 0, 15, 15, -40, -21, 0, true);
        var approaching = new AABB(-2, -35, 5, -1, -33, 6);
        var shapes = SolidBoundary.admissionShapes(entry, approaching);
        assertEquals(5, shapes.size());
        assertTrue(shapes.stream().noneMatch(shape -> shape.bounds().minX < 0 && shape.bounds().maxX == 0));
        assertTrue(shapes.stream().anyMatch(shape -> shape.bounds().minX == 16));
        assertEquals(6, SolidBoundary.admissionShapes(entry, approaching.move(7, 0, 0)).size());
    }

    @Test void fullVehicleFamilyIncludesAllPassengersAndSiblings() {
        var mount = mock(net.minecraft.world.entity.Entity.class);
        var rider = mock(net.minecraft.world.entity.Entity.class);
        var sibling = mock(net.minecraft.world.entity.Entity.class);
        when(rider.getRootVehicle()).thenReturn(mount);
        when(mount.getBoundingBox()).thenReturn(new AABB(4, -35, 4, 6, -33, 6));
        when(rider.getBoundingBox()).thenReturn(new AABB(4.5, -34, 4.5, 5.5, -32, 5.5));
        when(sibling.getBoundingBox()).thenReturn(new AABB(5.5, -34, 4.5, 7, -31, 5.5));
        when(mount.getSelfAndPassengers()).thenAnswer(invocation -> java.util.stream.Stream.of(mount, rider, sibling));
        var family = SolidBoundary.movementBox(rider);
        assertEquals(7, family.maxX);
        assertEquals(-31, family.maxY);
        assertEquals(4, family.minX);
    }
}
