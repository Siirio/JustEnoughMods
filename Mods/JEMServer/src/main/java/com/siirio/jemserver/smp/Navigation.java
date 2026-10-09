package com.siirio.jemserver.smp;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.*;

public final class Navigation {
    private static final int MAX_POINTS = 64;
    private static final Map<UUID, LinkedHashMap<String, NavigationPoint>> ACTIVE = new HashMap<>();
    private static final SimpleChannel CHANNEL =
            NetworkRegistry.newSimpleChannel(
                    new ResourceLocation("jem_server", "navigation"),
                    () -> "2",
                    "2"::equals,
                    "2"::equals);

    public static void register() {
        CHANNEL.messageBuilder(NavigationPoints.class, 0, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(
                        (p, b) ->
                                b.writeCollection(
                                        p.entries(),
                                        (o, w) -> {
                                            o.writeUtf(w.subsystem(), 16);
                                            o.writeUUID(w.id());
                                            o.writeUtf(w.label(), 80);
                                            o.writeResourceLocation(w.dimension());
                                            o.writeBlockPos(w.position());
                                            o.writeLong(w.expires());
                                        }))
                .decoder(
                        b ->
                                new NavigationPoints(
                                        b.readCollection(
                                                FriendlyByteBuf.limitValue(
                                                        ArrayList::new, MAX_POINTS),
                                                i ->
                                                        new NavigationPoint(
                                                                i.readUtf(16),
                                                                i.readUUID(),
                                                                i.readUtf(80),
                                                                i.readResourceLocation(),
                                                                i.readBlockPos(),
                                                                i.readLong()))))
                .consumerMainThread(
                        (p, c) -> {
                            DistExecutor.unsafeRunWhenOn(
                                    Dist.CLIENT,
                                    () ->
                                            () ->
                                                    com.siirio.jemserver.client.smp.SmpClient
                                                            .navigation(p));
                            c.get().setPacketHandled(true);
                        })
                .add();
    }

    public static void send(
            ServerPlayer player,
            String subsystem,
            UUID id,
            String label,
            ResourceLocation dimension,
            BlockPos pos,
            long expires) {
        var entries = ACTIVE.computeIfAbsent(player.getUUID(), key -> new LinkedHashMap<>());
        entries.put(
                subsystem + ":" + id,
                new NavigationPoint(
                        subsystem,
                        id,
                        label.length() > 80 ? label.substring(0, 80) : label,
                        dimension,
                        pos,
                        expires));
        while (entries.size() > MAX_POINTS) entries.remove(entries.keySet().iterator().next());
        sync(player, entries);
        player.sendSystemMessage(
                net.minecraft.network.chat.Component.literal(
                        label
                                + ": "
                                + pos.getX()
                                + ", "
                                + pos.getY()
                                + ", "
                                + pos.getZ()
                                + " · "
                                + dimension));
    }

    public static void accepted(ServerPlayer player) {
        var entries = ACTIVE.computeIfAbsent(player.getUUID(), key -> new LinkedHashMap<>());
        boolean changed = false;
        for (String table : List.of("parties"))
            for (var row : SmpData.get(player.server).all(table)) {
                if (SmpData.closed(row) || !row.contains("dimension")) continue;
                boolean authorized =
                        row.getUUID("owner").equals(player.getUUID())
                                || Parties.accepted(row, player.getUUID());
                String key = table + ":" + row.getUUID("id");
                if (authorized && !entries.containsKey(key) && entries.size() < MAX_POINTS) {
                    String label = row.getString("title");
                    entries.put(
                            key,
                            new NavigationPoint(
                                    table,
                                    row.getUUID("id"),
                                    label.length() > 80 ? label.substring(0, 80) : label,
                                    new ResourceLocation(row.getString("dimension")),
                                    BlockPos.of(row.getLong("position")),
                                    Long.MAX_VALUE));
                    changed = true;
                }
            }
        if (changed) sync(player, entries);
    }

    public static void tick(net.minecraft.server.MinecraftServer server) {
        if (server.getTickCount() % 20 != 0) return;
        long now = System.currentTimeMillis();
        ACTIVE.forEach(
                (id, entries) -> {
                    boolean changed =
                            entries.values()
                                    .removeIf(
                                            point -> {
                                                if (point.expires() <= now) return true;

                                                var row =
                                                        SmpData.get(server)
                                                                .find(
                                                                        point.subsystem(),
                                                                        point.id());
                                                if (row == null || SmpData.closed(row)) return true;
                                                return point.subsystem().equals("parties")
                                                        && !Parties.accepted(row, id);
                                            });
                    if (changed) {
                        var player = server.getPlayerList().getPlayer(id);
                        if (player != null) sync(player, entries);
                    }
                });
    }

    private static void sync(ServerPlayer player, Map<String, NavigationPoint> entries) {
        if (CHANNEL.isRemotePresent(player.connection.connection))
            CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> player),
                    new NavigationPoints(List.copyOf(entries.values())));
    }

    public static void logout(ServerPlayer player) {
        ACTIVE.remove(player.getUUID());
    }

    public static void clear() {
        ACTIVE.clear();
    }

    private Navigation() {}
}
