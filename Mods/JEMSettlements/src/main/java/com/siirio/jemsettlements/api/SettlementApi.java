package com.siirio.jemsettlements.api;

import com.siirio.jemidentity.api.IdentityPolicy;
import com.siirio.jemidentity.api.DedicatedSmp;
import com.siirio.jemsettlements.town.SettlementSavedData;
import com.siirio.jemsettlements.town.StorageLink;
import com.siirio.jemsettlements.town.TownRecord;
import com.siirio.jemsettlements.workforce.WorkforceRecord;
import com.siirio.jemsettlements.workforce.WorkforceType;
import com.siirio.jemsettlements.compat.recruits.RecruitsAdapter;
import com.siirio.jemsettlements.compat.workers.WorkersAdapter;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraft.world.level.block.Blocks;

public final class SettlementApi {
    public enum Result { ACCEPTED, REPLAYED, INSUFFICIENT_FUNDS, UNAVAILABLE, UNAUTHENTICATED, ALREADY_OWNED, INVALID, CONFLICT }

    private SettlementApi() {
    }

    public static Result create(ServerPlayer player, String rawName) {
        var hit = player.pick(16.0D, 0.0F, false);
        if (!(hit instanceof net.minecraft.world.phys.BlockHitResult blockHit)) return Result.INVALID;
        return create(player, rawName, blockHit.getBlockPos());
    }

    public static Result create(ServerPlayer player, String rawName, BlockPos bellPosition) {
        if (!DedicatedSmp.active(player)) return Result.UNAVAILABLE;
        var principal = IdentityPolicy.resolve(player);
        if (principal.isEmpty()) return Result.UNAUTHENTICATED;
        String name = rawName == null ? "" : rawName.strip().replaceAll("\\s+", " ");
        if (name.length() < 3 || name.length() > 32) return Result.INVALID;
        if (bellPosition == null || player.distanceToSqr(bellPosition.getX() + 0.5D, bellPosition.getY() + 0.5D, bellPosition.getZ() + 0.5D) > 256.0D
                || !player.serverLevel().getBlockState(bellPosition).is(Blocks.BELL)) return Result.INVALID;
        SettlementSavedData data = SettlementSavedData.get(player.getServer());
        if (data.ownedBy(principal.get().accountId()).isPresent()) return Result.ALREADY_OWNED;
        if (data.nameUsed(name) || data.anchorWithin(player.serverLevel().dimension().location(), bellPosition, 64)) return Result.CONFLICT;
        TownRecord town = new TownRecord(UUID.randomUUID(), principal.get().accountId(), name,
                player.serverLevel().dimension().location(), bellPosition);
        data.add(town);
        return Result.ACCEPTED;
    }

    public static Optional<TownRecord> ownedTown(ServerPlayer player) {
        return IdentityPolicy.resolve(player).flatMap(principal -> SettlementSavedData.get(player.getServer()).ownedBy(principal.accountId()));
    }

    public static Result linkPhysicalStorage(ServerPlayer player, BlockPos position) {
        if (!DedicatedSmp.active(player)) return Result.UNAVAILABLE;
        var town = ownedTown(player);
        if (town.isEmpty() || !player.serverLevel().hasChunkAt(position)) return Result.INVALID;
        if (!town.get().dimension().equals(player.serverLevel().dimension().location()) || town.get().center().distSqr(position) > 128L * 128L) return Result.INVALID;
        var blockEntity = player.serverLevel().getBlockEntity(position);
        if (blockEntity == null || !blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).isPresent()) return Result.INVALID;
        SettlementSavedData data = SettlementSavedData.get(player.getServer());
        StorageLink link = new StorageLink(player.serverLevel().dimension().location(), position.immutable());
        for (TownRecord other : data.towns()) if (!other.id().equals(town.get().id()) && other.storage().contains(link)) return Result.CONFLICT;
        if (!town.get().addStorage(link)) return Result.CONFLICT;
        data.changed();
        return Result.ACCEPTED;
    }

    public static Result registerNativeWorker(ServerPlayer player, Entity entity, WorkforceType type, boolean coinWageEnabled) {
        if (!DedicatedSmp.active(player)) return Result.UNAVAILABLE;
        var principal = IdentityPolicy.resolve(player);
        var town = ownedTown(player);
        if (principal.isEmpty() || town.isEmpty() || entity == null) return Result.UNAUTHENTICATED;
        String namespace = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getNamespace().toLowerCase(Locale.ROOT);
        if (!namespace.equals("workers") && !namespace.equals("recruits")) return Result.INVALID;
        if (namespace.equals("recruits")) {
            if (!RecruitsAdapter.ownedBy(entity, principal.get().accountId())) return Result.INVALID;
            type = WorkforceType.RECRUIT;
        } else {
            var nativeType = WorkersAdapter.typeOwnedBy(entity, principal.get().accountId());
            if (nativeType.isEmpty()) return Result.INVALID;
            type = nativeType.get();
        }
        SettlementSavedData data = SettlementSavedData.get(player.getServer());
        if (data.entityAssigned(entity.getUUID())) return Result.CONFLICT;
        data.add(new WorkforceRecord(entity.getUUID(), town.get().id(), principal.get().accountId(), type, namespace, coinWageEnabled));
        return Result.ACCEPTED;
    }

    public static long treasuryBalance(ServerPlayer player, TownRecord town) {
        return com.siirio.jemeconomy.api.EconomyApi.accountBalance(player,
                com.siirio.jemeconomy.api.EconomyApi.townAccount(town.id()));
    }

    public static Result treasury(ServerPlayer player, boolean deposit, long amount, UUID requestId) {
        if (!DedicatedSmp.active(player) || amount <= 0L || amount > 1_000_000_000L || requestId == null) return Result.INVALID;
        var principal = IdentityPolicy.resolve(player);
        var town = ownedTown(player);
        if (principal.isEmpty()) return Result.UNAUTHENTICATED;
        if (town.isEmpty() || !town.get().owner().equals(principal.get().accountId())) return Result.UNAVAILABLE;
        UUID account = com.siirio.jemeconomy.api.EconomyApi.townAccount(town.get().id());
        var transfer = deposit
                ? com.siirio.jemeconomy.api.EconomyApi.depositToAccount(player, account, amount, requestId)
                : com.siirio.jemeconomy.api.EconomyApi.withdrawFromAccount(player, account, amount, requestId);
        return switch (transfer.status()) {
            case COMMITTED -> Result.ACCEPTED;
            case REPLAYED -> Result.REPLAYED;
            case INSUFFICIENT_FUNDS -> Result.INSUFFICIENT_FUNDS;
            case UNAUTHENTICATED -> Result.UNAUTHENTICATED;
            default -> Result.UNAVAILABLE;
        };
    }
}
