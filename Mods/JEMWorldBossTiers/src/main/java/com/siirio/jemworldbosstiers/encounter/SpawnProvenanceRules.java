package com.siirio.jemworldbosstiers.encounter;

import java.util.Set;

public final class SpawnProvenanceRules {
    private static final Set<String> LEGITIMATE_TYPES = Set.of("NATURAL", "CHUNK_GENERATION", "SPAWNER", "STRUCTURE", "MOB_SUMMONED", "TRIGGERED", "EVENT", "SPAWN_EGG");

    private SpawnProvenanceRules() {
    }

    public static SpawnDecision decide(String spawnType, boolean previouslyDefeated) {
        if (!LEGITIMATE_TYPES.contains(spawnType)) {
            return new SpawnDecision(false, false);
        }
        return previouslyDefeated ? new SpawnDecision(false, true) : new SpawnDecision(true, false);
    }
}
