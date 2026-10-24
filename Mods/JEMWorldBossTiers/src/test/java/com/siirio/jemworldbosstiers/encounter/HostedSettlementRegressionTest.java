package com.siirio.jemworldbosstiers.encounter;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

class HostedSettlementRegressionTest {
    @Test void rewardFailureCannotPublishTerminalSuccessOrCleanup() {
        List<String> calls = new ArrayList<>();
        assertThrows(IllegalStateException.class, () -> HostedSettlement.finish(
                () -> calls.add("intent"),
                () -> { calls.add("reward"); throw new IllegalStateException("disk full"); },
                () -> calls.add("terminal"), () -> calls.add("complete")));
        assertEquals(List.of("intent", "reward"), calls);
    }

    @Test void terminalFailureRetainsPendingSettlement() {
        List<String> calls = new ArrayList<>();
        assertThrows(IllegalStateException.class, () -> HostedSettlement.finish(
                () -> calls.add("intent"), () -> calls.add("reward"),
                () -> { calls.add("terminal"); throw new IllegalStateException("return storage failed"); },
                () -> calls.add("complete")));
        assertEquals(List.of("intent", "reward", "terminal"), calls);
    }

    @Test void eliminatedContributorsAndGenuineSupportKeepEarnedRewardsButIdlePlayersDoNot() {
        UUID fighter = UUID.randomUUID(), healer = UUID.randomUUID(), idle = UUID.randomUUID();
        CompoundTag state = new CompoundTag(), players = new CompoundTag();
        state.putDouble("StartingHealth", 1000);
        CompoundTag fighterState = new CompoundTag();
        fighterState.putString("State", "ELIMINATED");
        fighterState.putDouble("Contribution", 5);
        players.put(fighter.toString(), fighterState);
        CompoundTag healerState = new CompoundTag();
        healerState.putBoolean("Supported", true);
        players.put(healer.toString(), healerState);
        players.put(idle.toString(), new CompoundTag());
        state.put("Players", players);
        assertEquals(java.util.Set.of(fighter, healer), HostedSettlement.recipients(state, .005));
        HostedSettlement.freezeRecipients(state, .005);
        players.getCompound(idle.toString()).putDouble("Contribution", 1000);
        assertEquals(java.util.Set.of(fighter, healer), HostedSettlement.recipients(state, .005));
    }

    @Test void missingStartingHealthDoesNotQualifyIdleMembership() {
        CompoundTag state = new CompoundTag(), players = new CompoundTag();
        players.put(UUID.randomUUID().toString(), new CompoundTag());
        state.put("Players", players);
        assertTrue(HostedSettlement.recipients(state, .005).isEmpty());
    }

    @Test void onlyActualDeathConsumesNativeRespawnReturnAndLaterExitCannotEraseIt() {
        CompoundTag leaving = new CompoundTag();
        HostedSettlement.eliminate(leaving, false);
        assertFalse(leaving.getBoolean("NativeDeath"));
        CompoundTag dead = new CompoundTag();
        HostedSettlement.eliminate(dead, true);
        HostedSettlement.eliminate(dead, false);
        assertTrue(dead.getBoolean("NativeDeath"));
        assertEquals("ELIMINATED", dead.getString("State"));
    }

    @Test void partyLinkFailureCannotReleaseNativeCombat() {
        List<String> calls = new ArrayList<>();
        assertThrows(IllegalStateException.class, () -> HostedSettlement.start(
                () -> { calls.add("attempt"); return true; },
                () -> { calls.add("party"); throw new IllegalStateException("party disk failure"); },
                () -> calls.add("release")));
        assertEquals(List.of("attempt", "party"), calls);
    }

    @Test void failedAttemptAcknowledgmentNeverInvokesPartyOrNativeRelease() {
        List<String> calls = new ArrayList<>();
        assertFalse(HostedSettlement.start(() -> false, () -> calls.add("party"), () -> calls.add("release")));
        assertTrue(calls.isEmpty());
    }

