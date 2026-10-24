package com.siirio.jemworldbosstiers.progression;

import com.siirio.jemworldbosstiers.revival.ArenaRecord;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WorldTierEncounterPersistenceTest {
    private static final ResourceLocation BOSS = new ResourceLocation("minecraft", "wither");

    private static WorldTierData state() {
        WorldTierData state = new WorldTierData();
        state.putArena(ArenaRecord.create("arena", BOSS, "minecraft:overworld", 0, -50, 0, 20, -30, 20)
                .asStructureArena().unlock());
        return state;
    }

    @Test void duplicateDeathKeepsFirstExactThreeDayDueTime() {
        WorldTierData state = state();
        state.scheduleRespawn("arena", BOSS, 100 + 72_000L);
        state.scheduleRespawn("arena", BOSS, 300 + 72_000L);
        assertEquals(72_100L, state.respawn("arena").orElseThrow().dueTime());
        WorldTierData loaded = WorldTierData.load(state.save(new CompoundTag()));
        assertEquals(72_100L, loaded.respawn("arena").orElseThrow().dueTime());
    }

    @Test void pendingAndCompletedSettlementSurviveReloadWithoutAliasing() {
        WorldTierData state = state();
        UUID attempt = UUID.randomUUID();
        CompoundTag frozen = new CompoundTag();
        frozen.putUUID("Boss", UUID.randomUUID());
        frozen.putString("Loot", "frozen-once");
        state.putHostedSettlement(attempt, frozen);
        frozen.putString("Loot", "mutated-caller");
        WorldTierData loaded = WorldTierData.load(state.save(new CompoundTag()));
        assertEquals("frozen-once", loaded.hostedSettlements().get(attempt).getString("Loot"));
        loaded.hostedSettlements().get(attempt).putString("Loot", "mutated-reader");
        assertEquals("frozen-once", loaded.hostedSettlements().get(attempt).getString("Loot"));
        loaded.completeHostedSettlement(attempt);
        WorldTierData completed = WorldTierData.load(loaded.save(new CompoundTag()));
        assertTrue(completed.hostedSettlementCompleted(attempt));
        assertFalse(completed.hostedSettlements().containsKey(attempt));
        completed.putHostedSettlement(attempt, frozen);
        assertFalse(completed.hostedSettlements().containsKey(attempt));
    }

    @Test void unresolvedCanonicalBossClaimCannotBeReplacedByAnotherUuid() {
        WorldTierData state = state();
        UUID existing = UUID.randomUUID();
        assertTrue(state.recordLiveBoss("arena", existing));
        WorldTierData loaded = WorldTierData.load(state.save(new CompoundTag()));
        assertFalse(loaded.recordLiveBoss("arena", UUID.randomUUID()));
        assertEquals(existing, loaded.liveBoss("arena").orElseThrow());
        loaded.clearLiveBoss("arena", UUID.randomUUID());
        assertEquals(existing, loaded.liveBoss("arena").orElseThrow());
    }

    @Test void duplicateAttemptCannotResetCooldownButLaterLegitimateDeathHasItsOwnDueTime() {
        WorldTierData state = state();
        UUID firstDeath = UUID.randomUUID(), laterDeath = UUID.randomUUID();
        state.scheduleRespawn("arena", BOSS, 72_100L, firstDeath);
        state.scheduleRespawn("arena", BOSS, 72_300L, firstDeath);
        assertEquals(72_100L, state.respawn("arena").orElseThrow().dueTime());
        state.markRaidReplacement("arena");
        WorldTierData loaded = WorldTierData.load(state.save(new CompoundTag()));
        loaded.clearRaidReplacement("arena");
        assertEquals(72_100L, loaded.respawn("arena").orElseThrow().dueTime());
        loaded.scheduleRespawn("arena", BOSS, 72_500L, laterDeath);
        assertEquals(72_500L, loaded.respawn("arena").orElseThrow().dueTime());
    }

    @Test void failedAttemptCannotRejoinOrSettleVictoryAfterRestart() {
        WorldTierData state = state();
        UUID attempt = UUID.randomUUID();
        CompoundTag snapshot = new CompoundTag(); snapshot.putUUID("Boss", UUID.randomUUID());
        state.putHostedAttempt(attempt, snapshot);
        state.failHostedAttempt(attempt);
        WorldTierData loaded = WorldTierData.load(state.save(new CompoundTag()));
        loaded.putHostedAttempt(attempt, snapshot);
        loaded.putHostedSettlement(attempt, snapshot);
        assertTrue(loaded.hostedAttemptFailed(attempt));
        assertFalse(loaded.hostedAttempts().containsKey(attempt));
        assertFalse(loaded.hostedSettlements().containsKey(attempt));
        assertThrows(IllegalStateException.class, () -> loaded.completeHostedSettlement(attempt));
    }

    @Test void preparedCreationKeepsExactNativeIdentityAcrossRestartAndRejectsAnotherSpawn() {
        WorldTierData state = state();
        UUID expected = UUID.randomUUID();
        CompoundTag nativeEntity = new CompoundTag();
        nativeEntity.putString("id", BOSS.toString()); nativeEntity.putUUID("UUID", expected);
        state.beginBossCreation("arena", nativeEntity);
        state.recordLiveBoss("arena", expected);
        WorldTierData loaded = WorldTierData.load(state.save(new CompoundTag()));
        assertEquals(expected, loaded.pendingBossCreations().get("arena").getUUID("UUID"));
        nativeEntity.putUUID("UUID", UUID.randomUUID());
        assertThrows(IllegalStateException.class, () -> loaded.beginBossCreation("arena", nativeEntity));
        loaded.clearBossCreation("arena", UUID.randomUUID());
        assertTrue(loaded.pendingBossCreations().containsKey("arena"));
        loaded.clearBossCreation("arena", expected);
        assertFalse(loaded.pendingBossCreations().containsKey("arena"));
        assertEquals(expected, loaded.liveBoss("arena").orElseThrow());
    }

    @Test void canonicalCloseReceiptPrecedesFinalRewardCleanupAndSurvivesRetry() {
        WorldTierData state = state();
        UUID attempt = UUID.randomUUID();
        state.markCanonicalHostedClosed(attempt);
        WorldTierData loaded = WorldTierData.load(state.save(new CompoundTag()));
        assertTrue(loaded.canonicalHostedClosed(attempt));
        assertFalse(loaded.hostedSettlementCompleted(attempt));
    }

    @Test void retiredMovingBossCannotReclaimArenaFromAnOlderNativeChunkSave() {
        WorldTierData state = state();
        UUID old = UUID.randomUUID(), replacement = UUID.randomUUID();
        state.retireBossIdentity("arena", old);
        WorldTierData loaded = WorldTierData.load(state.save(new CompoundTag()));
        assertTrue(loaded.bossIdentityRetired(old));
        assertFalse(loaded.recordLiveBoss("arena", old));
        assertTrue(loaded.recordLiveBoss("arena", replacement));
        assertFalse(loaded.recordLiveBoss("arena", old));
        assertEquals(replacement, loaded.liveBoss("arena").orElseThrow());
    }

    @Test void legacyAliasMigratesCreationIntentAndCanClearItWithoutLaterReplay() {
        WorldTierData state = state();
        String legacy = "minecraft:overworld|minecraft:wither|10|10";
        state.putArena(ArenaRecord.create(legacy, BOSS, "minecraft:overworld", 0, -50, 0, 20, -30, 20).unlock());
        UUID boss = UUID.randomUUID();
        CompoundTag nativeEntity = new CompoundTag(); nativeEntity.putString("id", BOSS.toString()); nativeEntity.putUUID("UUID", boss);
        state.beginBossCreation(legacy, nativeEntity);
        state.recordLiveBoss(legacy, boss);
        state.aliasArena(legacy, "arena");
        WorldTierData loaded = WorldTierData.load(state.save(new CompoundTag()));
        assertEquals(boss, loaded.pendingBossCreations().get("arena").getUUID("UUID"));
        assertFalse(loaded.pendingBossCreations().containsKey(legacy));
        loaded.clearBossCreation(legacy, boss);
        assertTrue(loaded.pendingBossCreations().isEmpty());
    }

    @Test void failedStartRollbackCannotEraseAnotherAttemptsRaidParticipation() {
        WorldTierData state = state();
        UUID player = UUID.randomUUID(), attempt = UUID.randomUUID();
        state.recordRaidParticipation("arena", java.util.List.of(player), attempt);
        WorldTierData loaded = WorldTierData.load(state.save(new CompoundTag()));
        loaded.rollbackRaidParticipation("arena", UUID.randomUUID(), java.util.List.of(player));
        assertTrue(loaded.hasRaidParticipation("arena", player));
        loaded.rollbackRaidParticipation("arena", attempt, java.util.List.of(player));
        assertFalse(loaded.hasRaidParticipation("arena", player));
    }
}
