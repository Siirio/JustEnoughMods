package com.siirio.jemsettlements.runtime;

import com.siirio.jemidentity.api.DedicatedSmp;
import com.siirio.jemsettlements.api.SettlementApi;
import com.siirio.jemsettlements.collection.MountCollectionService;
import com.siirio.jemsettlements.collection.PlayerMountCollectionSavedData;
import com.siirio.jemsettlements.happiness.AmenityRegistry;
import com.siirio.jemsettlements.happiness.AmenityScanService;
import com.siirio.jemsettlements.happiness.FoodValueRegistry;
import com.siirio.jemsettlements.happiness.HappinessService;
import com.siirio.jemsettlements.network.SettlementNetwork;
import com.siirio.jemsettlements.payroll.PayrollService;
import com.siirio.jemsettlements.resident.ResidentMood;
import com.siirio.jemsettlements.simulation.SettlementSimulationService;
import com.siirio.jemsettlements.town.SettlementSavedData;
import com.siirio.jemsettlements.town.TownRecord;
import com.siirio.jemsettlements.workforce.WorkforceType;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import java.util.Comparator;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.AnimalTameEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class SettlementRuntime {
    private SettlementRuntime() { }

    @SubscribeEvent public static void reload(AddReloadListenerEvent event) { event.addListener(new FoodValueRegistry()); event.addListener(new AmenityRegistry()); }

    @SubscribeEvent
    public static void blockInteraction(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !DedicatedSmp.active(player)) return;
        var state = player.serverLevel().getBlockState(event.getPos());
        if (state.is(Blocks.BELL)) { if (SettlementApi.ownedTown(player).isPresent()) SettlementNetwork.sendOwned(player, true); else SettlementNetwork.sendFounding(player, event.getPos()); event.setCanceled(true); return; }
        if (!player.isShiftKeyDown() || !player.getMainHandItem().is(Items.BELL)) return;
        var blockEntity = player.serverLevel().getBlockEntity(event.getPos()); if (blockEntity == null || !blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).isPresent()) return;
        SettlementApi.Result result = SettlementApi.linkPhysicalStorage(player, event.getPos()); player.sendSystemMessage(Component.translatable(result == SettlementApi.Result.ACCEPTED ? "jem.settlements.storage_linked" : "jem.settlements.storage_rejected"), true);
        if (result == SettlementApi.Result.ACCEPTED) SettlementNetwork.sendOwned(player, false); event.setCanceled(true);
    }

    @SubscribeEvent
    public static void entityInteraction(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !DedicatedSmp.active(player) || !player.isShiftKeyDown() || !player.getMainHandItem().is(Items.BELL)) return;
        SettlementApi.Result result = event.getTarget() instanceof Villager villager ? SettlementApi.registerResident(player, villager)
                : SettlementApi.registerNativeWorker(player, event.getTarget(), WorkforceType.RECRUIT, false);
        if (result == SettlementApi.Result.ACCEPTED) { player.sendSystemMessage(Component.translatable(event.getTarget() instanceof Villager ? "jem.settlements.resident_added" : "jem.settlements.workforce_added"), true); SettlementNetwork.sendOwned(player, false); event.setCanceled(true); }
    }

    @SubscribeEvent public static void placed(BlockEvent.EntityPlaceEvent event) { if (event.getLevel() instanceof ServerLevel level) AmenityScanService.invalidate(level.getServer(), level.dimension(), event.getPos()); }
    @SubscribeEvent public static void broken(BlockEvent.BreakEvent event) { if (event.getLevel() instanceof ServerLevel level) AmenityScanService.invalidate(level.getServer(), level.dimension(), event.getPos()); }
    @SubscribeEvent public static void finalizedSpawn(MobSpawnEvent.FinalizeSpawn event) { MountCollectionService.finalizedSpawn(event); }
    @SubscribeEvent public static void tamed(AnimalTameEvent event) { if (MountCollectionService.tame(event) && event.getTamer() instanceof ServerPlayer player) player.sendSystemMessage(Component.translatable("jem.settlements.mount.recorded"), true); }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) { if (event.getEntity() instanceof ServerPlayer player && DedicatedSmp.active(player)) SettlementNetwork.sendFoodPolicy(player); }

    @SubscribeEvent
    public static void death(LivingDeathEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel level) || !level.getServer().isDedicatedServer()) return;
        SettlementSavedData data = SettlementSavedData.get(level.getServer()); boolean changed = false;
        PlayerMountCollectionSavedData.get(level.getServer()).markDead(event.getEntity().getUUID());
        if (event.getEntity() instanceof Villager) { var resident = data.resident(event.getEntity().getUUID()); if (resident.isPresent()) { resident.get().markDead(); changed = true; } }
        if (event.getEntity() instanceof AbstractRecruitEntity) { var worker = data.workforceEntity(event.getEntity().getUUID()); if (worker.isPresent()) { worker.get().markDead(); changed = true; } }
        if (changed) data.changed();
    }

    @SubscribeEvent
    public static void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !event.getServer().isDedicatedServer()) return;
        AmenityScanService.tick(event.getServer());
        if (event.getServer().getTickCount() % 20 == 0) feedLoadedResidents(event.getServer().overworld().getGameTime(), SettlementSavedData.get(event.getServer()));
        if (event.getServer().getTickCount() % 200 != 0) return;
        SettlementSavedData data = SettlementSavedData.get(event.getServer());
        guideResidentsHome(data); PayrollService.tick(event.getServer()); SettlementSimulationService.tick(event.getServer()); recomputeHappiness(data);
    }

    private static void feedLoadedResidents(long now, SettlementSavedData data) {
        int attempts = 0;
        for (TownRecord town : data.towns()) {
            ServerLevel level = dataLevel(data, town); if (level == null) continue;
            long day = Math.floorDiv(level.getGameTime(), 24_000L);
            for (ResidentMood resident : data.residents(town.id())) {
                if (attempts >= 8) break; var entity = level.getEntity(resident.villagerId()); if (!(entity instanceof Villager villager) || !villager.isAlive()) continue;
                resident.seen(villager.getBrain().getMemory(MemoryModuleType.HOME).orElse(null), villager.getBrain().getMemory(MemoryModuleType.JOB_SITE).orElse(null)); resident.dailyUpdate(day);
                if (resident.satiety() >= 60 || !level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) continue; attempts++;
                if (!consumeDrop(level, villager, resident, now)) consumeStorage(level, town, villager, resident, now);
            }
        }
        data.changed();
    }
    private static ServerLevel dataLevel(SettlementSavedData ignored, TownRecord town) {
        var server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer(); return server == null ? null : server.getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, town.dimension()));
    }
    private static boolean consumeDrop(ServerLevel level, Villager villager, ResidentMood resident, long now) {
        var candidates = level.getEntitiesOfClass(ItemEntity.class, villager.getBoundingBox().inflate(3.0D), item -> item.isAlive() && item.getOwner() == null && !item.hasPickUpDelay()).stream().sorted(Comparator.comparingDouble(villager::distanceToSqr)).toList();
        for (ItemEntity item : candidates) if (accept(level, villager, resident, item.getItem(), now, () -> { item.getItem().shrink(1); if (item.getItem().isEmpty()) item.discard(); })) return true; return false;
    }
    private static boolean consumeStorage(ServerLevel level, TownRecord town, Villager villager, ResidentMood resident, long now) {
        for (var link : town.storage()) {
            if (!link.feedResidents() || !link.dimension().equals(level.dimension().location()) || !level.hasChunkAt(link.position())) continue;
            var blockEntity = level.getBlockEntity(link.position()); if (blockEntity == null) continue; var optional = blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).resolve(); if (optional.isEmpty()) continue; var handler = optional.get();
            for (int slot = 0; slot < handler.getSlots(); slot++) { ItemStack simulated = handler.extractItem(slot, 1, true); if (simulated.isEmpty()) continue; final int selected = slot; if (accept(level, villager, resident, simulated, now, () -> handler.extractItem(selected, 1, false))) return true; }
        }
        return false;
    }
    private static boolean accept(ServerLevel level, Villager villager, ResidentMood resident, ItemStack stack, long now, Runnable consume) {
        var value = FoodValueRegistry.classify(stack, villager); var id = BuiltInRegistries.ITEM.getKey(stack.getItem()); if (value.isEmpty() || id == null || !resident.canEat(now, id)) return false;
        ItemStack accepted = stack.copy(); accepted.setCount(1); consume.run();
        if (value.get().mood() > 0 && isNativeBreedingFood(accepted)) villager.getInventory().addItem(accepted.copy());
        var food = stack.getItem().getFoodProperties(stack, villager); int nutrition = food == null || value.get().mood() < 0 ? 0 : food.getNutrition(); resident.acceptFood(id, value.get().group(), value.get().mood(), nutrition, now);
        villager.playSound(SoundEvents.GENERIC_EAT, 0.8F, 0.9F + villager.getRandom().nextFloat() * 0.2F); villager.gameEvent(GameEvent.EAT);
        ItemStack particle = stack.copy(); particle.setCount(1); level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, particle), villager.getX(), villager.getEyeY(), villager.getZ(), 8, .2D, .2D, .2D, .05D); return true;
    }
    private static boolean isNativeBreedingFood(ItemStack stack) {
        return stack.is(Items.BREAD) || stack.is(Items.CARROT) || stack.is(Items.POTATO) || stack.is(Items.BEETROOT);
    }
    private static void guideResidentsHome(SettlementSavedData data) {
        for (TownRecord town : data.towns()) {
            ServerLevel level = dataLevel(data, town); if (level == null) continue;
            int softEdge = town.territoryRadius() + 12;
            for (ResidentMood resident : data.residents(town.id())) {
                var entity = level.getEntity(resident.villagerId()); if (!(entity instanceof Villager villager) || !villager.isAlive()
                        || villager.isLeashed() || villager.isPassenger() || villager.hurtTime > 0) continue;
                BlockPos current = villager.blockPosition();
                if (Math.abs(current.getX() - town.center().getX()) <= softEdge && Math.abs(current.getZ() - town.center().getZ()) <= softEdge) continue;
                BlockPos preferred = villager.getBrain().getMemory(MemoryModuleType.HOME).filter(pos -> pos.dimension().equals(level.dimension()) && town.contains(pos.pos())).map(GlobalPos::pos)
                        .orElseGet(() -> villager.getBrain().getMemory(MemoryModuleType.MEETING_POINT).filter(pos -> pos.dimension().equals(level.dimension()) && town.contains(pos.pos())).map(GlobalPos::pos).orElse(town.center()));
                double dx = preferred.getX() + .5D - villager.getX(), dz = preferred.getZ() + .5D - villager.getZ(), length = Math.max(1.0D, Math.sqrt(dx * dx + dz * dz));
                villager.getNavigation().moveTo(villager.getX() + dx / length * 8.0D, villager.getY(), villager.getZ() + dz / length * 8.0D, 0.6D);
            }
        }
    }
    private static void recomputeHappiness(SettlementSavedData data) {
        for (TownRecord town : data.towns()) {
            ServerLevel level = dataLevel(data, town); if (level == null) continue; long day = Math.floorDiv(level.getGameTime(), 24_000L); if (town.lastHappinessDay() == day) continue;
            var residents = data.residents(town.id()).stream().filter(ResidentMood::living).toList(); var workers = data.workforce(town.id()); int population = residents.size();
            int housing = population == 0 ? 50 : (int) Math.round(residents.stream().mapToInt(value -> value.home().isPresent() ? Math.min(100, 45 + value.homeAmenities().points() * 4) : 20).average().orElse(50));
            int food = population == 0 ? 50 : (int) Math.round(residents.stream().mapToInt(ResidentMood::satiety).average().orElse(50));
            int community = population == 0 ? 40 : (int) Math.round(residents.stream().mapToInt(ResidentMood::mood).average().orElse(50));
            int recruits = (int) workers.stream().filter(value -> value.type() == WorkforceType.RECRUIT && value.living()).count(); int safety = Math.min(100, recruits * 25);
            int jobs = population == 0 ? 25 : (int) Math.min(100L, residents.stream().filter(value -> value.workplace().isPresent()).count() * 100L / population);
            int decoration = population == 0 ? 0 : (int) Math.min(20, Math.round(residents.stream().mapToInt(value -> value.homeAmenities().points() + value.workAmenities().points()).average().orElse(0)));
            town.happinessEvidence(housing, food, safety, jobs, community, decoration); town.happiness(HappinessService.calculate(housing, food, safety, jobs, community, decoration, town.taxPercent())); town.lastHappinessDay(day);
        }
        data.changed();
    }
}
