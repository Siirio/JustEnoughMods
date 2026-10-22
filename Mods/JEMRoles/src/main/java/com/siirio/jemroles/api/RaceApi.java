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
        return RaceSavedData.get(server).find(accountId).filter(record -> !record.choiceRequired()).map(record -> snapshot(server, record));
    }

    public static RaceId effectiveRace(MinecraftServer server, UUID accountId) {
        return RaceSavedData.get(server).find(accountId).filter(record -> !record.choiceRequired()).map(RaceRecord::race).orElse(RaceId.ANCESTORS);
    }

    public static boolean hasRace(MinecraftServer server, UUID accountId, RaceId race) {
        return RaceSavedData.get(server).find(accountId).filter(record -> !record.choiceRequired()).map(RaceRecord::race).filter(race::equals).isPresent();
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

    private static RaceSnapshot snapshot(MinecraftServer server, RaceRecord record) {
        boolean test = com.siirio.jemroles.race.RaceService.testRaceReselect(server);
        long remaining = test ? 0L : record.remainingReselectTicks(server.overworld().getGameTime());
        int progress = record.uses(record.race().ordinal());
        int nativeTier = com.siirio.jemvisualsandbox.workbench.RacePowerStages.stageForUses(progress) + 1;
        return new RaceSnapshot(record.race(), true, record.activeTicks(), remaining == 0L, record.lastRespecAt(),
                RaceRecord.SCHEMA, nativeTier, progress, record.resonance(), record.waterExposure(),
                record.lavaExposure(), record.livingPath(), remaining, test);
    }
}
