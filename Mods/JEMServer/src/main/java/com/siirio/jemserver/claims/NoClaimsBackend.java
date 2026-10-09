package com.siirio.jemserver.claims;

import java.util.List;
import java.util.UUID;
import java.util.function.BiPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;

final class NoClaimsBackend implements ClaimsBackend {
    static final NoClaimsBackend INSTANCE = new NoClaimsBackend();

    private NoClaimsBackend() {}

    @Override public boolean enabled() { return false; }
    @Override public void configureReservation(BiPredicate<ServerPlayer, Claims.Area> reservation) {}
    @Override public void configurePlacement(Claims.PlacementObserver observer) {}
    @Override public void openOverview(ServerPlayer player) { unavailable(player); }
    @Override public void openProfile(ServerPlayer player, UUID playerId) { unavailable(player); }
    @Override public void confirmDelete(ServerPlayer player, UUID claimId) { unavailable(player); }
    @Override public List<Claims.Territory> mapTerritories(ServerPlayer player) { return List.of(); }
    @Override public List<Claims.Territory> territories(MinecraftServer server) { return List.of(); }
    @Override public Claims.BoundaryData boundary(ServerPlayer player, int limit) { return null; }
    @Override public int boundaryTicks() { return 20; }
    @Override public boolean available(ServerPlayer player, ChunkPos position) { return player.serverLevel().getChunkSource().getChunkNow(position.x, position.z) != null; }
    @Override public boolean canStay(ServerPlayer player, ServerLevel level, BlockPos position) { return true; }
    @Override public boolean bossAreaAvailable(ServerPlayer player, BlockPos first, BlockPos second) { return true; }

    private static void unavailable(ServerPlayer player) { player.sendSystemMessage(Component.literal("Территории отключены в этом мире.")); }
}
