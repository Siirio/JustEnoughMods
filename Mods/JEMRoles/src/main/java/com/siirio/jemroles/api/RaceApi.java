package com.siirio.jemroles.api;

import com.siirio.jemroles.race.RaceId;
import com.siirio.jemroles.race.RaceRecord;
import com.siirio.jemroles.race.RaceSavedData;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import com.siirio.jemidentity.api.IdentityPolicy;

public final class RaceApi {
    private RaceApi() {
    }

    public static Optional<RaceSnapshot> get(MinecraftServer server, UUID accountId) {
        return RaceSavedData.get(server).find(accountId).map(RaceApi::snapshot);
    }

    public static RaceId effectiveRace(MinecraftServer server, UUID accountId) {
        return RaceSavedData.get(server).find(accountId).map(RaceRecord::race).orElse(RaceId.ANCESTORS);
    }

    public static boolean hasRace(MinecraftServer server, UUID accountId, RaceId race) {
        return RaceSavedData.get(server).find(accountId).map(RaceRecord::race).filter(race::equals).isPresent();
    }

    public static boolean hasRace(ServerPlayer player, RaceId race) {
        return IdentityPolicy.resolve(player).flatMap(principal -> get(player.getServer(), principal.accountId()))
                .map(RaceSnapshot::race).filter(race::equals).isPresent();
    }

    public static Optional<RaceSnapshot> get(ServerPlayer player) {
        return IdentityPolicy.resolve(player).flatMap(principal -> get(player.getServer(), principal.accountId()));
    }

    public static void openScreen(ServerPlayer player) {
        com.siirio.jemroles.network.RolesNetwork.sendStatus(player, true);
    }

    private static RaceSnapshot snapshot(RaceRecord record) {
        return new RaceSnapshot(record.race(), true, record.activeTicks(), record.freeSwitchAvailable(), record.lastRespecAt(),
                RaceRecord.SCHEMA, record.spellTier(), record.spellProgress(), record.resonance(), record.waterExposure(),
                record.lavaExposure(), record.livingPath());
    }
}
