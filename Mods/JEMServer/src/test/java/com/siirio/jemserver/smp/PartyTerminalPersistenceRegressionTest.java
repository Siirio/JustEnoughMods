package com.siirio.jemserver.smp;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class PartyTerminalPersistenceRegressionTest {
    @Test void failedTerminalWriteRetainsRetryIntentAndAttemptAcknowledgment() {
        var party = new PartyState();
        var attempt = UUID.randomUUID(); party.closedAttempt(attempt);
        party.state("RECOVERY"); party.pendingOutcome("COMPLETED");
        assertThrows(IllegalStateException.class, () -> HostedParties.acknowledgeClose(party,
                () -> { throw new IllegalStateException("disk unavailable"); }));
        assertEquals("RECOVERY", party.state());
        assertEquals("COMPLETED", party.pendingOutcome());
        assertEquals(attempt, party.closedAttempt());
        HostedParties.acknowledgeClose(party, () -> {});
        assertEquals("COMPLETED", party.state());
        assertEquals("", party.pendingOutcome());
        assertEquals(attempt, party.closedAttempt());
    }
}
