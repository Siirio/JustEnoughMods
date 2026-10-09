package com.siirio.jemserver.smp;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.*;

public final class SmpNetwork {
    private static final String VERSION = "2";
    private static final int MAX_ACTION_BYTES = 4096;
    private static final SimpleChannel CHANNEL =
            NetworkRegistry.ChannelBuilder.named(new ResourceLocation("jem_server", "smp_v2"))
                    .networkProtocolVersion(() -> VERSION)
                    .clientAcceptedVersions(VERSION::equals)
                    .serverAcceptedVersions(VERSION::equals)
                    .simpleChannel();

    public record Query(String tab, String filter, String search, int page, UUID selected) {}

    public record Action(
            UUID request, String table, UUID id, int revision, String action, String json) {}

    private record Revive(UUID target) {}

    public record Update(CompoundTag view, boolean open, UUID request, String error) {}

    private static final Map<UUID, Query> VIEWS = new HashMap<>();
    private static final Map<UUID, CompoundTag> SENT = new HashMap<>();
    private record ViewRevision(long data, long rewards) {}
    private static final Map<UUID, ViewRevision> REVISIONS = new HashMap<>();
    private static final Map<UUID, LinkedHashMap<UUID, String>> RECEIPTS = new HashMap<>();
    private static final Map<UUID, Long> LAST_ACTION = new HashMap<>();
    private static final Set<String> TABS =
            Set.of("parties", "shops", "events", "profiles", "prizes");

