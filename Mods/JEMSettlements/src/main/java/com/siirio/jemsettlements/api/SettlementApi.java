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
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import com.siirio.jemsettlements.resident.ResidentMood;
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
        if (data.nameUsed(name) || data.anchorWithin(player.serverLevel().dimension().location(), bellPosition, 64)) return Result.CONFLICT;
        TownRecord town = new TownRecord(UUID.randomUUID(), principal.get().accountId(), name,
                player.serverLevel().dimension().location(), bellPosition);
        data.add(town);
        return Result.ACCEPTED;
    }

    public static Optional<TownRecord> ownedTown(ServerPlayer player) {
        return IdentityPolicy.resolve(player).flatMap(principal -> SettlementSavedData.get(player.getServer()).ownedBy(principal.accountId()));
    }

    public static java.util.List<TownRecord> ownedTowns(ServerPlayer player) {
        return IdentityPolicy.resolve(player).map(principal -> SettlementSavedData.get(player.getServer()).ownedTowns(principal.accountId())).orElseGet(java.util.List::of);
    }

    public static Optional<TownRecord> ownedTown(ServerPlayer player, UUID townId) {
        if (townId == null) return Optional.empty();
        var principal = IdentityPolicy.resolve(player);
        if (principal.isEmpty()) return Optional.empty();
        return SettlementSavedData.get(player.getServer()).town(townId).filter(town -> town.owner().equals(principal.get().accountId()));
    }

    private static Optional<TownRecord> ownedTownNear(ServerPlayer player, BlockPos position) {
        return ownedTowns(player).stream().filter(town -> town.dimension().equals(player.serverLevel().dimension().location()))
                .min(java.util.Comparator.comparingDouble(town -> town.center().distSqr(position)));
    }

    public static Result linkPhysicalStorage(ServerPlayer player, BlockPos position) {
        if (!DedicatedSmp.active(player)) return Result.UNAVAILABLE;
        var town = ownedTownNear(player, position);
        if (town.isEmpty() || !player.serverLevel().hasChunkAt(position)) return Result.INVALID;
        if (!town.get().dimension().equals(player.serverLevel().dimension().location()) || town.get().center().distSqr(position) > 128L * 128L) return Result.INVALID;
        var blockEntity = player.serverLevel().getBlockEntity(position);
        if (blockEntity == null || !blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).isPresent()) return Result.INVALID;
        SettlementSavedData data = SettlementSavedData.get(player.getServer());
        StorageLink link = new StorageLink(player.serverLevel().dimension().location(), position.immutable());
        for (TownRecord other : data.towns()) if (!other.id().equals(town.get().id()) && other.storage().stream().anyMatch(value -> value.dimension().equals(link.dimension()) && value.position().equals(link.position()))) return Result.CONFLICT;
        if (!town.get().addStorage(link)) return Result.CONFLICT;
        data.changed();
        return Result.ACCEPTED;
    }

    public static Result registerNativeWorker(ServerPlayer player, Entity entity, WorkforceType type, boolean coinWageEnabled) {
        if (!DedicatedSmp.active(player)) return Result.UNAVAILABLE;
        var principal = IdentityPolicy.resolve(player);
        var town = ownedTownNear(player, entity == null ? player.blockPosition() : entity.blockPosition());
        if (principal.isEmpty() || town.isEmpty() || entity == null) return Result.UNAUTHENTICATED;
        String namespace = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getNamespace().toLowerCase(Locale.ROOT);
        if (!namespace.equals("workers") && !namespace.equals("recruits")) return Result.INVALID;
        if (namespace.equals("recruits")) {
            if (!RecruitsAdapter.ownedBy(entity, principal.get().accountId())) return Result.INVALID;
            type = WorkforceType.RECRUIT;
            coinWageEnabled = true;
        } else {
            var nativeType = WorkersAdapter.typeOwnedBy(entity, principal.get().accountId());
            if (nativeType.isEmpty()) return Result.INVALID;
            type = nativeType.get();
        }
        SettlementSavedData data = SettlementSavedData.get(player.getServer());
        if (data.entityAssigned(entity.getUUID())) return Result.CONFLICT;
        var nativeType = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        data.add(new WorkforceRecord(entity.getUUID(), town.get().id(), principal.get().accountId(), type, namespace, nativeType, coinWageEnabled, player.serverLevel().getGameTime()));
        if (type == WorkforceType.RECRUIT) town.get().initializePayroll(player.serverLevel().getGameTime());
        return Result.ACCEPTED;
    }

    public static Result registerResident(ServerPlayer player, Villager villager) {
        if (!DedicatedSmp.active(player) || villager == null || !villager.isAlive() || villager.isBaby()) return Result.INVALID;
        var town = ownedTownNear(player, villager.blockPosition()); if (town.isEmpty()) return Result.UNAUTHENTICATED;
        if (!town.get().dimension().equals(player.serverLevel().dimension().location()) || town.get().center().distSqr(villager.blockPosition()) > 128L * 128L) return Result.INVALID;
        SettlementSavedData data = SettlementSavedData.get(player.getServer()); if (data.residentAssigned(villager.getUUID())) return Result.CONFLICT;
        int cap = switch (town.get().tier()) { case CAMP, VILLAGE -> 24; case TOWN -> 48; case CITY, CITADEL -> 96; };
        if (data.residents(town.get().id()).stream().filter(ResidentMood::living).count() >= cap) return Result.CONFLICT;
        var home = villager.getBrain().getMemory(MemoryModuleType.HOME).orElse(null); var work = villager.getBrain().getMemory(MemoryModuleType.JOB_SITE).orElse(null);
        ResidentMood resident = new ResidentMood(villager.getUUID(), town.get().id(), home, work); resident.markAmenitiesDirty(player.serverLevel().getGameTime()); data.add(resident); return Result.ACCEPTED;
    }

    public static Result setFoodStorage(ServerPlayer player, BlockPos position, boolean enabled) {
        var town = ownedTown(player); if (town.isEmpty()) return Result.UNAUTHENTICATED;
        StorageLink link = new StorageLink(player.serverLevel().dimension().location(), position);
        if (!town.get().setStorageFeed(link, enabled)) return Result.INVALID; SettlementSavedData.get(player.getServer()).changed(); return Result.ACCEPTED;
    }

    public static Result setTerritoryRadius(ServerPlayer player, int radius) {
        var principal = IdentityPolicy.resolve(player); var town = ownedTown(player);
        if (principal.isEmpty() || town.isEmpty()) return Result.UNAUTHENTICATED;
        if (!town.get().owner().equals(principal.get().accountId()) || radius < 32 || radius > 256) return Result.INVALID;
        SettlementSavedData data = SettlementSavedData.get(player.getServer());
        for (TownRecord other : data.towns()) if (!other.id().equals(town.get().id()) && other.dimension().equals(town.get().dimension())) {
            int separation = radius + other.territoryRadius() + 16;
            if (Math.abs(other.center().getX() - town.get().center().getX()) < separation
                    && Math.abs(other.center().getZ() - town.get().center().getZ()) < separation) return Result.CONFLICT;
        }
        town.get().territoryRadius(radius); data.changed(); return Result.ACCEPTED;
    }

    public static Result expandTerritory(ServerPlayer player, UUID townId, int targetX, int targetZ, UUID requestId) {
        if (!DedicatedSmp.active(player) || requestId == null) return Result.INVALID;
        var town = ownedTown(player, townId);
        if (town.isEmpty()) return Result.UNAUTHENTICATED;
        if (town.get().expansionSeen(requestId)) return Result.REPLAYED;
        int minX = Math.min(town.get().territoryMinX(), targetX), maxX = Math.max(town.get().territoryMaxX(), targetX);
        int minZ = Math.min(town.get().territoryMinZ(), targetZ), maxZ = Math.max(town.get().territoryMaxZ(), targetZ);
        if (minX < town.get().center().getX() - 256 || maxX > town.get().center().getX() + 256 || minZ < town.get().center().getZ() - 256 || maxZ > town.get().center().getZ() + 256) return Result.CONFLICT;
        long area = (long) (maxX - minX + 1) * (maxZ - minZ + 1), added = area - town.get().territoryArea();
        if (added <= 0L) return Result.INVALID;
        SettlementSavedData data = SettlementSavedData.get(player.getServer());
        for (TownRecord other : data.towns()) if (!other.id().equals(townId) && other.dimension().equals(town.get().dimension())) {
            if (minX <= other.territoryMaxX() + 16 && maxX >= other.territoryMinX() - 16 && minZ <= other.territoryMaxZ() + 16 && maxZ >= other.territoryMinZ() - 16) return Result.CONFLICT;
        }
        var payment = com.siirio.jemeconomy.api.EconomyApi.spendPhysical(player, added, requestId, "settlementTerritorySpend");
        if (payment == com.siirio.jemeconomy.physical.PhysicalCoinService.Status.NO_VALID_COINS
                || payment == com.siirio.jemeconomy.physical.PhysicalCoinService.Status.INSUFFICIENT_FUNDS) return Result.INSUFFICIENT_FUNDS;
        if (payment != com.siirio.jemeconomy.physical.PhysicalCoinService.Status.COMMITTED
                && payment != com.siirio.jemeconomy.physical.PhysicalCoinService.Status.REPLAYED) return Result.UNAVAILABLE;
        town.get().territoryBounds(minX, minZ, maxX, maxZ);
        town.get().recordExpansion(requestId);
        data.changed();
        return payment == com.siirio.jemeconomy.physical.PhysicalCoinService.Status.REPLAYED ? Result.REPLAYED : Result.ACCEPTED;
    }

    public static Result rename(ServerPlayer player, UUID townId, String rawName) {
        var town = ownedTown(player, townId);
        if (town.isEmpty()) return Result.UNAUTHENTICATED;
        String name = rawName == null ? "" : rawName.strip().replaceAll("\\s+", " ");
        if (name.length() < 3 || name.length() > 32) return Result.INVALID;
        SettlementSavedData data = SettlementSavedData.get(player.getServer());
        if (!town.get().name().equalsIgnoreCase(name) && data.nameUsed(name)) return Result.CONFLICT;
        town.get().rename(name); data.changed(); return Result.ACCEPTED;
    }

    public static Result removeRegistration(ServerPlayer player, UUID townId, UUID entityId) {
        if (ownedTown(player, townId).isEmpty()) return Result.UNAUTHENTICATED;
        SettlementSavedData data = SettlementSavedData.get(player.getServer());
        return data.removeResident(townId, entityId) || data.removeWorkforce(townId, entityId) ? Result.ACCEPTED : Result.INVALID;
    }

    public static long treasuryBalance(ServerPlayer player, TownRecord town) {
        return com.siirio.jemeconomy.api.EconomyApi.accountBalance(player,
                com.siirio.jemeconomy.api.EconomyApi.townAccount(town.id()));
    }

    public static Result treasury(ServerPlayer player, boolean deposit, long amount, UUID requestId) {
        var owned = ownedTowns(player);
        return owned.size() == 1 ? treasury(player, owned.get(0).id(), deposit, amount, requestId) : Result.INVALID;
    }

    public static Result treasury(ServerPlayer player, UUID townId, boolean deposit, long amount, UUID requestId) {
        if (!DedicatedSmp.active(player) || amount <= 0L || amount > 1_000_000_000L || requestId == null) return Result.INVALID;
        var principal = IdentityPolicy.resolve(player);
        var town = ownedTown(player, townId);
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
