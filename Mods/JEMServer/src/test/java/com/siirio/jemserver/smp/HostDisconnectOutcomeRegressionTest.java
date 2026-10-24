package com.siirio.jemserver.smp;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HostDisconnectOutcomeRegressionTest {
    @Test void onlyExpiredLivingHostAbsenceFailsTheWholeAttempt() {
        assertTrue(HostedParties.absentHostDefeat(true, true, "ELIMINATED", false, true));
        assertFalse(HostedParties.absentHostDefeat(false, true, "ELIMINATED", false, true));
        assertFalse(HostedParties.absentHostDefeat(true, true, "ELIMINATED", true, true));
        assertFalse(HostedParties.absentHostDefeat(true, true, "DISCONNECTED", false, false));
        assertFalse(HostedParties.absentHostDefeat(true, false, "ALIVE", false, false));
    }
}
