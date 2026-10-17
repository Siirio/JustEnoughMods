package com.siirio.jemsettlements.api;

import com.siirio.jemsettlements.runtime.SettlementAuthority;
import com.siirio.jemsettlements.town.SettlementSavedData;
import com.siirio.jemsettlements.town.StorageLink;
import com.siirio.jemsettlements.town.TownRecord;
import com.siirio.jemsettlements.workforce.WorkforceRecord;
import com.siirio.jemsettlements.workforce.WorkforceType;
import com.siirio.jemsettlements.compat.recruits.RecruitsAdapter;
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
        return create(player, rawName, bellPosition, UUID.randomUUID());
    }

    public static Result create(ServerPlayer player, String rawName, BlockPos bellPosition, UUID requestId) {
        if (!SettlementAuthority.active(player) || requestId == null) return Result.UNAVAILABLE;
        var principal = SettlementAuthority.principal(player);
        if (principal.isEmpty()) return Result.UNAUTHENTICATED;
        String name = rawName == null ? "" : rawName.strip().replaceAll("\\s+", " ");
        if (name.length() < 3 || name.length() > 32) return Result.INVALID;
        if (bellPosition == null || player.distanceToSqr(bellPosition.getX() + 0.5D, bellPosition.getY() + 0.5D, bellPosition.getZ() + 0.5D) > 256.0D
                || !player.serverLevel().getBlockState(bellPosition).is(Blocks.BELL)) return Result.INVALID;
        SettlementSavedData data = SettlementSavedData.get(player.getServer());
        UUID createdId = UUID.nameUUIDFromBytes(("settlement:create:" + principal.get() + ":" + requestId).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        if (data.town(createdId).isPresent()) return data.town(createdId).get().owner().equals(principal.get()) ? Result.REPLAYED : Result.CONFLICT;
        if (data.nameUsed(name) || data.anchorWithin(player.serverLevel().dimension().location(), bellPosition, 64)) return Result.CONFLICT;
        TownRecord town = new TownRecord(createdId, principal.get(), name,
                player.serverLevel().dimension().location(), bellPosition);
        for (TownRecord other : data.towns()) if (other.dimension().equals(town.dimension())
                && town.territoryMinX() <= other.territoryMaxX() + 16 && town.territoryMaxX() >= other.territoryMinX() - 16
                && town.territoryMinZ() <= other.territoryMaxZ() + 16 && town.territoryMaxZ() >= other.territoryMinZ() - 16) return Result.CONFLICT;
        var payment = com.siirio.jemsettlements.currency.SettlementCurrency.depositToAccount(player,
                com.siirio.jemeconomy.ledger.SystemAccount.OPERATIONS.id(), com.siirio.jemsettlements.payroll.SettlementEconomyConfig.founding(), town.id());
        if (payment.status() == com.siirio.jemeconomy.ledger.TransferResult.Status.INSUFFICIENT_FUNDS) return Result.INSUFFICIENT_FUNDS;
        if (payment.status() != com.siirio.jemeconomy.ledger.TransferResult.Status.COMMITTED
                && payment.status() != com.siirio.jemeconomy.ledger.TransferResult.Status.REPLAYED) return Result.UNAVAILABLE;
        data.add(town);
        return Result.ACCEPTED;
    }

    public static Optional<TownRecord> ownedTown(ServerPlayer player) {
        var towns = ownedTowns(player); return towns.size() == 1 ? Optional.of(towns.get(0)) : Optional.empty();
    }

    public static java.util.List<TownRecord> ownedTowns(ServerPlayer player) {
        return SettlementAuthority.principal(player).map(principal -> SettlementSavedData.get(player.getServer()).ownedTowns(principal)).orElseGet(java.util.List::of);
    }

    public static Optional<TownRecord> ownedTown(ServerPlayer player, UUID townId) {
        if (townId == null) return Optional.empty();
        var principal = SettlementAuthority.principal(player);
        if (principal.isEmpty()) return Optional.empty();
        return SettlementSavedData.get(player.getServer()).town(townId).filter(town -> town.owner().equals(principal.get()));
    }

    private static Optional<TownRecord> ownedTownNear(ServerPlayer player, BlockPos position) {
        var matching = ownedTowns(player).stream().filter(town -> town.dimension().equals(player.serverLevel().dimension().location()) && town.contains(position)).toList();
        return matching.size() == 1 ? Optional.of(matching.get(0)) : Optional.empty();
    }

    public static Result linkPhysicalStorage(ServerPlayer player, BlockPos position) {
        if (!SettlementAuthority.active(player)) return Result.UNAVAILABLE;
        var town = ownedTownNear(player, position);
        return town.isEmpty() ? Result.INVALID : linkPhysicalStorage(player, town.get().id(), position);
    }
    public static Result linkPhysicalStorage(ServerPlayer player, UUID townId, BlockPos position) {
        if (!SettlementAuthority.active(player) || position == null) return Result.INVALID;
        if (player.distanceToSqr(position.getX() + .5D, position.getY() + .5D, position.getZ() + .5D) > 36.0D) return Result.INVALID;
        var town = ownedTown(player, townId);
        if (town.isEmpty() || !player.serverLevel().hasChunkAt(position)) return Result.INVALID;
        if (!town.get().dimension().equals(player.serverLevel().dimension().location()) || town.get().center().distSqr(position) > 128L * 128L) return Result.INVALID;
        var blockEntity = player.serverLevel().getBlockEntity(position);
        if (blockEntity == null || !blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).isPresent()) return Result.INVALID;
        SettlementSavedData data = SettlementSavedData.get(player.getServer());
        StorageLink link = new StorageLink(player.serverLevel().dimension().location(), position.immutable()).at(position.immutable(), com.siirio.jemsettlements.runtime.SharedStorageService.partner(player.serverLevel(), position));
        for (TownRecord other : data.towns()) if (!other.id().equals(town.get().id()) && other.storage().stream().anyMatch(value -> value.dimension().equals(link.dimension()) && (value.contains(link.position()) || link.second()!=null && value.contains(link.second())))) return Result.CONFLICT;
        if (town.get().storage().stream().anyMatch(value -> value.contains(link.position()) || link.second()!=null && value.contains(link.second()))) return Result.CONFLICT;
        if (!town.get().addStorage(link)) return Result.CONFLICT;
        data.changed();
        return Result.ACCEPTED;
    }

    public static Result registerNativeWorker(ServerPlayer player, Entity entity, WorkforceType type, boolean coinWageEnabled) {
        if (!SettlementAuthority.active(player)) return Result.UNAVAILABLE;
        var town = ownedTownNear(player, entity == null ? player.blockPosition() : entity.blockPosition());
        return town.isEmpty() ? Result.INVALID : registerNativeWorker(player, town.get().id(), entity, type, coinWageEnabled);
    }
    public static Result registerNativeWorker(ServerPlayer player, UUID townId, Entity entity, WorkforceType type, boolean coinWageEnabled) {
        if (!SettlementAuthority.active(player)) return Result.UNAVAILABLE;
        var principal = SettlementAuthority.principal(player);
        var town = ownedTown(player, townId);
        if (principal.isEmpty() || town.isEmpty() || entity == null) return Result.UNAUTHENTICATED;
        if (entity.level() != player.level() || !town.get().dimension().equals(player.serverLevel().dimension().location())
                || player.distanceToSqr(entity) > 256.0D || !town.get().contains(entity.blockPosition())) return Result.INVALID;
        String namespace = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getNamespace().toLowerCase(Locale.ROOT);
        if (!namespace.equals("recruits")) return Result.INVALID;
        if (namespace.equals("recruits")) {
            if (!net.minecraftforge.fml.ModList.get().isLoaded("recruits") || !RecruitsAdapter.ownedBy(entity, principal.get())) return Result.INVALID;
            type = WorkforceType.RECRUIT;
            coinWageEnabled = true;
        }
        SettlementSavedData data = SettlementSavedData.get(player.getServer());
        if (data.entityAssigned(entity.getUUID())) {
            var existing = data.workforceEntity(entity.getUUID()).orElseThrow();
            if (existing.type() != WorkforceType.RECRUIT || !existing.ownerId().equals(principal.get())
                    || !existing.settlementActive()) return Result.CONFLICT;
            if (existing.townId().equals(townId)) {
                existing.enrollNeeds(); existing.observeName(entity); data.changed(); return Result.ACCEPTED;
            }
            if (data.town(existing.townId()).isPresent()) return Result.CONFLICT;
            if (data.employer(existing.ownerId()).payroll().stream().anyMatch(period -> !period.paid()
                    && period.roster().stream().anyMatch(wage -> wage.soldierId().equals(existing.entityId())))) return Result.CONFLICT;
            existing.assignTown(town.get().id()); existing.enrollNeeds(); existing.observeName(entity); data.changed(); return Result.ACCEPTED;
        }
        var nativeType = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        WorkforceRecord registered = new WorkforceRecord(entity.getUUID(), town.get().id(), principal.get(), type, namespace, nativeType, coinWageEnabled, player.serverLevel().getGameTime());
        registered.enrollNeeds(); registered.observeName(entity); data.add(registered);
        if (type == WorkforceType.RECRUIT) town.get().initializePayroll(player.serverLevel().getGameTime());
        return Result.ACCEPTED;
    }

    public static Result registerResident(ServerPlayer player, Villager villager) {
        if (!SettlementAuthority.active(player) || villager == null || !villager.isAlive() || villager.isBaby()) return Result.INVALID;
        var town = ownedTownNear(player, villager.blockPosition());
        return town.isEmpty() ? Result.INVALID : registerResident(player, town.get().id(), villager);
    }
    public static Result registerResident(ServerPlayer player, UUID townId, Villager villager) {
        if (!SettlementAuthority.active(player) || villager == null || !villager.isAlive() || villager.isBaby() || villager.level() != player.level() || player.distanceToSqr(villager) > 256.0D) return Result.INVALID;
        var town = ownedTown(player, townId); if (town.isEmpty() || !town.get().contains(villager.blockPosition())) return Result.UNAUTHENTICATED;
        if (!town.get().dimension().equals(player.serverLevel().dimension().location()) || town.get().center().distSqr(villager.blockPosition()) > 128L * 128L) return Result.INVALID;
        SettlementSavedData data = SettlementSavedData.get(player.getServer()); if (data.residentAssigned(villager.getUUID())) return Result.CONFLICT;
        int cap = switch (town.get().tier()) { case CAMP, VILLAGE -> 24; case TOWN -> 48; case CITY, CITADEL -> 96; };
        if (data.residents(town.get().id()).stream().filter(ResidentMood::living).count() >= cap) return Result.CONFLICT;
        var home = villager.getBrain().getMemory(MemoryModuleType.HOME).orElse(null); var work = villager.getBrain().getMemory(MemoryModuleType.JOB_SITE).orElse(null);
        ResidentMood resident = new ResidentMood(villager.getUUID(), town.get().id(), home, work); resident.observeName(villager); resident.markAmenitiesDirty(player.serverLevel().getGameTime()); data.add(resident); return Result.ACCEPTED;
    }

    public static Result setFoodStorage(ServerPlayer player, BlockPos position, boolean enabled) {
        var town = ownedTown(player); return town.isEmpty() ? Result.INVALID : setFoodStorage(player, town.get().id(), position, enabled);
    }
    public static Result setFoodStorage(ServerPlayer player, UUID townId, BlockPos position, boolean enabled) {
        if (!SettlementAuthority.active(player) || position == null) return Result.INVALID;
        var town = ownedTown(player, townId); if (town.isEmpty()) return Result.UNAUTHENTICATED;
        StorageLink link = new StorageLink(player.serverLevel().dimension().location(), position);
        if (!town.get().setStorageFeed(link, true)) return Result.INVALID; SettlementSavedData.get(player.getServer()).changed(); return Result.ACCEPTED;
    }

    public static Result setTerritoryRadius(ServerPlayer player, int radius) {
        if (!SettlementAuthority.active(player)) return Result.UNAVAILABLE;
        var town = ownedTown(player);
        if (town.isEmpty()) return Result.UNAUTHENTICATED;
        if (radius < 32 || radius > 256) return Result.INVALID;
        // Legacy command must pass through the same priced union as Xaero. Shrinking
        // and reclaiming land is not a means of bypassing the area price.
        return expandBounds(player, town.get(), town.get().center().getX() - radius,
                town.get().center().getZ() - radius, town.get().center().getX() + radius,
                town.get().center().getZ() + radius, UUID.randomUUID());
    }

    public static Result expandTerritory(ServerPlayer player, UUID townId, int targetX, int targetZ, UUID requestId) {
        if (!SettlementAuthority.active(player) || requestId == null) return Result.INVALID;
        var town = ownedTown(player, townId);
        if (town.isEmpty()) return Result.UNAUTHENTICATED;
        if (town.get().expansionSeen(requestId)) return Result.REPLAYED;
        return expandBounds(player, town.get(), targetX, targetZ, targetX, targetZ, requestId);
    }

    private static Result expandBounds(ServerPlayer player, TownRecord value, int proposedMinX, int proposedMinZ,
                                       int proposedMaxX, int proposedMaxZ, UUID requestId) {
        var town = Optional.of(value); UUID townId = value.id();
        if ((long) proposedMinX < (long) value.center().getX() - 256 || (long) proposedMaxX > (long) value.center().getX() + 256
                || (long) proposedMinZ < (long) value.center().getZ() - 256 || (long) proposedMaxZ > (long) value.center().getZ() + 256) return Result.CONFLICT;
        var quote = com.siirio.jemsettlements.town.TerritoryQuote.union(value.territoryMinX(), value.territoryMinZ(), value.territoryMaxX(), value.territoryMaxZ(),
                proposedMinX, proposedMinZ, proposedMaxX, proposedMaxZ);
        int minX = quote.minX(), maxX = quote.maxX(), minZ = quote.minZ(), maxZ = quote.maxZ();
        if (minX < town.get().center().getX() - 256 || maxX > town.get().center().getX() + 256 || minZ < town.get().center().getZ() - 256 || maxZ > town.get().center().getZ() + 256) return Result.CONFLICT;
        long added = quote.addedCopper();
        if (added <= 0L) return Result.INVALID;
        SettlementSavedData data = SettlementSavedData.get(player.getServer());
        for (TownRecord other : data.towns()) if (!other.id().equals(townId) && other.dimension().equals(town.get().dimension())) {
            if (minX <= other.territoryMaxX() + 16 && maxX >= other.territoryMinX() - 16 && minZ <= other.territoryMaxZ() + 16 && maxZ >= other.territoryMinZ() - 16) return Result.CONFLICT;
        }
        var payment = com.siirio.jemsettlements.currency.SettlementCurrency.depositToAccount(player,
                com.siirio.jemeconomy.ledger.SystemAccount.OPERATIONS.id(), added,
                UUID.nameUUIDFromBytes(("territory:" + townId + ":" + requestId + ":" + minX + ":" + minZ + ":" + maxX + ":" + maxZ).getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        if (payment.status() == com.siirio.jemeconomy.ledger.TransferResult.Status.INSUFFICIENT_FUNDS) return Result.INSUFFICIENT_FUNDS;
        if (payment.status() != com.siirio.jemeconomy.ledger.TransferResult.Status.COMMITTED
                && payment.status() != com.siirio.jemeconomy.ledger.TransferResult.Status.REPLAYED) return Result.UNAVAILABLE;
        town.get().territoryBounds(minX, minZ, maxX, maxZ);
        town.get().recordExpansion(requestId);
        data.changed();
        return payment.status() == com.siirio.jemeconomy.ledger.TransferResult.Status.REPLAYED ? Result.REPLAYED : Result.ACCEPTED;
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

    public static Result delete(ServerPlayer player, UUID townId, UUID requestId) {
        if (!SettlementAuthority.active(player) || requestId == null) return Result.INVALID;
        var town = ownedTown(player, townId);
        if (town.isEmpty()) return Result.UNAUTHENTICATED;
        var deletionData = SettlementSavedData.get(player.getServer());
        if (!com.siirio.jemsettlements.currency.SettlementCurrency.canDelete(player, townId)
                || town.get().payroll().stream().anyMatch(period -> !period.paid())
                || deletionData.residents(townId).stream().anyMatch(resident -> resident.living() && resident.mood() < 10)) return Result.CONFLICT;
        long balance = com.siirio.jemsettlements.currency.SettlementCurrency.townBalance(player.getServer(), townId);
        boolean replayed = false;
        if (balance > 0L) {
            var refund = com.siirio.jemsettlements.currency.SettlementCurrency.withdrawFromAccount(player,
                    com.siirio.jemsettlements.currency.SettlementCurrency.townAccount(townId), balance, requestId);
            if (refund.status() == com.siirio.jemeconomy.ledger.TransferResult.Status.INSUFFICIENT_FUNDS) return Result.CONFLICT;
            if (refund.status() != com.siirio.jemeconomy.ledger.TransferResult.Status.COMMITTED
                    && refund.status() != com.siirio.jemeconomy.ledger.TransferResult.Status.REPLAYED) return Result.UNAVAILABLE;
            replayed = refund.status() == com.siirio.jemeconomy.ledger.TransferResult.Status.REPLAYED;
        }
        SettlementSavedData.get(player.getServer()).removeTown(townId);
        return replayed ? Result.REPLAYED : Result.ACCEPTED;
    }

    public static Result removeRegistration(ServerPlayer player, UUID townId, UUID entityId) {
        if (ownedTown(player, townId).isEmpty()) return Result.UNAUTHENTICATED;
        SettlementSavedData data = SettlementSavedData.get(player.getServer());
        if (data.resident(entityId).filter(resident -> resident.townId().equals(townId) && resident.living() && resident.mood() < 10).isPresent()) return Result.CONFLICT;
        return data.removeResident(townId, entityId) || data.removeWorkforce(townId, entityId) ? Result.ACCEPTED : Result.INVALID;
    }

    public static Result openRecruitBuyback(ServerPlayer player, UUID townId, UUID entityId) {
        var employer = SettlementAuthority.principal(player);
        if (employer.isEmpty() || (ownedTown(player, townId).isEmpty() && !employer.get().equals(townId))) return Result.UNAUTHENTICATED;
        var record = SettlementSavedData.get(player.getServer()).workforceEntity(entityId);
        if (record.isEmpty() || !record.get().townId().equals(townId) || !record.get().ownerId().equals(SettlementAuthority.principal(player).orElse(null))) return Result.INVALID;
        return net.minecraftforge.fml.ModList.get().isLoaded("recruits") && com.siirio.jemsettlements.compat.recruits.RecruitRebellionService.openBuyback(player, entityId) ? Result.ACCEPTED : Result.UNAVAILABLE;
    }

    public static Result openNativeManagement(ServerPlayer player, UUID townId, UUID entityId) {
        if (!SettlementAuthority.active(player) || ownedTown(player, townId).isEmpty()) return Result.UNAUTHENTICATED;
        var data = SettlementSavedData.get(player.getServer());
        var record = data.workforceEntity(entityId);
        if (record.isEmpty() || !record.get().townId().equals(townId) || !record.get().settlementActive()) return Result.INVALID;
        var entity = player.serverLevel().getEntity(entityId);
        if (entity == null || !entity.isAlive() || player.distanceToSqr(entity) > 256.0D) return Result.UNAVAILABLE;
        UUID owner = SettlementAuthority.principal(player).orElseThrow();
        if (record.get().nativeModId().equals("recruits") && net.minecraftforge.fml.ModList.get().isLoaded("recruits"))
            return com.siirio.jemsettlements.compat.recruits.RecruitRuntime.openManagement(player, entity, owner) ? Result.ACCEPTED : Result.INVALID;
        return Result.UNAVAILABLE;
    }

    public static long treasuryBalance(ServerPlayer player, TownRecord town) {
        return com.siirio.jemsettlements.currency.SettlementCurrency.accountBalance(player,
                com.siirio.jemsettlements.currency.SettlementCurrency.townAccount(town.id()));
    }

    public static Result treasury(ServerPlayer player, boolean deposit, long amount, UUID requestId) {
        var owned = ownedTowns(player);
        return owned.size() == 1 ? treasury(player, owned.get(0).id(), deposit, amount, requestId) : Result.INVALID;
    }

    public static Result treasury(ServerPlayer player, UUID townId, boolean deposit, long amount, UUID requestId) {
        if (!SettlementAuthority.active(player) || amount <= 0L || amount > 1_000_000_000L || requestId == null) return Result.INVALID;
        var principal = SettlementAuthority.principal(player);
        var town = ownedTown(player, townId);
        if (principal.isEmpty()) return Result.UNAUTHENTICATED;
        if (town.isEmpty() || !town.get().owner().equals(principal.get())) return Result.UNAVAILABLE;
        UUID account = com.siirio.jemsettlements.currency.SettlementCurrency.townAccount(town.get().id());
        var transfer = deposit
                ? com.siirio.jemsettlements.currency.SettlementCurrency.depositToAccount(player, account, amount, requestId)
                : com.siirio.jemsettlements.currency.SettlementCurrency.withdrawFromAccount(player, account, amount, requestId);
        return switch (transfer.status()) {
            case COMMITTED -> Result.ACCEPTED;
            case REPLAYED -> Result.REPLAYED;
            case INSUFFICIENT_FUNDS -> Result.INSUFFICIENT_FUNDS;
            case UNAUTHENTICATED -> Result.UNAUTHENTICATED;
            default -> Result.UNAVAILABLE;
        };
    }
}
