package com.siirio.jemserver.smp;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** Transient state of one running server, discarded as a unit on server stop. */
final class SmpServerSession {
    private static SmpServerSession current;
    private final MinecraftServer server;
    final Map<UUID, SmpActionSession> requests = new HashMap<>();
    final Map<UUID, SmpViewSession> views = new HashMap<>();
    final Map<UUID, CompoundTag> combat = new HashMap<>();

    private SmpServerSession(MinecraftServer server) { this.server = server; }

    static SmpServerSession get(MinecraftServer server) {
        if (current == null) current = new SmpServerSession(server);
        if (current.server != server) throw new IllegalStateException("SMP session belongs to another server");
        return current;
    }

    static void logout(ServerPlayer player) {
        if (current == null || current.server != player.server) return;
        UUID id = player.getUUID();
        current.requests.remove(id);
        current.views.remove(id);
        current.combat.remove(id);
    }

    static void stop(MinecraftServer server) {
        if (current != null && current.server == server) current = null;
    }
}
