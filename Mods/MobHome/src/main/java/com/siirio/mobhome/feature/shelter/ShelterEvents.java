package com.siirio.mobhome.feature.shelter;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.AnimalTameEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class ShelterEvents {
    private static final String CAPTURED = "JemShelterCapturedDeath";
    private static final Map<UUID, String> LAST_APPROACH = new HashMap<>();
    private static final java.util.Set<Entity> DUE_OBSERVATIONS = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private static final java.util.Set<ServerLevel> DUE_LEVELS = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private static boolean permissionProbe;
    private ShelterEvents() { }

    static void discover(ServerPlayer player) {
        // First-use discovery deliberately visits only actual already-loaded entities, never chunk storage.
        for (ServerLevel level : player.server.getAllLevels()) for (Entity entity : level.getAllEntities()) {
            if (player.getUUID().equals(CompanionAdapters.owner(entity))) observe(entity);
        }
    }
    private static void observe(Entity entity) {
        entity = CompanionAdapters.canonical(entity);
        if (!(entity.level() instanceof ServerLevel level) || !entity.isAlive()) return;
        UUID owner = CompanionAdapters.owner(entity); if (owner == null) return;
        CompanionData data = CompanionData.get(level); CompanionData.Pet pet = data.ensure(entity.getUUID());
        if (pet.owner != null && !pet.owner.equals(owner)) { pet.home = null; pet.homeDimension = ""; pet.custodian = false; }
        pet.owner = owner; pet.species = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
        pet.name = entity.hasCustomName() ? entity.getCustomName().getString() : "";
        if (pet.name.length() > 256) pet.name = pet.name.substring(0, 256);
        pet.dimension = level.dimension().location().toString(); pet.lastPosition = entity.blockPosition();
        pet.supported = CompanionAdapters.supported(entity); pet.provisional = CompanionAdapters.provisional(entity); data.setDirty();
    }
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void joined(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (CompanionAdapters.excluded(BuiltInRegistries.ENTITY_TYPE.getKey(event.getEntity().getType()).toString())) return;
        Entity entity = event.getEntity(); CompanionData data = CompanionData.get(level); CompanionData.Pet pet = data.pet(entity.getUUID());
        if (pet != null && pet.restoring && pet.pending != null && !ShelterRecovery.admitting(entity)) {
            // No restored entity was released to simulation before its first synchronous native commit.
            // An interrupted attempt is replayed with a new epoch, never resumed from an uncertain disk copy.
            event.setCanceled(true); DUE_LEVELS.add(level); return;
        }
        if (pet != null) for (ServerLevel other : level.getServer().getAllLevels()) {
            Entity existing = other.getEntity(entity.getUUID());
            if (existing != null && existing != entity) { event.setCanceled(true); return; }
        }
        if (pet != null && pet.epoch > 0 && entity.getPersistentData().getLong(CompanionData.ENTITY_EPOCH) != pet.epoch) {
            // A pre-death chunk copy cannot become a second native inventory after journaled restoration.
            event.setCanceled(true); return;
        }
        // A disk join proves data exists but can still be cancelled by another listener; keep the WAL until
        // the accepted entity participates in a successful native storage write.
        observe(entity);
    }
    @SubscribeEvent public static void tamed(AnimalTameEvent event) {
        if (event.getAnimal().level() instanceof ServerLevel) DUE_OBSERVATIONS.add(event.getAnimal());
    }
    @SubscribeEvent public static void unloaded(EntityLeaveLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        observe(event.getEntity());
        // Native death removal is the earliest safe opportunity to reuse the stable UUID.
        if (event.getEntity().getPersistentData().getBoolean(CAPTURED)) DUE_LEVELS.add(level);
    }
    public static void captureDeath(Entity entity) {
        if (!(entity.level() instanceof ServerLevel level)) return;
        CompanionData data = CompanionData.get(level); CompanionData.Pet pet = data.pet(entity.getUUID());
        if (pet != null && pet.home == null && pet.restoring) {
            // A living recovered pet can be unbound before its first autosave. A later unbound death must
            // not leave the earlier live-spawn WAL masquerading as a new recoverable death.
            CompoundTag previous = pet.pending; pet.pending = null; pet.restoring = false;
            try { data.flush(level); }
            catch (RuntimeException failure) { pet.pending = previous; pet.restoring = true; data.setDirty(); throw failure; }
        }
        if (pet == null || pet.home == null || (pet.pending != null && !pet.restoring) || !CompanionAdapters.bound(entity)) return;
        CompoundTag nativeData = entity.saveWithoutId(new CompoundTag());
        CompanionAdapters.snapshot(entity, nativeData);
        nativeData.putString("id", pet.species); nativeData.remove("Passengers"); nativeData.remove("Leash");
        long oldEpoch = pet.epoch; CompoundTag oldPending = pet.pending; boolean oldRestoring = pet.restoring; String oldBlocked = pet.blocked;
        pet.epoch++; pet.pending = nativeData; pet.restoring = false; pet.blocked = "";
        // Persist the death intent before native equipment/loot processing can run.
        try { data.flush(level); }
        catch (RuntimeException failure) {
            pet.epoch = oldEpoch; pet.pending = oldPending; pet.restoring = oldRestoring; pet.blocked = oldBlocked; data.setDirty(); throw failure;
        }
        entity.getPersistentData().putBoolean(CAPTURED, true);
    }
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void drops(LivingDropsEvent event) { if (event.getEntity().getPersistentData().getBoolean(CAPTURED)) { event.getDrops().clear(); event.setCanceled(true); } }
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void experience(LivingExperienceDropEvent event) { if (event.getEntity().getPersistentData().getBoolean(CAPTURED)) event.setDroppedExperience(0); }
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void interact(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (acknowledge(player, event.getTarget())) {
            event.setCancellationResult(InteractionResult.SUCCESS); event.setCanceled(true);
        }
    }
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void interactPart(PlayerInteractEvent.EntityInteractSpecific event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (acknowledge(player, event.getTarget())) {
            event.setCancellationResult(InteractionResult.SUCCESS); event.setCanceled(true);
        }
    }
    private static boolean acknowledge(ServerPlayer player, Entity target) {
        Entity entity = CompanionAdapters.canonical(target); observe(entity);
        if (player.getUUID().equals(CompanionAdapters.owner(entity)) && CompanionAdapters.nativeOwnerTeleport(entity)
                && entity.getPersistentData().getBoolean(CompanionData.FOLLOW_HOLD)) {
            entity.getPersistentData().remove(CompanionData.FOLLOW_HOLD);
            return true;
        }
        return false;
    }
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void breaking(BlockEvent.BreakEvent event) {
        if (permissionProbe || !(event.getLevel() instanceof ServerLevel level) || !(event.getState().getBlock() instanceof ShelterBlock block)) return;
        ShelterController home = block.controller(level, event.getPos(), event.getState());
        if (home == null || (!event.getPlayer().getUUID().equals(home.owner()) && !event.getPlayer().hasPermissions(2))) { event.setCanceled(true); return; }
        permissionProbe = true;
        try {
            for (BlockPos pos : block.cells(home.getBlockPos(), event.getState().getValue(ShelterBlock.FACING))) {
                if (!level.hasChunkAt(pos)) { event.setCanceled(true); return; }
                if (pos.equals(event.getPos())) continue;
                if (!event.getPlayer().mayUseItemAt(pos, net.minecraft.core.Direction.UP, event.getPlayer().getMainHandItem())
                        || MinecraftForge.EVENT_BUS.post(new BlockEvent.BreakEvent(level, pos, level.getBlockState(pos), event.getPlayer()))) {
                    event.setCanceled(true); return;
                }
            }
        } finally { permissionProbe = false; }
    }
    @SubscribeEvent public static void approach(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player) || player.tickCount % 20 != 0) return;
        String position = player.serverLevel().dimension().location() + ":" + player.blockPosition().asLong();
        if (!position.equals(LAST_APPROACH.put(player.getUUID(), position))) ShelterRecovery.nearby(player);
    }
    @SubscribeEvent public static void chunkLoaded(ChunkEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level) DUE_LEVELS.add(level);
    }
    @SubscribeEvent public static void terrainPlaced(BlockEvent.EntityPlaceEvent event) { terrainChanged(event); }
    @SubscribeEvent public static void terrainBroken(BlockEvent.BreakEvent event) { if (!permissionProbe) terrainChanged(event); }
    private static void terrainChanged(BlockEvent event) {
        if (event.getLevel() instanceof ServerLevel level) DUE_LEVELS.add(level);
    }
    @SubscribeEvent public static void endTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var observed = java.util.List.copyOf(DUE_OBSERVATIONS); DUE_OBSERVATIONS.clear(); observed.forEach(ShelterEvents::observe);
        var levels = java.util.List.copyOf(DUE_LEVELS); DUE_LEVELS.clear();
        for (ServerLevel level : levels) for (ServerPlayer player : level.players()) ShelterRecovery.nearby(player);
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event) { LAST_APPROACH.clear(); DUE_OBSERVATIONS.clear(); DUE_LEVELS.clear(); }
    public static boolean capturedDeath(Entity entity) { return entity.getPersistentData().getBoolean(CAPTURED); }
}
