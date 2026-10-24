package com.siirio.jemserver.smp.events;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PartyTeleportFlowRegressionTest {
    @Test void livingOfflineEliminationRetainsReturnButActualDeathDiscardsIt() {
        var member = new com.siirio.jemserver.smp.PartyMember();
        var origin = new com.siirio.jemserver.smp.WorldPosition(new net.minecraft.resources.ResourceLocation("minecraft:overworld"), 1, 64, 3, 0, 0);
        member.eliminated(true); member.returnPending(true); member.returnPoint(origin);
        assertFalse(PartyTeleportFlow.discardNativeDeathReturn(member));
        assertTrue(member.returnPending()); assertEquals(origin, member.returnPoint());
        member.nativeDeath(true);
        assertTrue(PartyTeleportFlow.discardNativeDeathReturn(member));
        assertFalse(member.returnPending()); assertNull(member.returnPoint());
    }

    @Test void failedSecondTransferRollsBackFirstAndFailingTransfer() {
        var history = new ArrayList<String>();
        assertThrows(IllegalStateException.class, () -> PartyTeleportFlow.transferAll(List.of(
                () -> history.add("first"),
                () -> { history.add("second-partial"); throw new IllegalStateException("rejected"); }),
                List.of(() -> history.add("rollback-first"), () -> history.add("rollback-second"))));
        assertEquals(List.of("first", "second-partial", "rollback-second", "rollback-first"), history);
    }

    @Test void failedRollbackDoesNotSuppressOtherRestorationOrOriginalFailure() {
        var history = new ArrayList<String>();
        var failure = assertThrows(IllegalStateException.class, () -> PartyTeleportFlow.transferAll(List.of(
                () -> history.add("first"), () -> { throw new IllegalStateException("transfer"); }),
                List.of(() -> history.add("restored-first"), () -> { throw new IllegalStateException("rollback"); })));
        assertEquals("transfer", failure.getMessage());
        assertEquals(1, failure.getSuppressed().length);
        assertEquals(List.of("first", "restored-first"), history);
    }
}
