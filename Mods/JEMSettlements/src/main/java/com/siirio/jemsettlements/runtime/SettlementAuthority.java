package com.siirio.jemsettlements.runtime;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** Production is dedicated-only; the explicit standalone artifact also owns integrated gameplay. */
public final class SettlementAuthority {
    private SettlementAuthority() { }
    public static boolean active(MinecraftServer server) { return server != null && (server.isDedicatedServer() || com.siirio.jemsettlements.SettlementBuild.STANDALONE) && server.overworld() != null; }
    public static boolean active(ServerPlayer player) { return player != null && active(player.getServer()) && principal(player).isPresent(); }
    public static Optional<UUID> principal(ServerPlayer player) {
        if (player == null || player.getServer() == null) return Optional.empty();
        if (!player.getServer().isDedicatedServer()) return Optional.of(player.getUUID());
        return com.siirio.jemidentity.api.IdentityPolicy.resolve(player).map(value -> value.accountId());
    }
}
