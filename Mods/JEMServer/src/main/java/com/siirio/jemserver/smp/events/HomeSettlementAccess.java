package com.siirio.jemserver.smp.events;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/** Optional Settlements authority adapter; loaded only when that module is present. */
final class HomeSettlementAccess {
    static boolean permits(ServerPlayer player, Entity entity) {
        return !com.siirio.jemserver.smp.DomainFeatures.loaded("jem_settlements")
                || com.siirio.jemserver.compat.settlements.SettlementHostAdapter.permits(player, entity);
    }
    private HomeSettlementAccess() {}
}
