package com.siirio.jemsettlements.network;

import com.siirio.jemsettlements.runtime.SettlementAuthority;
import com.siirio.jemsettlements.town.SettlementSavedData;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Bounded observation of persisted conditions; unchanged warnings never generate another toast. */
public final class SettlementWarnings {
    private static final Map<UUID, Set<String>> CONDITIONS = new HashMap<>();
    private static int cursor;
    private SettlementWarnings() { }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.getServer().getTickCount() % 100 != 0) return;
        var data = SettlementSavedData.get(event.getServer()); var towns = new java.util.ArrayList<>(data.towns());
        if (towns.isEmpty()) return;
        for (int i = 0; i < Math.min(4, towns.size()); i++) {
            var town = towns.get(Math.floorMod(cursor++, towns.size()));
            var player = event.getServer().getPlayerList().getPlayers().stream()
                    .filter(p -> SettlementAuthority.principal(p).filter(town.owner()::equals).isPresent()).findFirst().orElse(null);
            if (player == null) continue;
            Set<String> active = new HashSet<>();
            if (data.residents(town.id()).stream().anyMatch(r -> r.living() && r.mood() < 10)) active.add("critical");
            if (town.payroll().stream().anyMatch(p -> !p.paid())) active.add("payroll");
            if (data.workforce(town.id()).stream().anyMatch(r -> r.loyalty() == com.siirio.jemsettlements.workforce.WorkforceRecord.Loyalty.REBELLIOUS)) active.add("rebellion");
            if (town.simulation().production().stream().anyMatch(p -> p.status() == com.siirio.jemsettlements.simulation.ProductionObligation.Status.OUTPUT_FULL)) active.add("storage");
            Set<String> previous = CONDITIONS.put(town.id(), active);
            if (previous == null) continue;
            for (String kind : active) if (!previous.contains(kind)) {
                CompoundTag notice = new CompoundTag(); notice.putString("Notice", kind); notice.putString("Name", town.name());
                notice.putString("NoticeId", town.id() + ":" + kind + ":" + event.getServer().overworld().getGameTime());
                SettlementNetwork.send(player, new TownViewPacket(false, notice));
            }
        }
        CONDITIONS.keySet().removeIf(id -> data.town(id).isEmpty());
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player)
            SettlementAuthority.principal(player).ifPresent(owner -> SettlementSavedData.get(player.getServer()).ownedTowns(owner).forEach(town -> CONDITIONS.remove(town.id())));
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event) { CONDITIONS.clear(); cursor = 0; }
}
