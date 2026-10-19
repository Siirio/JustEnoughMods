package com.siirio.jemsettlements.happiness;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.Villager;

public final class ResidentPatService {
    private ResidentPatService() { }
    public static void accept(ServerPlayer player, java.util.UUID id) {
        if (!com.siirio.jemsettlements.runtime.SettlementAuthority.active(player) || !player.isAlive() || player.isSpectator()) return;
        var level = player.serverLevel();
        if (!(level.getEntity(id) instanceof Villager villager) || !villager.isAlive() || villager.isInvisible()
                || player.distanceToSqr(villager) > 16D || !player.hasLineOfSight(villager)) return;
        var data = com.siirio.jemsettlements.town.SettlementSavedData.get(player.getServer());
        var resident = data.resident(id);
        if (resident.isEmpty() || data.town(resident.get().townId()).filter(town -> town.dimension().equals(level.dimension().location()) && town.contains(villager.blockPosition())).isEmpty()) return;
        if (resident.get().pat(player.getServer().overworld().getGameTime())) data.changed();
    }
}
