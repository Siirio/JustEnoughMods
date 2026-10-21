package com.siirio.jemserver.compat.settlements;

import com.siirio.jemsettlements.api.SettlementApi;
import com.siirio.jemsettlements.network.SettlementNetwork;
import com.siirio.jemsettlements.town.SettlementSavedData;
import java.util.List;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/** Loaded only after Settlements presence; only native/server DTOs cross the boundary. */
public final class SettlementHostAdapter {
    private SettlementHostAdapter() { }
    public static void open(ServerPlayer player, UUID selected) {
        if (selected == null) SettlementNetwork.sendOwned(player, true);
        else SettlementNetwork.sendSelected(player, selected, true);
    }
    public static void profile(ServerPlayer viewer, UUID id, CompoundTag row) {
        var data = SettlementSavedData.get(viewer.server);
        data.towns().stream().filter(town -> town.members().contains(id)).findFirst().ifPresent(town -> {
            row.putString("settlement", town.name()); row.putBoolean("settlementOwner", town.owner().equals(id));
            row.putString("settlementTier", town.tier().name());
        });
        long recruits = data.towns().stream().flatMap(town -> data.workforce(town.id()).stream())
                .filter(record -> record.ownerId().equals(id) && record.type() == com.siirio.jemsettlements.workforce.WorkforceType.RECRUIT).count();
        row.putLong("recruitCount", recruits);
    }
    public static String action(ServerPlayer player, com.siirio.jemserver.smp.SettlementUiAction action) {
        var result = switch (action.action()) {
            case "rename" -> SettlementApi.rename(player, action.town(), action.text());
            case "remove" -> action.target() == null ? SettlementApi.Result.INVALID : SettlementApi.removeRegistration(player, action.town(), action.target());
            case "delete" -> SettlementApi.delete(player, action.town(), action.request());
            case "buyback" -> action.target() == null ? SettlementApi.Result.INVALID : SettlementApi.openRecruitBuyback(player, action.town(), action.target());
            case "deposit" -> SettlementApi.treasury(player, action.town(), true, action.amount(), action.request());
            case "withdraw" -> SettlementApi.treasury(player, action.town(), false, action.amount(), action.request());
            default -> SettlementApi.Result.INVALID;
        };
        return result.name();
    }
    public static String expand(ServerPlayer player, UUID id, int x, int z, UUID request) {
        return SettlementApi.expandTerritory(player, id, x, z, request).name();
    }
    public static List<com.siirio.jemserver.MapNetwork.SettlementArea> areas(ServerPlayer player) {
        var principal = com.siirio.jemidentity.api.IdentityPolicy.resolve(player);
        return SettlementSavedData.get(player.server).towns().stream().limit(256)
                .map(town -> new com.siirio.jemserver.MapNetwork.SettlementArea(town.id(), town.name(), town.dimension(), town.center(),
                        town.territoryMinX(), town.territoryMinZ(), town.territoryMaxX(), town.territoryMaxZ(),
                        principal.isPresent() && town.owner().equals(principal.get().accountId()))).toList();
    }
    public static boolean permits(ServerPlayer player, Entity entity) {
        var data = SettlementSavedData.get(player.server);
        var town = data.resident(entity.getUUID()).map(com.siirio.jemsettlements.resident.ResidentMood::townId)
                .or(() -> data.workforceEntity(entity.getUUID()).map(com.siirio.jemsettlements.workforce.WorkforceRecord::townId));
        if (town.isEmpty()) return true;
        var principal = com.siirio.jemidentity.api.IdentityPolicy.resolve(player);
        return principal.isPresent() && data.town(town.get()).filter(value -> value.owner().equals(principal.get().accountId())).isPresent();
    }
}
