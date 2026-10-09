package com.siirio.jemserver.claims;

import com.siirio.jemclaims.BossClaimZones;
import com.siirio.jemclaims.ClaimGroups;
import com.siirio.jemclaims.ClaimPermission;
import com.siirio.jemclaims.ClaimSelectionGuard;
import com.siirio.jemclaims.ClaimsConfig;
import com.siirio.jemclaims.ClaimsMenus;
import com.siirio.jemclaims.FlanBridge;
import com.siirio.jemclaims.JemClaims;
import com.siirio.jemclaims.compat.CreatePlacement;
import java.util.List;
import java.util.UUID;
import java.util.function.BiPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;

public final class JemClaimsBackend implements ClaimsBackend {
    public JemClaimsBackend() {}

    @Override public boolean enabled() { return true; }

    @Override
    public void configureReservation(BiPredicate<ServerPlayer, Claims.Area> reservation) {
        ClaimSelectionGuard.reservation((player, area) -> reservation.test(player, new Claims.Area(area.minX(), area.minZ(), area.maxX(), area.maxZ())));
    }

    @Override public void configurePlacement(Claims.PlacementObserver observer) { CreatePlacement.setObserver(observer::placing); }

    @Override public void openOverview(ServerPlayer player) { ClaimsMenus.overview(player, 0); }
    @Override public void openProfile(ServerPlayer player, UUID playerId) { ClaimsMenus.profile(player, playerId); }
    @Override public void confirmDelete(ServerPlayer player, UUID claimId) { ClaimsMenus.confirmDelete(player, claimId); }

    @Override
    public List<Claims.Territory> mapTerritories(ServerPlayer player) {
        return ClaimGroups.connected(FlanBridge.list(player.server).stream()
                        .filter(claim -> claim.owner() != null && FlanBridge.trusted(player, claim.id())).toList()).stream()
                .flatMap(group -> group.stream().map(claim -> territory(claim, group.get(0).color()))).toList();
    }

    @Override public List<Claims.Territory> territories(MinecraftServer server) { return FlanBridge.list(server).stream().map(claim -> territory(claim, claim.color())).toList(); }

    @Override
    public Claims.BoundaryData boundary(ServerPlayer player, int limit) {
        var stack = player.getMainHandItem();
        String toolName = ClaimsConfig.TOOL_NAME.get();
        boolean holding = stack.is(Items.STICK) && stack.hasCustomHoverName() && stack.getHoverName().getString().equals(toolName);
        List<Claims.Boundary> bounds = holding ? FlanBridge.nearby(player).stream().limit(limit)
                .map(claim -> new Claims.Boundary(claim.minX(), claim.minZ(), claim.maxX(), claim.maxZ(),
                        FlanBridge.owner(player, claim.id()) ? 0 : FlanBridge.trusted(player, claim.id()) ? 1 : 2,
                        claim.owner() == null ? claim.id() : claim.owner(), claim.name())).toList() : List.of();
        var selection = holding ? JemClaims.selection(player) : null;
        return new Claims.BoundaryData(toolName, ClaimsConfig.BORDER_RADIUS.get(), FlanBridge.budget(player) - FlanBridge.used(player), bounds,
                selection == null ? null : new Claims.Selection(selection.first(), selection.second()));
    }

    @Override public int boundaryTicks() { return ClaimsConfig.BORDER_TICKS.get(); }
    @Override public boolean available(ServerPlayer player, ChunkPos position) { return ClaimSelectionGuard.available(player, position); }
    @Override public boolean canStay(ServerPlayer player, ServerLevel level, BlockPos position) { return FlanBridge.can(player, level, position, ClaimPermission.CANSTAY); }
    @Override public boolean bossAreaAvailable(ServerPlayer player, BlockPos first, BlockPos second) { return BossClaimZones.get(player.server).validate(player, first, second, false); }

    private static Claims.Territory territory(FlanBridge.Territory claim, int color) {
        return new Claims.Territory(claim.id(), claim.owner(), claim.name(), claim.dimension(), claim.minX(), claim.minZ(), claim.maxX(), claim.maxZ(), color);
    }
}
