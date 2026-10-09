package com.siirio.jemworldbosstiers.progression;

import com.siirio.jemworldbosstiers.encounter.EncounterData;

public final class BossDefeatPolicy {
    private BossDefeatPolicy() {
    }

    public static boolean canProgress(EncounterData encounter, boolean genericKill) {
        if (encounter.provenance() == com.siirio.jemworldbosstiers.encounter.EncounterProvenance.RAID_EVENT) {
            return false;
        }
        return encounter.canProgressUnique()
                || genericKill && encounter.progressionEligible() && !encounter.rematch();
    }
}
