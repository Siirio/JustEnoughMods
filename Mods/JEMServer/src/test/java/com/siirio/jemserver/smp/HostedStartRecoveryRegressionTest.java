package com.siirio.jemserver.smp;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class HostedStartRecoveryRegressionTest {
    @Test void exactPreparedLinkIsRecoverableWithoutRevivingEliminatedMembers() {
        var party = new PartyState(); var owner = UUID.randomUUID();
        party.id(UUID.randomUUID()); party.owner(owner); party.state("DRAFT");
        var member = new PartyMember(); member.accepted(true); member.eliminated(true); party.addMember(owner, member);
        var boss = UUID.randomUUID(); var attempt = UUID.randomUUID();
        var state = new CompoundTag(); state.putUUID("Party", party.id()); state.putUUID("Boss", boss); state.putUUID("Attempt", attempt);
        var players = new CompoundTag(); players.put(owner.toString(), new CompoundTag()); state.put("Players", players);
        assertTrue(HostedParties.validHostedLink(party, boss, attempt, state));
        assertTrue(party.member(owner).eliminated());
        party.hostedAttempt(UUID.randomUUID());
        assertFalse(HostedParties.validHostedLink(party, boss, attempt, state));
        party.hostedAttempt(null); party.pendingOutcome("FAILED");
        assertFalse(HostedParties.validHostedLink(party, boss, attempt, state));
    }
}
