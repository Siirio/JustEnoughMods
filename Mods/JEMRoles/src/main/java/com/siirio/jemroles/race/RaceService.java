package com.siirio.jemroles.race;

import com.siirio.jemidentity.api.IdentityPolicy;
import com.siirio.jemroles.api.RaceSnapshot;
import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;

public final class RaceService {
    public enum Result { CHOSEN, CHANGED, UNAUTHENTICATED, CHANGE_LOCKED, UNCHANGED }

    private RaceService() {
    }

    public static Result choose(ServerPlayer player, RaceId next) {
        var principal = IdentityPolicy.resolve(player);
        if (principal.isEmpty()) return Result.UNAUTHENTICATED;
        RaceSavedData data = RaceSavedData.get(player.getServer());
        Optional<RaceRecord> existing = data.find(principal.get().accountId());
        if (existing.isEmpty()) {
            data.put(principal.get().accountId(), new RaceRecord(next, System.currentTimeMillis()));
            return Result.CHOSEN;
        }
        RaceRecord record = existing.get();
        if (record.race() == next) return Result.UNCHANGED;
        if (!record.freeSwitchAvailable()) return Result.CHANGE_LOCKED;
        record.change(next, true);
        data.changed();
        return Result.CHANGED;
    }

    public static Optional<RaceSnapshot> snapshot(ServerPlayer player) {
        return IdentityPolicy.resolve(player).flatMap(principal -> com.siirio.jemroles.api.RaceApi.get(player.getServer(), principal.accountId()));
    }
}
