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
import net.minecraftforge.fml.ModList;

public final class Claims {
    private static final ClaimsBackend BACKEND = load();
    public record Area(int minX, int minZ, int maxX, int maxZ) {}
    public record Territory(UUID id, UUID owner, String name, ResourceLocation dimension, int minX, int minZ, int maxX, int maxZ, int color) {}
    public record Boundary(int minX, int minZ, int maxX, int maxZ, int style, UUID owner, String name) {}
    public record Selection(BlockPos first, BlockPos second) {}
    public record BoundaryData(String toolName, int radius, int remaining, List<Boundary> bounds, Selection selection) {}
    @FunctionalInterface public interface PlacementObserver { void placing(ServerLevel level, BlockPos source, BlockPos target); }

    private Claims() {}

    private static ClaimsBackend load() {
        if (!ModList.get().isLoaded("jem_claims") || !ModList.get().isLoaded("flan")) return NoClaimsBackend.INSTANCE;
        try {
            return (ClaimsBackend) Class.forName(Claims.class.getPackageName() + ".JemClaimsBackend").getConstructor().newInstance();
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("Unable to load claims integration", failure);
        }
    }

    public static boolean enabled() { return BACKEND.enabled(); }
    public static void configureReservation(BiPredicate<ServerPlayer, Area> reservation) { BACKEND.configureReservation(reservation); }
    public static void configurePlacement(PlacementObserver observer) { BACKEND.configurePlacement(observer); }
    public static void openOverview(ServerPlayer player) { BACKEND.openOverview(player); }
    public static void openProfile(ServerPlayer player, UUID playerId) { BACKEND.openProfile(player, playerId); }
    public static void confirmDelete(ServerPlayer player, UUID claimId) { BACKEND.confirmDelete(player, claimId); }
    public static List<Territory> mapTerritories(ServerPlayer player) { return BACKEND.mapTerritories(player); }
    public static List<Territory> territories(MinecraftServer server) { return BACKEND.territories(server); }
    public static BoundaryData boundary(ServerPlayer player, int limit) { return BACKEND.boundary(player, limit); }
    public static int boundaryTicks() { return BACKEND.boundaryTicks(); }
    public static boolean available(ServerPlayer player, ChunkPos position) { return BACKEND.available(player, position); }
    public static boolean canStay(ServerPlayer player, ServerLevel level, BlockPos position) { return BACKEND.canStay(player, level, position); }
    public static boolean bossAreaAvailable(ServerPlayer player, BlockPos first, BlockPos second) { return BACKEND.bossAreaAvailable(player, first, second); }
}
