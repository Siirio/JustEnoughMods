package com.siirio.jemserver.smp;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

final class SmpSubscriptions {
    private static final Map<UUID, SmpViewSession> VIEWS = new HashMap<>();

    static void query(ServerPlayer player, SmpQuery query) {
        if (query.tab().isEmpty()) { close(player); return; }
        try { SmpTable.fromKey(query.tab()); } catch (IllegalArgumentException invalid) { return; }
        if (query.page() < 0 || query.page() > SmpProtocol.MAX_PAGE) return;
        VIEWS.computeIfAbsent(player.getUUID(), ignored -> new SmpViewSession(query)).select(query);
    }

    static void open(ServerPlayer player, SmpQuery query) {
        LegacyRewards.migrate(player);
        query(player, query);
        var session = VIEWS.get(player.getUUID());
        if (session != null) publish(player, session, true, null, "");
    }

    static void close(ServerPlayer player) { VIEWS.remove(player.getUUID()); }
    static void clear() { VIEWS.clear(); }

    static void shopRemoved(MinecraftServer server, UUID shopId) {
        VIEWS.forEach((id, session) -> {
            var query = session.query;
            if (!query.tab().equals("shops")) return;
            if (shopId.equals(query.selected())) session.select(new SmpQuery(query.tab(), query.filter(), query.search(), query.page(), null));
            var player = server.getPlayerList().getPlayer(id);
            if (player != null) publish(player, session, false, null, "");
        });
    }

    static void tick(MinecraftServer server) {
        long tick = server.getTickCount();
        boolean refresh = tick % SmpConfig.REFRESH_TICKS.get() == 0;
        VIEWS.forEach((id, session) -> {
            boolean requested = session.sent == null;
            if ((!requested && !refresh) || session.builtAt != Long.MIN_VALUE && tick - session.builtAt < SmpProtocol.QUERY_TICKS) return;
            var player = server.getPlayerList().getPlayer(id);
            if (player == null) return;
            var revision = SmpViewRevision.current(player);
            boolean stable = session.query.tab().equals("prizes") || session.query.tab().equals("events") && session.query.selected() == null;
            if (!requested && stable && revision.equals(session.owners)) return;
            var next = build(player, session);
            if (!next.equals(session.sent)) SmpNetwork.send(player, session.update(next, false, null, ""));
        });
    }

    static void reply(ServerPlayer player, UUID request, String error, boolean refresh) {
        var session = VIEWS.get(player.getUUID());
        if (session != null && refresh) publish(player, session, false, request, error);
        else SmpNetwork.send(player, new SmpUpdate(new CompoundTag(), false, request, error));
    }

    private static CompoundTag build(ServerPlayer player, SmpViewSession session) {
        var next = SmpViews.build(player, session.query);
        session.owners = SmpViewRevision.current(player);
        session.builtAt = player.server.getTickCount();
        return next;
    }

    private static void publish(ServerPlayer player, SmpViewSession session, boolean open, UUID request, String error) {
        SmpNetwork.send(player, session.update(build(player, session), open, request, error));
    }

    private SmpSubscriptions() {}
}
