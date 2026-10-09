package com.siirio.jemworldbosstiers.encounter;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpawnProvenanceRulesTest {
    @Test
    void commandSpawnCannotEstablishProgression() {
        SpawnDecision decision = SpawnProvenanceRules.decide("COMMAND", false);

        assertFalse(decision.progressionEligible());
        assertFalse(decision.rematch());
    }

    @Test
    void structureSpawnIsEligibleBeforeFirstDefeat() {
        SpawnDecision decision = SpawnProvenanceRules.decide("STRUCTURE", false);

        assertTrue(decision.progressionEligible());
        assertFalse(decision.rematch());
    }

    @Test
    void spawnEggIsEligibleBeforeFirstDefeat() {
        SpawnDecision decision = SpawnProvenanceRules.decide("SPAWN_EGG", false);

        assertTrue(decision.progressionEligible());
        assertFalse(decision.rematch());
    }

    @Test
    void spawnEggAfterFirstDefeatIsARematch() {
        SpawnDecision decision = SpawnProvenanceRules.decide("SPAWN_EGG", true);

        assertFalse(decision.progressionEligible());
        assertTrue(decision.rematch());
    }

    @Test
    void legitimateSpawnAfterFirstDefeatIsAlwaysARematch() {
        SpawnDecision decision = SpawnProvenanceRules.decide("MOB_SUMMONED", true);

        assertFalse(decision.progressionEligible());
        assertTrue(decision.rematch());
    }
}
