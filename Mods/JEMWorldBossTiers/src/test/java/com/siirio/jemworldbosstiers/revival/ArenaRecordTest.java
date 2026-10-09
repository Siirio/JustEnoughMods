package com.siirio.jemworldbosstiers.revival;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArenaRecordTest {
    private static final ResourceLocation PROFILE = ResourceLocation.fromNamespaceAndPath("test", "boss");

    @Test
    void permitsOnlyOneActiveRevivalEncounter() {
        ArenaRecord arena = ArenaRecord.create("arena", PROFILE, "minecraft:overworld", 0, 0, 0, 20, 20, 20).unlock();
        UUID first = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
        UUID second = UUID.fromString("11111111-2222-3333-4444-555555555555");

        ArenaRecord active = arena.activate(first, 100L).orElseThrow();

        assertFalse(active.activate(second, 101L).isPresent());
        assertTrue(active.release(first).activeEncounterId() == null);
    }

    @Test
    void revivalRequiresFirstDefeatUnlock() {
        ArenaRecord arena = ArenaRecord.create("arena", PROFILE, "minecraft:overworld", 0, 0, 0, 20, 20, 20);

        assertFalse(arena.activate(UUID.randomUUID(), 100L).isPresent());
    }

    @Test
    void staleLockCanRecoverAfterRestartTimeout() {
        UUID encounter = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
        ArenaRecord active = ArenaRecord.create("arena", PROFILE, "minecraft:overworld", 0, 0, 0, 20, 20, 20).unlock().activate(encounter, 100L).orElseThrow();

        assertTrue(active.releaseIfStale(1000L, 600L).activeEncounterId() == null);
        assertTrue(active.releaseIfStale(500L, 600L).activeEncounterId() != null);
    }
}
