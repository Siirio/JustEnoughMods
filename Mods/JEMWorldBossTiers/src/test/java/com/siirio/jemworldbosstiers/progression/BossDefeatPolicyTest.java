package com.siirio.jemworldbosstiers.progression;

import com.siirio.jemworldbosstiers.encounter.EncounterData;
import com.siirio.jemworldbosstiers.encounter.EncounterProvenance;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BossDefeatPolicyTest {
    private static final ResourceLocation PROFILE = ResourceLocation.fromNamespaceAndPath("test", "boss");

    @Test
    void genericKillCanCompleteAValidatedFirstEncounterWithoutPriorDamage() {
        EncounterData encounter = new EncounterData(PROFILE, 1, EncounterProvenance.NATIVE, true, false, null, Set.of());

        assertTrue(BossDefeatPolicy.canProgress(encounter, true));
        assertFalse(BossDefeatPolicy.canProgress(encounter, false));
    }

    @Test
    void genericKillCannotBypassProvenanceOrRematchRules() {
        EncounterData unverified = new EncounterData(PROFILE, 1, EncounterProvenance.UNVERIFIED, false, false, null, Set.of());
        EncounterData rematch = new EncounterData(PROFILE, 2, EncounterProvenance.REVIVAL, false, true, null, Set.of());

        assertFalse(BossDefeatPolicy.canProgress(unverified, true));
        assertFalse(BossDefeatPolicy.canProgress(rematch, true));
    }
}
