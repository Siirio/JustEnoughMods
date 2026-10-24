package com.siirio.jemserver.smp.events;

import com.siirio.jemserver.smp.PartyMember;
import com.siirio.jemserver.smp.PartyState;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

final class HostedBoundaryAdmissionRegressionTest extends NativeRegistryFixture {
    @Test void entryIsSingleUseAndEliminationNeverReopensIt() {
        var id = UUID.randomUUID();
        var player = mock(ServerPlayer.class);
        when(player.getUUID()).thenReturn(id); when(player.isAlive()).thenReturn(true);
        var party = new PartyState(); party.state("ACTIVE");
        var member = new PartyMember(); member.accepted(true); party.addMember(id, member);
        assertTrue(HostedBoundary.passable(player, party));
        member.arenaEntered(true);
        assertFalse(HostedBoundary.passable(player, party));
        assertTrue(HostedBoundary.locked(player, party));
        member.eliminated(true); member.arenaEntered(false);
        assertFalse(HostedBoundary.authorized(player, party));
        assertFalse(HostedBoundary.passable(player, party));
    }

    @Test void terminalPartiesAndDeadPlayersNeverReceiveAdmission() {
        var id = UUID.randomUUID(); var player = mock(ServerPlayer.class);
        when(player.getUUID()).thenReturn(id); when(player.isAlive()).thenReturn(true);
        var party = new PartyState(); party.state("COMPLETED");
        var member = new PartyMember(); member.accepted(true); party.addMember(id, member);
        assertFalse(HostedBoundary.authorized(player, party));
        party.state("ACTIVE"); when(player.isAlive()).thenReturn(false);
        assertFalse(HostedBoundary.authorized(player, party));
    }

    @Test void oldAttemptTokenCannotForfeitANewAttemptUsingTheSameBoss() {
        var party = new PartyState(); party.bossEntity(UUID.randomUUID());
        var old = UUID.randomUUID(); party.hostedAttempt(old);
        assertTrue(HostedBoundary.matchesAction(null, party, old));
        var current = UUID.randomUUID(); party.hostedAttempt(current);
        assertFalse(HostedBoundary.matchesAction(null, party, old));
        assertTrue(HostedBoundary.matchesAction(null, party, current));
    }
}
