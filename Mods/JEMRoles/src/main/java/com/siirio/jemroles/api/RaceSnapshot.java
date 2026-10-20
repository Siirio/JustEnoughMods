package com.siirio.jemroles.api;

import com.siirio.jemroles.race.RaceId;

public record RaceSnapshot(RaceId race, boolean confirmed, long activeTicks,
                           boolean freeSwitchAvailable, long lastRespecEpochMillis, int schemaVersion,
                           int spellTier, int spellProgress, int resonance, int waterExposure, int lavaExposure,
                           boolean livingPath, long remainingReselectTicks, boolean testProfile) {
    public double remainingReselectDays() { return remainingReselectTicks / 24_000.0D; }
}
