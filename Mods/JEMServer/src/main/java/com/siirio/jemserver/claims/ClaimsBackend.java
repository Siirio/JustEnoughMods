package com.siirio.jemserver.claims;

import java.util.List;
import java.util.UUID;
import java.util.function.BiPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;

interface ClaimsBackend {
    boolean enabled();
    void configureReservation(BiPredicate<ServerPlayer, Claims.Area> reservation);
    void configurePlacement(Claims.PlacementObserver observer);
    void openOverview(ServerPlayer player);
    void openProfile(ServerPlayer player, UUID playerId);
    void confirmDelete(ServerPlayer player, UUID claimId);
    List<Claims.Territory> mapTerritories(ServerPlayer player);
    List<Claims.Territory> territories(MinecraftServer server);
    Claims.BoundaryData boundary(ServerPlayer player, int limit);
    int boundaryTicks();
    boolean available(ServerPlayer player, ChunkPos position);
    boolean canStay(ServerPlayer player, ServerLevel level, BlockPos position);
    boolean bossAreaAvailable(ServerPlayer player, BlockPos first, BlockPos second);
}
