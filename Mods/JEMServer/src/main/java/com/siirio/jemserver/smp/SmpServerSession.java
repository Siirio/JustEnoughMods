package com.siirio.jemserver.smp;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** Transient state of one running server, discarded as a unit on server stop. */
final class SmpServerSession {
    static final class PlayerSession {
        final SmpActionSession actions = new SmpActionSession();
        SmpViewSession view;
        CompoundTag combat;
    }

    private static SmpServerSession current;
    private final MinecraftServer server;
    private final Map<UUID, PlayerSession> players = new HashMap<>();

    private SmpServerSession(MinecraftServer server) { this.server = server; }

    static void start(MinecraftServer server) {
        requireServerThread(server);
        if (current != null) throw new IllegalStateException("SMP session already started");
        current = new SmpServerSession(server);
    }

    static SmpServerSession get(MinecraftServer server) {
        requireServerThread(server);
        if (current == null || current.server != server)
            throw new IllegalStateException("No SMP session for this server");
        return current;
    }

    PlayerSession player(UUID id) {
        requireServerThread(server);
        return players.computeIfAbsent(id, ignored -> new PlayerSession());
    }

    void views(BiConsumer<UUID, SmpViewSession> action) {
        requireServerThread(server);
        players.forEach((id, player) -> { if (player.view != null) action.accept(id, player.view); });
    }

    List<UUID> combatPlayers() {
        requireServerThread(server);
        return players.entrySet().stream().filter(entry -> entry.getValue().combat != null)
                .map(Map.Entry::getKey).toList();
    }

    static void logout(ServerPlayer player) {
        requireServerThread(player.server);
        if (current != null && current.server == player.server) current.players.remove(player.getUUID());
    }

    static void stop(MinecraftServer server) {
        requireServerThread(server);
        if (current != null && current.server == server) {
            current.players.clear();
            current = null;
        }
    }

    private static void requireServerThread(MinecraftServer server) {
        if (!server.isSameThread()) throw new IllegalStateException("SMP session accessed off the server thread");
    }
}
