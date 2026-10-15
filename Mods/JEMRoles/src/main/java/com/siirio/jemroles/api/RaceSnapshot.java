package com.siirio.jemroles.api;

import com.siirio.jemroles.race.RaceId;

public record RaceSnapshot(RaceId race, boolean confirmed, long activeTicks,
                           boolean freeSwitchAvailable, long lastRespecEpochMillis, int schemaVersion,
                           int spellTier, int spellProgress, int resonance, int waterExposure, int lavaExposure,
                           boolean livingPath) {
}
