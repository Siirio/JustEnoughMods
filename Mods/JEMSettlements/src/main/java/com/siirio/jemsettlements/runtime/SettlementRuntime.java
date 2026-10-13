package com.siirio.jemsettlements.runtime;

import com.siirio.jemsettlements.api.SettlementApi;
import com.siirio.jemsettlements.happiness.HappinessService;
import com.siirio.jemsettlements.network.SettlementNetwork;
import com.siirio.jemsettlements.town.SettlementSavedData;
import com.siirio.jemsettlements.town.TownRecord;
import com.siirio.jemsettlements.workforce.WorkforceType;
import com.siirio.jemidentity.api.DedicatedSmp;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class SettlementRuntime {
    private SettlementRuntime() {}

    @SubscribeEvent
    public static void blockInteraction(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !DedicatedSmp.active(player)) return;
        var state = player.serverLevel().getBlockState(event.getPos());
        if (state.is(Blocks.BELL)) {
            var town = SettlementApi.ownedTown(player);
            if (town.isPresent()) SettlementNetwork.sendOwned(player, true);
            else SettlementNetwork.sendFounding(player, event.getPos());
            event.setCanceled(true);
            return;
        }
        if (!player.isShiftKeyDown() || !player.getMainHandItem().is(Items.BELL)) return;
        var blockEntity = player.serverLevel().getBlockEntity(event.getPos());
        if (blockEntity == null || !blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).isPresent()) return;
        SettlementApi.Result result = SettlementApi.linkPhysicalStorage(player, event.getPos());
        player.sendSystemMessage(Component.translatable(result == SettlementApi.Result.ACCEPTED
                ? "jem.settlements.storage_linked" : "jem.settlements.storage_rejected"), true);
        if (result == SettlementApi.Result.ACCEPTED) SettlementNetwork.sendOwned(player, false);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void entityInteraction(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !DedicatedSmp.active(player)
                || !player.isShiftKeyDown() || !player.getMainHandItem().is(Items.BELL)) return;
        SettlementApi.Result result = SettlementApi.registerNativeWorker(player, event.getTarget(), WorkforceType.RECRUIT, false);
        if (result == SettlementApi.Result.ACCEPTED) {
            player.sendSystemMessage(Component.translatable("jem.settlements.workforce_added"), true);
            SettlementNetwork.sendOwned(player, false);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.getServer().getTickCount() % 200 != 0 || !event.getServer().isDedicatedServer()) return;
        SettlementSavedData data = SettlementSavedData.get(event.getServer());
        for (TownRecord town : data.towns()) {
            ServerLevel level = event.getServer().getLevel(net.minecraft.resources.ResourceKey.create(
                    net.minecraft.core.registries.Registries.DIMENSION, town.dimension()));
            if (level == null) continue;
            long day = level.getDayTime() / 24_000L;
            if (town.lastHappinessDay() == day) continue;
            var workers = data.workforce(town.id()); int population = workers.size();
            int recruits = (int) workers.stream().filter(worker -> worker.type() == WorkforceType.RECRUIT).count();
            long foodUnits = countFood(level, town);
            int housing = population == 0 ? 50 : 40;
            int food = population == 0 ? 50 : (int) Math.min(100L, foodUnits * 20L / population);
            int safety = Math.min(100, recruits * 25);
            int jobs = population == 0 ? 25 : Math.min(100, population * 20);
            int community = Math.min(100, 40 + population * 5);
            town.happiness(HappinessService.calculate(housing, food, safety, jobs, community, town.taxPercent()));
            town.lastHappinessDay(day); data.changed();
        }
    }

    private static long countFood(ServerLevel level, TownRecord town) {
        long food = 0L;
        for (var link : town.storage()) {
            if (!link.dimension().equals(level.dimension().location()) || !level.hasChunkAt(link.position())) continue;
            var blockEntity = level.getBlockEntity(link.position()); if (blockEntity == null) continue;
            var optional = blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).resolve(); if (optional.isEmpty()) continue;
            var handler = optional.get();
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                var stack = handler.getStackInSlot(slot);
                if (!stack.isEmpty() && stack.getItem().getFoodProperties(stack, null) != null) food += stack.getCount();
            }
        }
        return food;
    }
}
