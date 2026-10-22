package com.siirio.jemserver.smp;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

final class SmpSubscriptions {

    static void query(ServerPlayer player, SmpQuery query) {
        if (!SmpEnvironment.active(player)) return;
        if (query.tab().isEmpty()) { close(player); return; }
        if (!SmpTable.viewable(query.tab())) return;
        if (query.page() < 0 || query.page() > SmpProtocol.MAX_PAGE) return;
        var playerSession = SmpServerSession.get(player.server).player(player.getUUID());
        if (playerSession.view == null) playerSession.view = new SmpViewSession(query);
        playerSession.view.select(query);
    }

    static void open(ServerPlayer player, SmpQuery query) {
        if (!SmpEnvironment.active(player) || !SmpTable.viewable(query.tab())) return;
        LegacyRewards.migrate(player);
        query(player, query);
        var session = SmpServerSession.get(player.server).player(player.getUUID()).view;
        if (session != null) publish(player, session, true, null, "");
    }

    static void close(ServerPlayer player) { SmpServerSession.get(player.server).player(player.getUUID()).view = null; }

    static void shopRemoved(MinecraftServer server, UUID shopId) {
        if (!SmpEnvironment.active(server)) return;
        SmpServerSession.get(server).views((id, session) -> {
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
        SmpServerSession.get(server).views((id, session) -> {
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
        var session = SmpServerSession.get(player.server).player(player.getUUID()).view;
        if (session != null && refresh) publish(player, session, false, request, error);
        else SmpNetwork.send(player, new SmpUpdate(new CompoundTag(), false, request, error));
    }

    private static CompoundTag build(ServerPlayer player, SmpViewSession session) {
        if (session.query.tab().equals("shops")) {
            com.siirio.jemserver.smp.shops.ShopIndex.refreshLoaded(player);
        }
        var next = SmpViews.build(player, session.query);
        session.owners = SmpViewRevision.current(player);
        session.builtAt = player.server.getTickCount();
        return next;
    }

    private static void publish(ServerPlayer player, SmpViewSession session, boolean open, UUID request, String error) {
        SmpData.get(player.server).afterEdit(() -> SmpNetwork.send(player, session.update(build(player, session), open, request, error)));
    }

    private SmpSubscriptions() {}
}