    public static void register() {
        CHANNEL.messageBuilder(Query.class, 0, NetworkDirection.PLAY_TO_SERVER)
                .encoder(
                        (q, b) -> {
                            b.writeUtf(q.tab(), 16);
                            b.writeUtf(q.filter(), 64);
                            b.writeUtf(q.search(), 160);
                            b.writeVarInt(q.page());
                            b.writeNullable(q.selected(), FriendlyByteBuf::writeUUID);
                        })
                .decoder(
                        b ->
                                new Query(
                                        b.readUtf(16),
                                        b.readUtf(64),
                                        b.readUtf(160),
                                        b.readVarInt(),
                                        b.readNullable(FriendlyByteBuf::readUUID)))
                .consumerMainThread(
                        (q, c) -> {
                            var p = c.get().getSender();
                            if (p != null && SmpEnvironment.active(p)) {
                                if (q.tab().isEmpty()) close(p);
                                else if (TABS.contains(q.tab())
                                        && q.page() >= 0
                                        && q.page() <= 10000) {
                                    if (!q.equals(VIEWS.put(p.getUUID(), q))) {
                                        SENT.remove(p.getUUID());
                                        REVISIONS.remove(p.getUUID());
                                    }
                                }
                            }
                            c.get().setPacketHandled(true);
                        })
                .add();
        CHANNEL.messageBuilder(Action.class, 1, NetworkDirection.PLAY_TO_SERVER)
                .encoder(
                        (a, b) -> {
                            b.writeUUID(a.request());
                            b.writeUtf(a.table(), 16);
                            b.writeNullable(a.id(), FriendlyByteBuf::writeUUID);
                            b.writeVarInt(a.revision());
                            b.writeUtf(a.action(), 32);
                            b.writeUtf(a.json(), MAX_ACTION_BYTES);
                        })
                .decoder(
                        b ->
                                new Action(
                                        b.readUUID(),
                                        b.readUtf(16),
                                        b.readNullable(FriendlyByteBuf::readUUID),
                                        b.readVarInt(),
                                        b.readUtf(32),
                                        b.readUtf(MAX_ACTION_BYTES)))
                .consumerMainThread(
                        (a, c) -> {
                            var p = c.get().getSender();
                            if (p != null && SmpEnvironment.active(p)) handle(p, a);
                            c.get().setPacketHandled(true);
                        })
                .add();
        CHANNEL.messageBuilder(Update.class, 2, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(
                        (u, b) -> {
                            b.writeNbt(u.view());
                            b.writeBoolean(u.open());
                            b.writeNullable(u.request(), FriendlyByteBuf::writeUUID);
                            b.writeUtf(u.error(), 128);
                        })
                .decoder(
                        b ->
                                new Update(
                                        b.readNbt(),
                                        b.readBoolean(),
                                        b.readNullable(FriendlyByteBuf::readUUID),
                                        b.readUtf(128)))
                .consumerMainThread(
                        (u, c) -> {
                            DistExecutor.unsafeRunWhenOn(
                                    Dist.CLIENT,
                                    () ->
                                            () ->
                                                    com.siirio.jemserver.client.smp.SmpClient
                                                            .accept(u));
                            c.get().setPacketHandled(true);
                        })
                .add();
        CHANNEL.messageBuilder(Revive.class, 3, NetworkDirection.PLAY_TO_SERVER)
                .encoder((p, b) -> b.writeUUID(p.target()))
                .decoder(b -> new Revive(b.readUUID()))
                .consumerMainThread(
                        (p, c) -> {
                            var player = c.get().getSender();
                            if (player != null && SmpEnvironment.active(player))
                                com.siirio.jemworldbosstiers.api.HostedEncounterApi.revive(
                                        player, p.target());
                            c.get().setPacketHandled(true);
                        })
                .add();
    }

    public static void revive(UUID target) {
        CHANNEL.sendToServer(new Revive(target));
    }

    public static void query(Query query) {
        CHANNEL.sendToServer(query);
    }

    public static void action(Action action) {
        CHANNEL.sendToServer(action);
    }

    public static void open(ServerPlayer player, String tab, UUID selected) {
        open(player, new Query(tab, "", "", 0, selected));
    }

    public static void open(ServerPlayer player, Query query) {
        if (!SmpEnvironment.active(player)) return;
        LegacyRewards.migrate(player);
        VIEWS.put(player.getUUID(), query);
        var view = SmpViews.build(player, query);
        view.putString("querySearch", query.search());
        send(player, new Update(view, true, null, ""));
    }

    public static void close(ServerPlayer player) {
        VIEWS.remove(player.getUUID());
        SENT.remove(player.getUUID());
        REVISIONS.remove(player.getUUID());
    }

    public static void logout(ServerPlayer player) {
        close(player);
        RECEIPTS.remove(player.getUUID());
        LAST_ACTION.remove(player.getUUID());
    }

    public static void clear() {
        VIEWS.clear();
        SENT.clear();
        REVISIONS.clear();
        RECEIPTS.clear();
        LAST_ACTION.clear();
    }

    public static void shopRemoved(net.minecraft.server.MinecraftServer server, UUID shopId) {
        var viewers =
                VIEWS.entrySet().stream()
                        .filter(entry -> entry.getValue().tab().equals("shops"))
                        .map(Map.Entry::getKey)
                        .toList();
        for (UUID viewerId : viewers) {
            var query = VIEWS.get(viewerId);
            if (shopId.equals(query.selected())) {
                query =
                        new Query(
                                query.tab(),
                                query.filter(),
                                query.search(),
                                query.page(),
                                null);
                VIEWS.put(viewerId, query);
            }
            var player = server.getPlayerList().getPlayer(viewerId);
            if (player == null) continue;
            var view = SmpViews.build(player, query);
            view.putString("querySearch", query.search());
            send(player, new Update(view, false, null, ""));
        }
    }

    public static void tick(net.minecraft.server.MinecraftServer server) {
        if (VIEWS.isEmpty()) return;
        boolean refresh = server.getTickCount() % SmpConfig.REFRESH_TICKS.get() == 0;
        VIEWS.forEach(
                (id, query) -> {
                    boolean requested = !SENT.containsKey(id);
                    if (!requested && !refresh) return;
                    var player = server.getPlayerList().getPlayer(id);
                    if (player == null) return;
                    var revision = revision(player);
                    boolean stable = query.tab().equals("prizes") || query.tab().equals("events") && query.selected() == null;
                    if (!requested && stable && revision.equals(REVISIONS.get(id))) return;
                    var next = SmpViews.build(player, query);
                    REVISIONS.put(id, revision(player));
                    if (!next.equals(SENT.get(id))) {
                        var previous = SENT.put(id, next.copy());
                        send(
                                player,
                                new Update(
                                        previous == null ? next : ViewDelta.between(previous, next),
                                        false,
                                        null,
                                        ""));
                    }
                });
    }

    private static void handle(ServerPlayer player, Action action) {
        var receipts = RECEIPTS.computeIfAbsent(player.getUUID(), id -> new LinkedHashMap<>());
        if (receipts.containsKey(action.request())) {
            reply(player, action.request(), receipts.get(action.request()));
            return;
        }
        long tick = player.server.getTickCount();
        if (tick - LAST_ACTION.getOrDefault(player.getUUID(), -100L) < 3) {
            reply(player, action.request(), "cooldown");
            return;
        }
        LAST_ACTION.put(player.getUUID(), tick);
        String error = "";
        try {
            SmpActions.handle(player, action);
        } catch (IllegalArgumentException
                | IllegalStateException
                | com.google.gson.JsonParseException
                | java.time.DateTimeException failure) {
            error = failure.getMessage();
            if (error == null || error.length() > 80 || !error.matches("[a-z_]+"))
                error = "invalid_request";
        }
        receipts.put(action.request(), error);
        while (receipts.size() > 64) receipts.remove(receipts.keySet().iterator().next());
        reply(player, action.request(), error);
    }

    private static void reply(ServerPlayer player, UUID request, String error) {
        var query = VIEWS.get(player.getUUID());
        send(
                player,
                new Update(
                        query == null ? new CompoundTag() : SmpViews.build(player, query),
                        false,
                        request,
                        error));
    }

    private static void send(ServerPlayer player, Update update) {
        if (update.view() != null
                && !update.view().isEmpty()
                && !update.view().getBoolean("__delta")) {
            SENT.put(player.getUUID(), update.view().copy());
            REVISIONS.put(player.getUUID(), revision(player));
        }
        if (CHANNEL.isRemotePresent(player.connection.connection))
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), update);
    }

    private static ViewRevision revision(ServerPlayer player) {
        return new ViewRevision(SmpData.get(player.server).revision(),
                com.siirio.jemworldbosstiers.encounter.HostedRewards.get(player.server).revision());
    }

    private SmpNetwork() {}
}
