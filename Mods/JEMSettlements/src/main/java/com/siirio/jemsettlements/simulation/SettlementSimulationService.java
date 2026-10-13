package com.siirio.jemsettlements.simulation;

import com.siirio.jemsettlements.town.SettlementSavedData;
import com.siirio.jemsettlements.town.TownRecord;
import com.siirio.jemsettlements.town.TownSpecialization;
import com.siirio.jemsettlements.town.TownTier;
import com.siirio.jemsettlements.compat.workers.WorkersAdapter;
import com.siirio.jemsettlements.workforce.WorkforceRecord;
import com.siirio.jemsettlements.workforce.WorkforceType;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;

public final class SettlementSimulationService {
    private SettlementSimulationService() { }
    public static void tick(MinecraftServer server) {
        SettlementSavedData data = SettlementSavedData.get(server); int calculations = 0;
        for (TownRecord town : data.towns()) {
            ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, town.dimension())); if (level == null) continue;
            long now = level.getGameTime(); boolean active = isActive(server, level, data, town);
            if (active) {
                observeLoadedWorkers(server, data, town);
                if (!town.simulation().active() && town.simulation().canActivate(now) && calculations++ < 4) activate(data, town, now);
                else if (town.simulation().active()) town.simulation().observeActive(now);
                deliverPending(level, town);
            } else town.simulation().deactivate(now);
            evaluateProgress(data, town, now);
            data.changed();
        }
        data.changed();
    }
    private static boolean isActive(MinecraftServer server, ServerLevel level, SettlementSavedData data, TownRecord town) {
        if (level.players().stream().anyMatch(player -> player.distanceToSqr(town.center().getX() + .5D, town.center().getY() + .5D, town.center().getZ() + .5D) <= 128D * 128D)) return true;
        if (town.storage().stream().anyMatch(link -> link.dimension().equals(town.dimension()) && level.hasChunkAt(link.position()))) return true;
        for (WorkforceRecord worker : data.workforce(town.id())) for (ServerLevel candidate : server.getAllLevels()) if (candidate.getEntity(worker.entityId()) != null) return true;
        return false;
    }
    private static void observeLoadedWorkers(MinecraftServer server, SettlementSavedData data, TownRecord town) {
        for (WorkforceRecord worker : data.workforce(town.id())) {
            for (ServerLevel level : server.getAllLevels()) {
                var entity = level.getEntity(worker.entityId());
                if (entity == null) continue;
                worker.seen(level.getGameTime());
                if (worker.nativeModId().equals("workers")) WorkersAdapter.currentEvidence(entity)
                        .ifPresent(evidence -> worker.observeWorkArea(evidence.workAreaId(), evidence.observedGameTime()));
                break;
            }
        }
    }
    private static void activate(SettlementSavedData data, TownRecord town, long now) {
        long start = town.simulation().inactiveSinceGameTime(); long elapsed = Math.max(0L, now - start); int totalDays = (int) Math.min(Integer.MAX_VALUE, elapsed / 24_000L);
        int simulated = Math.min(6, totalDays), skipped = Math.max(0, totalDays - simulated);
        for (int day = 1; day <= simulated; day++) { long dayNumber = Math.floorDiv(start, 24_000L) + day; data.residents(town.id()).forEach(resident -> resident.dailyUpdate(dayNumber)); }
        int reserved = 0, unsupported = 0, missingEvidence = 0;
        if (simulated > 0) for (WorkforceRecord worker : data.workforce(town.id())) {
            if (!worker.living() || !worker.nativeModId().equals("workers")) continue;
            if (worker.lastWorkAreaId() == null || worker.lastWorkEvidenceGameTime() < start - 24_000L) { missingEvidence++; continue; }
            if (worker.type() != WorkforceType.FARMER) { unsupported++; continue; }
            int output = Math.max(0, (int) Math.floor(simulated * 8.0D * com.siirio.jemsettlements.happiness.HappinessService.productionFactor(town.happiness())));
            int input = (output + 1) / 2;
            String key = town.id() + ":" + start + ":" + now + ":" + worker.entityId() + ":farmer_wheat";
            if (output > 0 && town.simulation().add(new ProductionObligation(key, worker.entityId(), "workers_farmer_wheat", simulated,
                    new net.minecraft.resources.ResourceLocation("minecraft", "wheat_seeds"), input,
                    new net.minecraft.resources.ResourceLocation("minecraft", "wheat"), output))) reserved++;
        }
        String result = simulated == 0 ? "No complete inactive day elapsed"
                : "production_reserved=" + reserved + ", unsupported_workers=" + unsupported + ", missing_work_area_evidence=" + missingEvidence;
        town.simulation().activate(now, new SimulationJournalEntry(start, now, simulated, skipped, result));
    }

    private static void deliverPending(ServerLevel level, TownRecord town) {
        int[] slotBudget = {64}; int outputBudget = 128;
        for (ProductionObligation obligation : town.simulation().production()) {
            if (obligation.delivered() || outputBudget <= 0 || slotBudget[0] <= 0) continue;
            if (obligation.outputCount() > outputBudget) continue;
            List<StorageHandle> available = handles(level, town, slotBudget);
            List<StorageHandle> inputs = available.stream().filter(StorageHandle::productionInput).toList();
            List<StorageHandle> outputs = available.stream().filter(StorageHandle::productionOutput).toList();
            if (inputs.isEmpty() || outputs.isEmpty()) { obligation.blocked(ProductionObligation.Status.STORAGE_UNLOADED, "storage_unloaded"); continue; }
            Item inputItem = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(obligation.inputItem());
            Item outputItem = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(obligation.outputItem());
            if (inputItem == null || outputItem == null || inputItem == Items.AIR || outputItem == Items.AIR) { obligation.blocked(ProductionObligation.Status.QUARANTINED, "registry_item_missing"); continue; }
            if (!canExtract(inputs, inputItem, obligation.inputCount())) { obligation.blocked(ProductionObligation.Status.NEEDS_INPUTS, "need_" + obligation.inputCount() + "_" + obligation.inputItem()); continue; }
            ItemStack output = new ItemStack(outputItem, obligation.outputCount());
            if (capacity(outputs, output) < obligation.outputCount()) { obligation.blocked(ProductionObligation.Status.OUTPUT_FULL, "output_full"); continue; }
            if (!extract(inputs, inputItem, obligation.inputCount())) { obligation.blocked(ProductionObligation.Status.QUARANTINED, "input_changed_during_commit"); continue; }
            ItemStack remainder = insert(outputs, output);
            if (!remainder.isEmpty()) {
                ItemStack rollback = insert(inputs, new ItemStack(inputItem, obligation.inputCount()));
                obligation.blocked(ProductionObligation.Status.QUARANTINED, rollback.isEmpty() ? "output_changed_inputs_restored" : "ambiguous_inventory_mutation");
                continue;
            }
            StorageHandle destination = outputs.get(0);
            obligation.delivered(destination.dimension(), destination.position()); outputBudget -= obligation.outputCount();
        }
    }

    private static List<StorageHandle> handles(ServerLevel level, TownRecord town, int[] budget) {
        List<StorageHandle> result = new ArrayList<>();
        for (var link : town.storage()) {
            if (budget[0] <= 0 || !link.dimension().equals(level.dimension().location()) || !level.hasChunkAt(link.position())) continue;
            var blockEntity = level.getBlockEntity(link.position()); if (blockEntity == null) continue;
            var capability = blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).resolve(); if (capability.isEmpty()) continue;
            int admittedSlots = Math.min(budget[0], capability.get().getSlots());
            budget[0] -= admittedSlots;
            result.add(new StorageHandle(link.dimension(), link.position(), link.productionInput(), link.productionOutput(), admittedSlots, capability.get()));
        }
        return result;
    }
    private static boolean canExtract(List<StorageHandle> handles, Item item, int requested) {
        int remaining = requested; for (StorageHandle handle : handles) for (int slot = 0; slot < handle.admittedSlots() && remaining > 0; slot++) { ItemStack stack = handle.handler().getStackInSlot(slot); if (!stack.is(item) || stack.hasTag()) continue; ItemStack extracted = handle.handler().extractItem(slot, Math.min(remaining, stack.getCount()), true); remaining -= extracted.getCount(); } return remaining == 0;
    }
    private static int capacity(List<StorageHandle> handles, ItemStack output) {
        ItemStack remaining = output.copy(); for (StorageHandle handle : handles) for (int slot = 0; slot < handle.admittedSlots() && !remaining.isEmpty(); slot++) remaining = handle.handler().insertItem(slot, remaining, true); return output.getCount() - remaining.getCount();
    }
    private static boolean extract(List<StorageHandle> handles, Item item, int requested) {
        if (!canExtract(handles, item, requested)) return false;
        int remaining = requested; for (StorageHandle handle : handles) for (int slot = 0; slot < handle.admittedSlots() && remaining > 0; slot++) { ItemStack stack = handle.handler().getStackInSlot(slot); if (!stack.is(item) || stack.hasTag()) continue; ItemStack extracted = handle.handler().extractItem(slot, Math.min(remaining, stack.getCount()), false); remaining -= extracted.getCount(); } return remaining == 0;
    }
    private static ItemStack insert(List<StorageHandle> handles, ItemStack source) {
        ItemStack remaining = source.copy(); for (StorageHandle handle : handles) for (int slot = 0; slot < handle.admittedSlots() && !remaining.isEmpty(); slot++) remaining = handle.handler().insertItem(slot, remaining, false); return remaining;
    }
    private record StorageHandle(net.minecraft.resources.ResourceLocation dimension, net.minecraft.core.BlockPos position,
                                 boolean productionInput, boolean productionOutput, int admittedSlots, IItemHandler handler) { }
    private static void evaluateProgress(SettlementSavedData data, TownRecord town, long now) {
        var residents = data.residents(town.id()).stream().filter(value -> value.living()).toList(); long homes = residents.stream().filter(value -> value.home().isPresent()).count();
        long distinctJobs = data.workforce(town.id()).stream().filter(WorkforceRecord::living).map(WorkforceRecord::type).distinct().count();
        if (homes >= 16 && distinctJobs >= 3 && !town.storage().isEmpty() && town.happiness() >= 65) town.upgrade(TownTier.CITY);
        else if (homes >= 8 && distinctJobs >= 2 && !town.storage().isEmpty() && town.happiness() >= 55) town.upgrade(TownTier.TOWN);
        else if (homes >= 4 && distinctJobs >= 1 && !town.storage().isEmpty() && town.happiness() >= 45) town.upgrade(TownTier.VILLAGE);
        if (now - town.lastSpecializationEvaluation() < 72_000L) return;
        Map<TownSpecialization, Integer> scores = new EnumMap<>(TownSpecialization.class);
        for (WorkforceRecord worker : data.workforce(town.id())) {
            if (!worker.living() || now - worker.enlistedGameTime() < 168_000L) continue;
            TownSpecialization specialization = switch (worker.type()) {
                case FARMER, FISHER, COOK, ANIMAL_FARMER -> TownSpecialization.AGRICULTURAL;
                case MINER -> TownSpecialization.MINING;
                case MERCHANT, COURIER -> TownSpecialization.TRADING;
                case BUILDER, LUMBERJACK -> TownSpecialization.INDUSTRIAL;
                case RECRUIT -> TownSpecialization.MILITARY;
            }; scores.merge(specialization, 1, Integer::sum);
        }
        List<TownSpecialization> ranked = new ArrayList<>(scores.keySet()); ranked.sort(Comparator.<TownSpecialization>comparingInt(value -> scores.getOrDefault(value, 0)).reversed().thenComparing(Enum::name));
        town.specializations(ranked.isEmpty() ? TownSpecialization.NONE : ranked.get(0), ranked.size() < 2 ? TownSpecialization.NONE : ranked.get(1), now);
    }
}
