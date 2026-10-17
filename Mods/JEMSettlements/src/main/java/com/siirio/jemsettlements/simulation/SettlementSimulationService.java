package com.siirio.jemsettlements.simulation;

import com.siirio.jemsettlements.town.SettlementSavedData;
import com.siirio.jemsettlements.town.TownRecord;
import com.siirio.jemsettlements.town.TownSpecialization;
import com.siirio.jemsettlements.town.TownTier;
import com.siirio.jemsettlements.workforce.WorkforceRecord;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

public final class SettlementSimulationService {
    private SettlementSimulationService() { }
    public static void tick(MinecraftServer server) {
        SettlementSavedData data = SettlementSavedData.get(server); int calculations = 0;
        for (TownRecord town : data.towns()) {
            ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, town.dimension())); if (level == null) continue;
            long now = level.getGameTime(); boolean active = isActive(server, level, data, town);
            if (active) {
                observeLoadedRecruits(server, data, town);
                if (!town.simulation().active() && town.simulation().canActivate(now) && calculations++ < 4) activate(data, town, now);
                else if (town.simulation().active()) town.simulation().observeActive(now);
            } else town.simulation().deactivate(now);
            quarantineLegacyProduction(town);
            evaluateProgress(level, data, town, now);
            data.changed();
        }
        data.changed();
    }
    private static boolean isActive(MinecraftServer server, ServerLevel level, SettlementSavedData data, TownRecord town) {
        if (level.players().stream().anyMatch(player -> player.distanceToSqr(town.center().getX() + .5D, town.center().getY() + .5D, town.center().getZ() + .5D) <= 128D * 128D)) return true;
        if (town.storage().stream().anyMatch(link -> link.dimension().equals(town.dimension()) && level.hasChunkAt(link.position()))) return true;
        for (WorkforceRecord worker : data.workforce(town.id())) if (worker.settlementActive()) for (ServerLevel candidate : server.getAllLevels()) if (candidate.getEntity(worker.entityId()) != null) return true;
        return false;
    }
    private static void observeLoadedRecruits(MinecraftServer server, SettlementSavedData data, TownRecord town) {
        for (WorkforceRecord worker : data.workforce(town.id())) {
            if (!worker.settlementActive()) continue;
            for (ServerLevel level : server.getAllLevels()) {
                var entity = level.getEntity(worker.entityId());
                if (entity == null || !entity.isAlive()) continue;
                worker.seen(level.getGameTime());
                break;
            }
        }
    }
    private static void activate(SettlementSavedData data, TownRecord town, long now) {
        long start = town.simulation().inactiveSinceGameTime(); long elapsed = Math.max(0L, now - start); int totalDays = (int) Math.min(Integer.MAX_VALUE, elapsed / 24_000L);
        int simulated = Math.min(6, totalDays), skipped = Math.max(0, totalDays - simulated);
        for (int day = 1; day <= simulated; day++) { long dayNumber = Math.floorDiv(start, 24_000L) + day; data.residents(town.id()).forEach(resident -> resident.dailyUpdate(dayNumber)); }
        String result = simulated == 0 ? "No complete inactive day elapsed"
                : "resident_days_updated=" + simulated + "; native inventories unchanged";
        town.simulation().activate(now, new SimulationJournalEntry(start, now, simulated, skipped, result));
    }

    /** Old obligations are audit history; removing Workers must never mint or debit items. */
    private static void quarantineLegacyProduction(TownRecord town) {
        for (ProductionObligation obligation : town.simulation().production()) {
            if (!obligation.delivered() && obligation.status() != ProductionObligation.Status.QUARANTINED)
                obligation.blocked(ProductionObligation.Status.QUARANTINED, "workers_integration_removed");
        }
    }

    private static void evaluateProgress(ServerLevel level, SettlementSavedData data, TownRecord town, long now) {
        var residents = data.residents(town.id()).stream().filter(value -> value.living()).toList(); long homes = residents.stream().filter(value -> value.home().isPresent()).count();
        // Native professions replace the retired Workers role gate; no simulated labor/output.
        long distinctJobs = residents.stream().filter(value -> value.workplace().isPresent())
                .map(value -> level.getEntity(value.villagerId()))
                .filter(net.minecraft.world.entity.npc.Villager.class::isInstance)
                .map(net.minecraft.world.entity.npc.Villager.class::cast)
                .filter(net.minecraft.world.entity.npc.Villager::isAlive)
                .map(value -> net.minecraft.core.registries.BuiltInRegistries.VILLAGER_PROFESSION.getKey(value.getVillagerData().getProfession()))
                .filter(value -> value != null && !value.getPath().equals("none") && !value.getPath().equals("nitwit"))
                .distinct().count();
        if (data.workforce(town.id()).stream().anyMatch(WorkforceRecord::settlementActive)) distinctJobs++;
        if (homes >= 16 && distinctJobs >= 3 && !town.storage().isEmpty() && town.happiness() >= 65) town.upgrade(TownTier.CITY);
        else if (homes >= 8 && distinctJobs >= 2 && !town.storage().isEmpty() && town.happiness() >= 55) town.upgrade(TownTier.TOWN);
        else if (homes >= 4 && distinctJobs >= 1 && !town.storage().isEmpty() && town.happiness() >= 45) town.upgrade(TownTier.VILLAGE);
        if (now - town.lastSpecializationEvaluation() < 72_000L) return;
        Map<TownSpecialization, Integer> scores = new EnumMap<>(TownSpecialization.class);
        for (WorkforceRecord worker : data.workforce(town.id())) {
            if (!worker.settlementActive() || now - worker.enlistedGameTime() < 168_000L) continue;
            scores.merge(TownSpecialization.MILITARY, 1, Integer::sum);
        }
        List<TownSpecialization> ranked = new ArrayList<>(scores.keySet()); ranked.sort(Comparator.<TownSpecialization>comparingInt(value -> scores.getOrDefault(value, 0)).reversed().thenComparing(Enum::name));
        town.specializations(ranked.isEmpty() ? TownSpecialization.NONE : ranked.get(0), ranked.size() < 2 ? TownSpecialization.NONE : ranked.get(1), now);
    }
}
