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

public final class SettlementApi {
    public enum Result { ACCEPTED, UNAVAILABLE, UNAUTHENTICATED, ALREADY_OWNED, INVALID, CONFLICT }

    private SettlementApi() {
    }

    public static Result create(ServerPlayer player, String rawName) {
        if (!DedicatedSmp.active(player)) return Result.UNAVAILABLE;
        var principal = IdentityPolicy.resolve(player);
        if (principal.isEmpty()) return Result.UNAUTHENTICATED;
        String name = rawName == null ? "" : rawName.strip().replaceAll("\\s+", " ");
        if (name.length() < 3 || name.length() > 32) return Result.INVALID;
        SettlementSavedData data = SettlementSavedData.get(player.getServer());
        if (data.ownedBy(principal.get().accountId()).isPresent()) return Result.ALREADY_OWNED;
        TownRecord town = new TownRecord(UUID.randomUUID(), principal.get().accountId(), name,
                player.serverLevel().dimension().location(), player.blockPosition());
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
}
