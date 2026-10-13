package com.siirio.jemroles.api;

import com.siirio.jemroles.race.RaceId;

public record RaceSnapshot(RaceId race, boolean confirmed, long activeTicks,
                           boolean freeSwitchAvailable, long lastRespecEpochMillis, int schemaVersion) {
}
