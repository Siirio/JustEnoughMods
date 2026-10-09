package com.siirio.jemserver;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class SleepBallotTest {
    private final UUID initiator = UUID.randomUUID();
    private final UUID other = UUID.randomUUID();
    private final UUID third = UUID.randomUUID();
    private final Set<UUID> online = Set.of(initiator, other, third);

    @Test
    void automaticSleeperVoteCannotBeReplacedOrCountedTwice() {
        SleepBallot vote = new SleepBallot();
        assertTrue(vote.cast(initiator, true, online));
        assertFalse(vote.cast(initiator, false, online));
        assertFalse(vote.cast(initiator, true, online));
        assertEquals(1, vote.agreements(online));
        assertEquals(0, vote.disagreements(online));
    }

    @Test
    void majorityUsesResponsesAndTieRetainsNight() {
        SleepBallot vote = new SleepBallot();
        assertFalse(vote.approved(online));
        vote.cast(initiator, true, online);
        assertTrue(vote.approved(online));
        vote.cast(other, false, online);
        assertFalse(vote.approved(online));
        vote.cast(third, false, online);
        assertFalse(vote.approved(online));
        assertEquals(2, vote.disagreements(online));
    }

    @Test
    void rosterChangesCountCurrentPlayersAndRejoinCannotChangeChoice() {
        SleepBallot vote = new SleepBallot();
        vote.cast(initiator, true, online);
        vote.cast(other, false, online);
        assertTrue(vote.approved(Set.of(initiator)));
        assertFalse(vote.cast(other, true, online));
        assertFalse(vote.approved(online));
    }

    @Test
    void ineligiblePlayersCannotVoteAndNewJoinerCanVote() {
        SleepBallot vote = new SleepBallot();
        UUID joiner = UUID.randomUUID();
        assertFalse(vote.cast(joiner, true, online));
        assertNull(vote.choice(joiner));
        assertTrue(vote.cast(joiner, false, Set.of(initiator, joiner)));
        assertEquals(1, vote.disagreements(Set.of(initiator, joiner)));
    }
}
