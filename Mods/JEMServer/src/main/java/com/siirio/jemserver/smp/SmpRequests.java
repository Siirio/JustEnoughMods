package com.siirio.jemserver.smp;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;

public final class SmpRequests {
    private static final Map<UUID, SmpActionSession> SESSIONS = new HashMap<>();

    static void handle(ServerPlayer player, SmpAction action) {
        var session = session(player);
        var previous = session.receipts.get(action.request());
        if (previous != null) {
            SmpSubscriptions.reply(player, action.request(), previous.action().equals(action) ? previous.error() : "request_reused", false);
            return;
        }
        if (!allow(player)) {
            SmpSubscriptions.reply(player, action.request(), "cooldown", false);
            return;
        }
        String error = "";
        try {
            SmpActions.handle(player, action);
        } catch (SmpActionFailure failure) {
            error = failure.code();
        } catch (RuntimeException failure) {
            error = "internal_error";
            com.mojang.logging.LogUtils.getLogger().error("SMP action failed: player={}, request={}, table={}, kind={}, record={}",
                    player.getUUID(), action.request(), action.table(), action.kind(), action.id(), failure);
        }
        session.remember(action, error);
        SmpSubscriptions.reply(player, action.request(), error, error.isEmpty() || error.equals("stale_revision"));
    }

    public static boolean allow(ServerPlayer player) {
        var session = session(player);
        long tick = player.server.getTickCount();
        if (session.actedAt != Long.MIN_VALUE && tick - session.actedAt < SmpProtocol.ACTION_TICKS) return false;
        session.actedAt = tick;
        return true;
    }

    static void revive(ServerPlayer player, UUID target) {
        var session = session(player);
        long tick = player.server.getTickCount();
        if (session.revivedAt != Long.MIN_VALUE && tick - session.revivedAt < SmpProtocol.REVIVE_TICKS) return;
        session.revivedAt = tick;
        com.siirio.jemworldbosstiers.api.HostedEncounterApi.revive(player, target);
    }

    static void logout(ServerPlayer player) { SESSIONS.remove(player.getUUID()); }
    static void clear() { SESSIONS.clear(); }
    private static SmpActionSession session(ServerPlayer player) {
        return SESSIONS.computeIfAbsent(player.getUUID(), ignored -> new SmpActionSession());
    }

    private SmpRequests() {}
}
