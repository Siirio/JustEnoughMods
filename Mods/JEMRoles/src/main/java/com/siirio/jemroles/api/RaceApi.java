package com.siirio.jemroles.api;

import com.siirio.jemroles.race.RaceId;
import com.siirio.jemroles.race.RaceRecord;
import com.siirio.jemroles.race.RaceSavedData;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;

public final class RaceApi {
    private RaceApi() {
    }

    public static Optional<RaceSnapshot> get(MinecraftServer server, UUID accountId) {
        return RaceSavedData.get(server).find(accountId).map(RaceApi::snapshot);
    }

    public static RaceId effectiveRace(MinecraftServer server, UUID accountId) {
        return RaceSavedData.get(server).find(accountId).map(RaceRecord::race).orElse(RaceId.HUMAN);
    }

    private static RaceSnapshot snapshot(RaceRecord record) {
        return new RaceSnapshot(record.race(), true, record.activeTicks(), record.freeSwitchAvailable(), record.lastRespecAt(), RaceRecord.SCHEMA);
    }
}
