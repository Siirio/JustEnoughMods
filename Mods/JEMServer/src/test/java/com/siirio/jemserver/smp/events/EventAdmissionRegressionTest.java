package com.siirio.jemserver.smp.events;

import com.siirio.jemserver.smp.EventMember;
import com.siirio.jemserver.smp.EventState;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class EventAdmissionRegressionTest {
    @Test void eliminatedMemberCannotBeAcceptedAgain() {
        var id = UUID.randomUUID();
        var event = new EventState();
        var member = new EventMember(); member.accepted(true); member.eliminated(true);
        event.addMember(id, member);
        assertFalse(EventSession.canAccept(event, id));
        assertTrue(event.member(id).eliminated());
    }

    @Test void frozenSnapshotRejectsBothLateJoinAndExistingMemberReacceptance() {
        var admitted = UUID.randomUUID();
        var event = new EventState(); event.encounterParticipants(List.of(admitted));
        assertFalse(EventSession.canAccept(event, UUID.randomUUID()));
        assertFalse(EventSession.canAccept(event, admitted));
        assertEquals(List.of(admitted), event.encounterParticipants());
    }
}
