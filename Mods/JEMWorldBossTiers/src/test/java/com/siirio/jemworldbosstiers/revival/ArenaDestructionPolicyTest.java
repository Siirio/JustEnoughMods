package com.siirio.jemworldbosstiers.revival;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArenaDestructionPolicyTest {
    @Test
    void destructionRequiresArenaAndNonFunctionalBlock() {
        assertFalse(ArenaDestructionPolicy.canDestroy(false, false, false));
        assertFalse(ArenaDestructionPolicy.canDestroy(true, true, false));
        assertFalse(ArenaDestructionPolicy.canDestroy(true, false, true));
        assertTrue(ArenaDestructionPolicy.canDestroy(true, false, false));
    }
}
