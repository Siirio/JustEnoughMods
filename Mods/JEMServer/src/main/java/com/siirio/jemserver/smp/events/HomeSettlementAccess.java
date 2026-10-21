package com.siirio.jemserver.smp.events;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/** Optional Settlements authority adapter; loaded only when that module is present. */
final class HomeSettlementAccess {
    static boolean permits(ServerPlayer player, Entity entity) {
        var data = com.siirio.jemsettlements.town.SettlementSavedData.get(player.server);
        var town = data.resident(entity.getUUID()).map(com.siirio.jemsettlements.resident.ResidentMood::townId)
                .or(() -> data.workforceEntity(entity.getUUID()).map(com.siirio.jemsettlements.workforce.WorkforceRecord::townId));
        if (town.isEmpty()) return true;
        var principal = com.siirio.jemidentity.api.IdentityPolicy.resolve(player);
        return principal.isPresent() && data.town(town.get()).filter(value -> value.owner().equals(principal.get().accountId())).isPresent();
    }
    private HomeSettlementAccess() {}
}
