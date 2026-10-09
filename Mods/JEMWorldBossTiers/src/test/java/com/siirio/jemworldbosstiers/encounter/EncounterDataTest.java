package com.siirio.jemworldbosstiers.encounter;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EncounterDataTest {
    @Test
    void raidCannotBePromotedByNativeVerificationOrCommandKill() {
        CompoundTag tag = new CompoundTag();
        EncounterData.create(tag, PROFILE, 3, EncounterProvenance.RAID_EVENT, true, false, null);
        EncounterData.addParticipant(tag, UUID.randomUUID());
        EncounterData verified = EncounterData.verify(tag, EncounterProvenance.NATIVE, "arena");
        assertEquals(EncounterProvenance.RAID_EVENT, verified.provenance());
        assertFalse(verified.canProgressUnique());
        assertFalse(com.siirio.jemworldbosstiers.progression.BossDefeatPolicy.canProgress(verified, true));
    }
    @Test
    void unverifiedSnapshotCanBecomeRevivalWithoutChangingItsTier() {
        CompoundTag tag = new CompoundTag();
        ResourceLocation profile = ResourceLocation.fromNamespaceAndPath("test", "boss");
        EncounterData.create(tag, profile, 4, EncounterProvenance.UNVERIFIED, false, false, null);

        EncounterData classified = EncounterData.classify(tag, EncounterProvenance.REVIVAL, false, true, "arena");

        assertEquals(4, classified.tier());
        assertEquals(EncounterProvenance.REVIVAL, classified.provenance());
        assertTrue(classified.rematch());
        assertFalse(classified.canProgressUnique());
        assertEquals("arena", classified.arenaId());
    }

    private static final ResourceLocation PROFILE = ResourceLocation.fromNamespaceAndPath("test", "boss");

    @Test
    void existingEncounterRetainsItsOriginalTier() {
        CompoundTag entityData = new CompoundTag();

        EncounterData.create(entityData, PROFILE, 2, EncounterProvenance.NATIVE, true, false, null);
        EncounterData.create(entityData, PROFILE, 4, EncounterProvenance.NATIVE, true, false, null);

        assertEquals(2, EncounterData.read(entityData).orElseThrow().tier());
    }

    @Test
    void rematchCanNeverCountTowardUniqueProgression() {
        CompoundTag entityData = new CompoundTag();
        EncounterData.create(entityData, PROFILE, 4, EncounterProvenance.REVIVAL, true, true, "arena-1");
        EncounterData encounter = EncounterData.read(entityData).orElseThrow();

        assertTrue(encounter.rematch());
        assertFalse(encounter.canProgressUnique());
    }

    @Test
    void participantSurvivesNbtRoundTrip() {
        CompoundTag entityData = new CompoundTag();
        UUID playerId = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
        EncounterData.create(entityData, PROFILE, 3, EncounterProvenance.STRUCTURE, true, false, "arena-2");

        EncounterData.addParticipant(entityData, playerId);

        assertTrue(EncounterData.read(entityData).orElseThrow().participants().contains(playerId));
    }

    @Test
    void nativeVerificationDoesNotResnapshotAnExistingEncounter() {
        CompoundTag entityData = new CompoundTag();
        EncounterData.create(entityData, PROFILE, 2, EncounterProvenance.UNVERIFIED, false, false, null);

        EncounterData verified = EncounterData.verify(entityData, EncounterProvenance.STRUCTURE, "arena-3");

        assertEquals(2, verified.tier());
        assertTrue(verified.progressionEligible());
        assertEquals("arena-3", verified.arenaId());
    }
}