    @Test void nativeCombatReleasesOnlyAfterBothOwnersAcknowledgeStart() {
        List<String> calls = new ArrayList<>();
        assertTrue(HostedSettlement.start(() -> { calls.add("attempt"); return true; },
                () -> calls.add("party"), () -> calls.add("release")));
        assertEquals(List.of("attempt", "party", "release"), calls);
    }

    @Test void partyRecoveryMergesOnlyMonotonicEliminationWithoutLateRecipientsOrChangedContribution() {
        UUID player = UUID.randomUUID(), outsider = UUID.randomUUID();
        CompoundTag state = new CompoundTag(), players = new CompoundTag(), member = new CompoundTag();
        member.putString("State", "ALIVE");
        member.putDouble("Contribution", 5);
        players.put(player.toString(), member);
        state.put("Players", players);
        CompoundTag canonical = state.copy();
        HostedSettlement.eliminate(canonical.getCompound("Players").getCompound(player.toString()), true);
        canonical.getCompound("Players").getCompound(player.toString()).putDouble("Contribution", 999);
        canonical.getCompound("Players").put(outsider.toString(), new CompoundTag());
        HostedSettlement.mergeEliminations(state, canonical);
        assertEquals("ELIMINATED", member.getString("State"));
        assertTrue(member.getBoolean("NativeDeath"));
        assertEquals(5, member.getDouble("Contribution"));
        assertFalse(players.contains(outsider.toString()));
        canonical.getCompound("Players").getCompound(player.toString()).putString("State", "ALIVE");
        canonical.getCompound("Players").getCompound(player.toString()).remove("NativeDeath");
        HostedSettlement.mergeEliminations(state, canonical);
        assertEquals("ELIMINATED", member.getString("State"));
        assertTrue(member.getBoolean("NativeDeath"));
    }

    @Test void supportNeedsDistinctActiveFrozenPlayersAndRealRecipientCombat() {
        UUID helper = UUID.randomUUID(), target = UUID.randomUUID();
        CompoundTag state = new CompoundTag(), players = new CompoundTag();
        CompoundTag healer = new CompoundTag(), fighter = new CompoundTag();
        healer.putString("State", "ALIVE");
        fighter.putString("State", "ALIVE");
        players.put(helper.toString(), healer);
        players.put(target.toString(), fighter);
        state.put("Players", players);
        assertFalse(HostedSettlement.supportEligible(state, helper, target));
        fighter.putDouble("Contribution", 1);
        assertTrue(HostedSettlement.supportEligible(state, helper, target));
        assertFalse(HostedSettlement.supportEligible(state, helper, helper));
        assertFalse(HostedSettlement.supportEligible(state, UUID.randomUUID(), target));
        HostedSettlement.eliminate(healer, false);
        assertFalse(HostedSettlement.supportEligible(state, helper, target));
    }

    @Test void reconnectAfterGraceKeepsAbsenceCauseAndCannotReclassifyNativeDeathOrVoluntaryExit() {
        CompoundTag absent = new CompoundTag();
        absent.putString("State", "ALIVE");
        absent.putLong("DisconnectUntil", 100);
        assertFalse(HostedSettlement.expireAbsence(absent, 99));
        assertTrue(HostedSettlement.expireAbsence(absent, 100));
        absent.remove("DisconnectUntil");
        assertTrue(absent.getBoolean("AbsenceExpired"));
        assertFalse(absent.getBoolean("NativeDeath"));
        CompoundTag dead = new CompoundTag();
        dead.putLong("DisconnectUntil", 100);
        HostedSettlement.eliminate(dead, true);
        assertFalse(HostedSettlement.expireAbsence(dead, 100));
        assertFalse(dead.getBoolean("AbsenceExpired"));
        CompoundTag left = new CompoundTag();
        left.putLong("DisconnectUntil", 100);
        HostedSettlement.eliminate(left, false);
        assertFalse(HostedSettlement.expireAbsence(left, 100));
        assertFalse(left.getBoolean("AbsenceExpired"));
    }
}
