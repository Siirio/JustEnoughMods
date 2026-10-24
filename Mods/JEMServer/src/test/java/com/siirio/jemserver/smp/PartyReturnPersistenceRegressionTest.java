package com.siirio.jemserver.smp;

import org.junit.jupiter.api.Test;
import net.minecraft.resources.ResourceLocation;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class PartyReturnPersistenceRegressionTest {
    @Test void nativeDeathReasonIsPersistedSeparatelyFromElimination() {
        var member = new EventMember(); member.eliminated(true);
        var left = EventNbtCodec.decodeEventMember(EventNbtCodec.encode(member));
        assertTrue(left.eliminated()); assertFalse(left.nativeDeath());
        member.nativeDeath(true);
        assertTrue(EventNbtCodec.decodeEventMember(EventNbtCodec.encode(member)).nativeDeath());
    }

    @Test void pendingOfflineReturnAndEliminationSurvivePartySerialization() {
        var id = UUID.randomUUID();
        var party = new PartyState();
        party.id(UUID.randomUUID()); party.owner(id); party.state("FAILED");
        party.partyTeleportSession(UUID.randomUUID());
        var member = new PartyMember();
        member.accepted(true); member.eliminated(true); member.returnPending(true);
        member.returnPoint(new WorldPosition(new ResourceLocation("minecraft:the_nether"), 1, 65, 3, 4, 5));
        party.addMember(id, member);
        var recovered = PartyStateNbtCodec.decode(PartyStateNbtCodec.encode(party));
        assertTrue(recovered.member(id).returnPending());
        assertTrue(recovered.member(id).eliminated());
        assertEquals(member.returnPoint(), recovered.member(id).returnPoint());
        assertEquals(party.partyTeleportSession(), recovered.partyTeleportSession());
    }
}
