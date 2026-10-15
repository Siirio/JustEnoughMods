package com.siirio.jemroles.race;

import com.siirio.jemidentity.api.IdentityPolicy;
import com.siirio.jemroles.api.RaceSnapshot;
import java.util.Optional;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import com.siirio.jemroles.api.RaceChangedEvent;
import com.siirio.jemroles.api.RaceChosenEvent;

public final class RaceService {
    public enum Result { CHOSEN, CHANGED, UNAUTHENTICATED, CHANGE_LOCKED, UNCHANGED }
    private static final Map<UUID, UUID> PENDING_NONCES = new ConcurrentHashMap<>();

    private RaceService() {
    }

    public static Result choose(ServerPlayer player, RaceId next) {
        return choose(player, next, null);
    }

    public static Result choose(ServerPlayer player, RaceId next, UUID nonce) {
        var principal = IdentityPolicy.resolve(player);
        if (principal.isEmpty()) return Result.UNAUTHENTICATED;
        if (nonce != null && !nonce.equals(PENDING_NONCES.remove(player.getUUID()))) return Result.CHANGE_LOCKED;
        RaceSavedData data = RaceSavedData.get(player.getServer());
        Optional<RaceRecord> existing = data.find(principal.get().accountId());
        if (existing.isEmpty()) {
            data.put(principal.get().accountId(), new RaceRecord(next, System.currentTimeMillis()));
            MinecraftForge.EVENT_BUS.post(new RaceChosenEvent(player, next));
            return Result.CHOSEN;
        }
        RaceRecord record = existing.get();
        if (record.race() == next) return Result.UNCHANGED;
        if (!record.freeSwitchAvailable()) return Result.CHANGE_LOCKED;
        record.change(next, true);
        data.changed();
        MinecraftForge.EVENT_BUS.post(new RaceChangedEvent(player, next));
        return Result.CHANGED;
    }

    public static UUID issueNonce(ServerPlayer player) {
        UUID nonce = UUID.randomUUID();
        PENDING_NONCES.put(player.getUUID(), nonce);
        return nonce;
    }

    public static UUID currentNonce(ServerPlayer player) {
        return PENDING_NONCES.computeIfAbsent(player.getUUID(), ignored -> UUID.randomUUID());
    }

    public static void clearNonce(ServerPlayer player) {
        PENDING_NONCES.remove(player.getUUID());
    }

    public static Result administrativeSet(ServerPlayer player, RaceId next) {
        var principal = IdentityPolicy.resolve(player);
        if (principal.isEmpty()) return Result.UNAUTHENTICATED;
        RaceSavedData data = RaceSavedData.get(player.getServer());
        RaceRecord record = data.find(principal.get().accountId()).orElse(null);
        if (record == null) {
            data.put(principal.get().accountId(), new RaceRecord(next, System.currentTimeMillis()));
            MinecraftForge.EVENT_BUS.post(new RaceChosenEvent(player, next));
            return Result.CHOSEN;
        }
        if (record.race() == next) return Result.UNCHANGED;
        record.change(next, false);
        data.changed();
        MinecraftForge.EVENT_BUS.post(new RaceChangedEvent(player, next));
        return Result.CHANGED;
    }

    public static Optional<RaceSnapshot> snapshot(ServerPlayer player) {
        return IdentityPolicy.resolve(player).flatMap(principal -> com.siirio.jemroles.api.RaceApi.get(player.getServer(), principal.accountId()));
    }

    public static Optional<RaceRecord> record(ServerPlayer player) {
        return IdentityPolicy.resolve(player).flatMap(principal -> RaceSavedData.get(player.getServer()).find(principal.accountId()));
    }

    public static boolean mutate(ServerPlayer player, Consumer<RaceRecord> change) {
        var record = record(player);
        if (record.isEmpty()) return false;
        change.accept(record.get());
        RaceSavedData.get(player.getServer()).changed();
        return true;
    }
}
