package com.siirio.jemsettlements.runtime;

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
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class SettlementRuntime {
    private static final java.util.List<FoundingWave> FOUNDING_WAVES = new java.util.ArrayList<>();
    private SettlementRuntime() { }

    @SubscribeEvent public static void stopped(net.minecraftforge.event.server.ServerStoppedEvent event) { SharedStorageService.clear(); TownMealService.clear(); com.siirio.jemsettlements.happiness.CityComfortService.clear(); com.siirio.jemsettlements.happiness.TownMetricsService.clear(); com.siirio.jemsettlements.happiness.BedPersonalSpace.clear(); com.siirio.jemsettlements.happiness.ResidentConditions.clear(); }
    @SubscribeEvent public static void reload(AddReloadListenerEvent event) { event.addListener(new FoodValueRegistry()); event.addListener(new AmenityRegistry()); }

    @SubscribeEvent
    public static void blockInteraction(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !SettlementAuthority.active(player)) return;
        var state = player.serverLevel().getBlockState(event.getPos());
        if (state.is(Blocks.BELL)) {
            if (player.isShiftKeyDown()) { openSettlementsRoute(player); event.setCanceled(true); }
            return;
        }
        if (!player.isShiftKeyDown() || !player.getMainHandItem().is(Items.BELL)) return;
        var blockEntity = player.serverLevel().getBlockEntity(event.getPos()); if (blockEntity == null || !blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).isPresent()) return;
        SettlementApi.Result result = SettlementApi.linkPhysicalStorage(player, event.getPos()); player.sendSystemMessage(Component.translatable(result == SettlementApi.Result.ACCEPTED ? "jem.settlements.storage_linked" : "jem.settlements.storage_rejected"), true);
        if (result == SettlementApi.Result.ACCEPTED) SettlementNetwork.sendOwned(player, false); event.setCanceled(true);
    }

    @SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.HIGHEST)
    public static void entityInteraction(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !SettlementAuthority.active(player)) return;
        if (!registerWithBell(player, event.getTarget(), event.getHand())) return;
        event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    @SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.HIGHEST)
    public static void preciseEntityInteraction(PlayerInteractEvent.EntityInteractSpecific event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !SettlementAuthority.active(player)) return;
        if (!registerWithBell(player, event.getTarget(), event.getHand())) return;
        event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    private static boolean registerWithBell(ServerPlayer player, net.minecraft.world.entity.Entity target,
                                            net.minecraft.world.InteractionHand hand) {
        if (hand != net.minecraft.world.InteractionHand.MAIN_HAND || !player.isShiftKeyDown()
                || !player.getMainHandItem().is(Items.BELL)) return false;
        boolean villager = target instanceof Villager;
        boolean recruit = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).getNamespace().equals("recruits");
        if (!villager && !recruit) return false;
        SettlementApi.Result result = villager ? SettlementApi.registerResident(player, (Villager) target)
                : SettlementApi.registerNativeWorker(player, target, WorkforceType.RECRUIT, false);
        String key = result == SettlementApi.Result.ACCEPTED
                ? villager ? "jem.settlements.resident_added" : "jem.settlements.workforce_added"
                : "jem.settlements.enrollment." + result.name().toLowerCase(java.util.Locale.ROOT);
        player.sendSystemMessage(Component.translatable(key), true);
        if (result == SettlementApi.Result.ACCEPTED) SettlementNetwork.sendOwned(player, false);
        // Claim the intent even on rejection: native Recruits uses Shift to open its menu.
        return true;
    }

    @SubscribeEvent public static void placed(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !SettlementAuthority.active(level.getServer())) return;
        AmenityScanService.invalidate(level.getServer(), level.dimension(), event.getPos());
        com.siirio.jemsettlements.happiness.BedPersonalSpace.invalidate(level, event.getPos());
        com.siirio.jemsettlements.happiness.ResidentConditions.invalidate(level, event.getPos());
        if (event.getPlacedBlock().is(Blocks.BELL) && event.getEntity() instanceof ServerPlayer player && SettlementAuthority.active(player))
            SettlementNetwork.sendFounding(player, event.getPos());
    }
    @SubscribeEvent public static void broken(BlockEvent.BreakEvent event) { if (event.getLevel() instanceof ServerLevel level && SettlementAuthority.active(level.getServer())) { AmenityScanService.invalidate(level.getServer(), level.dimension(), event.getPos()); com.siirio.jemsettlements.happiness.BedPersonalSpace.invalidate(level, event.getPos()); com.siirio.jemsettlements.happiness.ResidentConditions.invalidate(level, event.getPos()); } }
    @SubscribeEvent public static void exploded(net.minecraftforge.event.level.ExplosionEvent.Detonate event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !SettlementAuthority.active(level.getServer())) return;
        for (BlockPos pos : event.getAffectedBlocks()) {
            com.siirio.jemsettlements.happiness.BedPersonalSpace.invalidate(level, pos);
            com.siirio.jemsettlements.happiness.ResidentConditions.invalidate(level, pos);
        }
    }
    @SubscribeEvent public static void pistonMoved(net.minecraftforge.event.level.PistonEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !SettlementAuthority.active(level.getServer())) return;
        com.siirio.jemsettlements.happiness.BedPersonalSpace.clear();
        com.siirio.jemsettlements.happiness.ResidentConditions.invalidate(level, event.getPos());
    }
    public static void registerBornChild(ServerLevel level, Villager firstParent, Villager secondParent, Villager child) {
        if (!SettlementAuthority.active(level.getServer()) || child == null || !child.isAlive()
                || level.getEntity(child.getUUID()) != child) return;
        var data = SettlementSavedData.get(level.getServer());
        if (data.residentAssigned(child.getUUID())) return;
        var first = data.resident(firstParent.getUUID());
        var second = data.resident(secondParent.getUUID());
        if (first.isEmpty() || second.isEmpty() || !first.get().living() || !second.get().living()
                || !first.get().townId().equals(second.get().townId())) return;
        var town = data.town(first.get().townId());
        if (town.isEmpty() || !town.get().dimension().equals(level.dimension().location()) || !town.get().contains(child.blockPosition())) return;
        var resident = new ResidentMood(child.getUUID(), town.get().id(), null, null);
        resident.observeName(child); resident.markAmenitiesDirty(level.getGameTime()); data.add(resident);
    }
    @SubscribeEvent public static void finalizedSpawn(MobSpawnEvent.FinalizeSpawn event) { if (event.getLevel() instanceof ServerLevel level && SettlementAuthority.active(level.getServer())) MountCollectionService.finalizedSpawn(event); }
    @SubscribeEvent public static void tamed(AnimalTameEvent event) { if (event.getAnimal().level() instanceof ServerLevel level && SettlementAuthority.active(level.getServer()) && MountCollectionService.tame(event) && event.getTamer() instanceof ServerPlayer player) player.sendSystemMessage(Component.translatable("jem.settlements.mount.recorded"), true); }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) { if (event.getEntity() instanceof ServerPlayer player && SettlementAuthority.active(player)) { com.siirio.jemsettlements.happiness.DishProgression.get(player.getServer()).visit(player.level().dimension().location()); SettlementNetwork.sendFoodPolicy(player); } }

    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) { if (event.getEntity() instanceof ServerPlayer player && SettlementAuthority.active(player)) com.siirio.jemsettlements.happiness.DishProgression.get(player.getServer()).visit(event.getTo().location()); }

    public static void celebrateFounding(ServerLevel level, BlockPos bell) { if (level != null && bell != null) FOUNDING_WAVES.add(new FoundingWave(level.dimension(), bell.immutable(), 0)); }

    @SubscribeEvent public static void permanentlyRemoved(net.minecraftforge.event.entity.EntityLeaveLevelEvent event) {
        if(!(event.getLevel() instanceof ServerLevel level)||!SettlementAuthority.active(level.getServer()))return;
        var reason=event.getEntity().getRemovalReason();
        if(reason==null||!reason.shouldDestroy())return;
        var data=SettlementSavedData.get(level.getServer());data.removeEntity(event.getEntity().getUUID());
        String id=event.getEntity().getUUID().toString();
        for(var town:data.towns())town.animalObservations().remove(id);
        data.changed();
    }
    @SubscribeEvent
    public static void death(LivingDeathEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel level) || !SettlementAuthority.active(level.getServer())) return;
        SettlementSavedData data = SettlementSavedData.get(level.getServer());
        PlayerMountCollectionSavedData.get(level.getServer()).markDead(event.getEntity().getUUID());
        data.removeEntity(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void damaged(LivingDamageEvent event) {
        if (!(event.getEntity() instanceof Villager) || !(event.getEntity().level() instanceof ServerLevel level)
                || !SettlementAuthority.active(level.getServer()) || event.getAmount() <= 0.0F || event.getSource().is(net.minecraft.world.damagesource.DamageTypes.FALL)) return;
        SettlementSavedData data = SettlementSavedData.get(level.getServer());
        var resident = data.resident(event.getEntity().getUUID());
        if (resident.isEmpty() || !resident.get().living()) return;
        if (com.siirio.jemsettlements.happiness.ResidentConditions.isHostileDamage(event)) return;
        resident.get().advanceTime(level.getServer().overworld().getGameTime());
        resident.get().damaged(event.getAmount());
        data.changed();
    }

    @SubscribeEvent
    public static void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !SettlementAuthority.active(event.getServer())) return;
        tickFoundingWaves(event.getServer());
        SharedStorageService.tick(event.getServer());
        AmenityScanService.tick(event.getServer());
        com.siirio.jemsettlements.happiness.TownMetricsService.tickPaths(event.getServer());
        com.siirio.jemsettlements.happiness.CityComfortService.tick(event.getServer());
        com.siirio.jemsettlements.happiness.ResidentConditions.tick(event.getServer());
        if (event.getServer().getTickCount() % 10 == 0) {
            SettlementSavedData data = SettlementSavedData.get(event.getServer());
            if (net.minecraftforge.fml.ModList.get().isLoaded("recruits")) com.siirio.jemsettlements.compat.recruits.RecruitRebellionService.tick(event.getServer());
            tickHostileResidents(event.getServer(), data);
        }
        if (event.getServer().getTickCount() % 20 == 0) feedLoadedResidents(event.getServer().overworld().getGameTime(), SettlementSavedData.get(event.getServer()));
        if (event.getServer().getTickCount() % 200 != 0) { com.siirio.jemsettlements.payroll.DailyTaxService.tick(event.getServer()); return; }
        SettlementSavedData data = SettlementSavedData.get(event.getServer()); data.purgeDeadRecords();
        guideResidentsHome(data); PayrollService.tick(event.getServer()); SettlementSimulationService.tick(event.getServer()); recomputeHappiness(data);
        com.siirio.jemsettlements.payroll.DailyTaxService.tick(event.getServer());
    }

    private static void tickFoundingWaves(net.minecraft.server.MinecraftServer server) {
        var iterator = FOUNDING_WAVES.listIterator();
        while (iterator.hasNext()) {
            FoundingWave wave = iterator.next(); ServerLevel level = server.getLevel(wave.dimension());
            if (level == null || wave.age() > 48) { iterator.remove(); continue; }
            int radius = wave.age() + 1, samples = Math.min(96, Math.max(16, radius * 8));
            var yellow = new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(1.0F, 0.78F, 0.12F), 1.0F);
            for (int i = 0; i < samples; i++) {
                double angle = Math.PI * 2.0D * i / samples; int x = wave.bell().getX() + (int) Math.round(Math.cos(angle) * radius), z = wave.bell().getZ() + (int) Math.round(Math.sin(angle) * radius);
                if (!level.hasChunkAt(new BlockPos(x, wave.bell().getY(), z))) continue;
                int surface = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                level.sendParticles(yellow, x + .5D, surface + .08D, z + .5D, 1, .08D, .02D, .08D, 0.0D);
                if (surface > wave.bell().getY() + 1) for (int y = wave.bell().getY() + 1; y < Math.min(surface, wave.bell().getY() + 7); y++)
                    level.sendParticles(yellow, x + .5D, y + .5D, z + .5D, 1, .02D, .08D, .02D, 0.0D);
            }
            iterator.set(new FoundingWave(wave.dimension(), wave.bell(), wave.age() + 1));
        }
    }
    private record FoundingWave(net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension, BlockPos bell, int age) {}

    private static void feedLoadedResidents(long now, SettlementSavedData data) {
        for (TownRecord town : data.towns()) {
            ServerLevel level = dataLevel(data, town); if (level == null) continue;
            long day = Math.floorDiv(level.getGameTime(), 24_000L);
            var availableDishes = com.siirio.jemsettlements.happiness.AvailableDishService.moddedDishes(level.getServer());
            for (ResidentMood resident : data.residents(town.id())) {
                var entity = level.getEntity(resident.villagerId()); if (!(entity instanceof Villager villager) || !villager.isAlive()) continue;
                com.siirio.jemsettlements.happiness.BedPersonalSpace.refresh(villager, resident);
                resident.advanceTime(now); resident.observeName(villager); resident.seen(villager.getBrain().getMemory(MemoryModuleType.HOME).orElse(null), villager.getBrain().getMemory(MemoryModuleType.JOB_SITE).orElse(null)); resident.dailyUpdate(day);
                resident.retainRequestedDishes(availableDishes);
                resident.refreshCraving(); TownMealService.prepare(level, town, villager, resident, now);
                if (resident.regenerate(level.getGameRules().getBoolean(GameRules.RULE_NATURAL_REGENERATION)
                        && villager.getHealth() < villager.getMaxHealth())) villager.heal(1.0F);
                if (resident.craving() && resident.requestedDish().isEmpty() && !availableDishes.isEmpty()) {
                    var requestable = availableDishes.stream().filter(resident::canRequest).toList();
                    if (!requestable.isEmpty()) resident.requestedDish(requestable.get(Math.floorMod(resident.villagerId().hashCode() * 31 + Long.hashCode(day), requestable.size())));
                }
                if (!resident.craving() || villager.isSleeping() || villager.hurtTime>0 || villager.getBrain().hasMemoryValue(MemoryModuleType.NEAREST_HOSTILE)) continue;
                boolean available = ResidentFoodService.feed(level, town, villager, resident, now, stack -> true, java.util.List.of(),
                        stack -> { var food = stack.getItem().getFoodProperties(stack, villager); return food == null ? 0 : food.getNutrition() * 4; },
                        stack -> ResidentFoodService.preserveBreedingFood(villager, stack));
                if (!available) {
                    if (resident.satiety() < 10 && Math.floorMod(now + resident.villagerId().hashCode(), 200L) < 20L)
                        villager.hurt(level.damageSources().starve(), 1.0F);
                    // Keep the request alive so newly supplied food is noticed without another hunger day.
                }
            }
        }
        data.changed();
    }
    private static ServerLevel dataLevel(SettlementSavedData ignored, TownRecord town) {
        var server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer(); return server == null ? null : server.getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, town.dimension()));
    }
    private static void guideResidentsHome(SettlementSavedData data) {
        for (TownRecord town : data.towns()) {
            ServerLevel level = dataLevel(data, town); if (level == null) continue;
            for (ResidentMood resident : data.residents(town.id())) {
                var entity = level.getEntity(resident.villagerId()); if (!(entity instanceof Villager villager) || !villager.isAlive()
                        || resident.mood() < 10 || villager.isLeashed() || villager.isPassenger() || villager.hurtTime > 0) continue;
                BlockPos current = villager.blockPosition();
                if (current.getX() >= town.territoryMinX() - 12 && current.getX() <= town.territoryMaxX() + 12 && current.getZ() >= town.territoryMinZ() - 12 && current.getZ() <= town.territoryMaxZ() + 12) continue;
                BlockPos preferred = villager.getBrain().getMemory(MemoryModuleType.HOME).filter(pos -> pos.dimension().equals(level.dimension()) && town.contains(pos.pos())).map(GlobalPos::pos)
                        .orElseGet(() -> villager.getBrain().getMemory(MemoryModuleType.MEETING_POINT).filter(pos -> pos.dimension().equals(level.dimension()) && town.contains(pos.pos())).map(GlobalPos::pos).orElse(town.center()));
                double dx = preferred.getX() + .5D - villager.getX(), dz = preferred.getZ() + .5D - villager.getZ(), length = Math.max(1.0D, Math.sqrt(dx * dx + dz * dz));
                villager.getNavigation().moveTo(villager.getX() + dx / length * 8.0D, villager.getY(), villager.getZ() + dz / length * 8.0D, 0.6D);
            }
        }
    }
    private static void tickHostileResidents(net.minecraft.server.MinecraftServer server, SettlementSavedData data) {
        for (TownRecord town : data.towns()) {
            ServerPlayer owner = null;
            for (ServerPlayer candidate : server.getPlayerList().getPlayers()) {
                var principal = com.siirio.jemsettlements.runtime.SettlementAuthority.principal(candidate);
                if (principal.isPresent() && principal.get().equals(town.owner())) { owner = candidate; break; }
            }
            ServerLevel level = dataLevel(data, town); if (level == null) continue;
            for (ResidentMood resident : data.residents(town.id())) {
                var entity = level.getEntity(resident.villagerId()); if (!(entity instanceof Villager villager) || !villager.isAlive()) continue;
                if (resident.mood() >= 10) {
                    if (owner != null && villager.getTarget() == owner) villager.setTarget(null);
                    continue;
                }
                if (owner == null || !owner.isAlive() || owner.level() != level) { villager.setTarget(null); continue; }
                villager.setTarget(owner); villager.getNavigation().moveTo(owner, 0.8D);
                if (villager.tickCount % 20 == 0 && villager.distanceToSqr(owner) <= 5.0D && villager.getSensing().hasLineOfSight(owner)) {
                    villager.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                    owner.hurt(level.damageSources().mobAttack(villager), 2.0F);
                }
            }
        }
    }
    private static void recomputeHappiness(SettlementSavedData data) {
        for (TownRecord town : data.towns()) {
            ServerLevel level = dataLevel(data, town); if (level == null) continue; recomputeTown(data, town, level);
        }
        data.changed();
    }

    private static void recomputeTown(SettlementSavedData data, TownRecord town, ServerLevel level) {
        com.siirio.jemsettlements.happiness.TownMetricsService.recompute(data, town, level);
    }

    private static void openSettlementsRoute(ServerPlayer player) { SettlementNetwork.sendOwned(player, true); }
}
