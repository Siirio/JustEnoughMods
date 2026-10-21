package com.siirio.jemworldbosstiers.revival;

import com.mojang.logging.LogUtils;
import com.siirio.jemworldbosstiers.api.WorldTierApi;
import com.siirio.jemworldbosstiers.api.BossRevivalApi;
import com.siirio.jemworldbosstiers.balance.BalanceRegistry;
import com.siirio.jemworldbosstiers.balance.BossProfile;
import com.siirio.jemworldbosstiers.progression.WorldTierData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

public final class BossRespawnScheduler {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final long DAY_TICKS = 24_000L;
    private static final long RESPAWN_DELAY = DAY_TICKS * 3;
    private final PriorityQueue<ScheduledRespawn> scheduled = new PriorityQueue<>(Comparator.comparingLong(ScheduledRespawn::dueTime));
    private final Map<String, ScheduledRespawn> scheduledByArena = new HashMap<>();
    private final Map<ChunkKey, List<ScheduledRespawn>> waitingForChunk = new HashMap<>();
    private final Map<String,String> blockedReasons = new HashMap<>();
    private MinecraftServer activeServer;

    public void scheduleAfterDeath(ServerLevel level, BossProfile profile, ArenaRecord arena, ResourceLocation entityId, BlockPos deathPosition) {
        if (!RespawnOwnership.usesJemScheduler(profile.revivalStrategy())) {
            return;
        }
        MinecraftServer server = level.getServer();
        ensureServer(server);
        WorldTierData data = WorldTierData.get(server);
        if (arena.structureArena() && !data.nativeAnchorVerified(arena.id())) {
            return;
        }
        long dueTime = server.overworld().getGameTime() + RESPAWN_DELAY;
        data.scheduleRespawn(arena.id(), entityId, dueTime);
        schedule(new ScheduledRespawn(
                arena.id(),
                profile.key(),
                entityId,
                level.dimension().location(),
                (arena.structureArena()?arena.respawnPosition():deathPosition).immutable(),
                dueTime
        ));
    }

    public BossRevivalApi.Result reviveNearest(ServerPlayer player, int radius) {
        MinecraftServer server = player.server;
        ensureServer(server);
        WorldTierData data = WorldTierData.get(server);
        String dimension = player.serverLevel().dimension().location().toString();
        double maximumDistance = (double) radius * radius;
        ArenaRecord arena = data.arenas().stream()
                .filter(ArenaRecord::structureArena)
                .filter(value -> value.dimension().equals(dimension))
                .filter(value -> BalanceRegistry.bossByKey(value.profileKey()).map(BossProfile::revivalStrategy)
                        .filter(RespawnOwnership::usesJemScheduler).isPresent())
                .filter(value -> value.center().distSqr(player.blockPosition()) <= maximumDistance)
                .min(Comparator.comparingDouble(value -> value.center().distSqr(player.blockPosition())))
                .orElse(null);
        if (arena == null) return BossRevivalApi.Result.NO_ARENA;
        BossProfile profile = BalanceRegistry.bossByKey(arena.profileKey()).orElse(null);
        if (profile == null) return BossRevivalApi.Result.UNAVAILABLE;
        LivingEntity livingBoss=livingBoss(player.serverLevel(),arena,profile);
        if(livingBoss!=null) {
            if(!data.recordLiveBoss(arena.id(),livingBoss)) return BossRevivalApi.Result.ACTIVE;
            data.putArena(arena.recover(livingBoss.getUUID(),player.serverLevel().getGameTime()));
            cancel(arena.id());
            return BossRevivalApi.Result.ACTIVE;
        }
        if(data.liveBoss(arena.id()).isPresent()||arena.activeEncounterId()!=null) return BossRevivalApi.Result.ACTIVE;
        if (arena.structureArena() && !data.nativeAnchorVerified(arena.id())) return BossRevivalApi.Result.UNAVAILABLE;
        if(!ArenaService.entitiesLoaded(player.serverLevel(),arena)) return BossRevivalApi.Result.UNAVAILABLE;
        ScheduledRespawn pending = cancel(arena.id());
        ScheduledRespawn task = new ScheduledRespawn(arena.id(), arena.profileKey(), pending == null ? null : pending.entityId(),
                player.serverLevel().dimension().location(), arena.structureArena()?arena.respawnPosition():pending == null ? arena.respawnPosition() : pending.position(), server.overworld().getGameTime());
        data.scheduleRespawn(arena.id(),task.entityId()==null?profile.entityIds().get(0):task.entityId(),task.dueTime());
        return spawn(server, task) ? BossRevivalApi.Result.CREATED : BossRevivalApi.Result.UNAVAILABLE;
    }

    public void anchorVerified(MinecraftServer server, String arenaId) {
        ensureServer(server);
        WorldTierData data = WorldTierData.get(server);
        ArenaRecord arena = data.arena(arenaId).orElse(null);
        BossProfile profile = arena == null ? null : BalanceRegistry.bossByKey(arena.profileKey()).orElse(null);
        if (arena == null || profile == null || !arena.unlocked() || arena.activeEncounterId() != null || data.liveBoss(arenaId).isPresent()
                || !data.defeatedBosses().contains(profile.key()) || !data.nativeAnchorVerified(arenaId)
                || !RespawnOwnership.usesJemScheduler(profile.revivalStrategy())) return;
        ResourceLocation dimension = ResourceLocation.tryParse(arena.dimension());
        if (dimension == null) return;
        ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, dimension));
        if (level != null && level.hasChunkAt(arena.respawnPosition()) && livingBossPresent(level, arena, profile)) {
            cancel(arenaId);
            return;
        }
        WorldTierData.RespawnState state = data.respawn(arenaId).orElse(null);
        if (state == null) {
            state = new WorldTierData.RespawnState(profile.entityIds().get(0), server.overworld().getGameTime() + RESPAWN_DELAY);
            data.scheduleRespawn(arenaId, state.entityType(), state.dueTime());
        }
        schedule(new ScheduledRespawn(arenaId, profile.key(), state.entityType(), dimension, arena.respawnPosition(), state.dueTime()));
    }

    @SubscribeEvent
    public void serverStarted(ServerStartedEvent event) {
        reset(event.getServer());
        WorldTierData data = WorldTierData.get(event.getServer());
        data.respawns().forEach(entry -> data.arena(entry.getKey()).ifPresent(arena -> {
            ResourceLocation dimension = ResourceLocation.tryParse(arena.dimension());
            if (dimension != null) schedule(new ScheduledRespawn(arena.id(), arena.profileKey(), entry.getValue().entityType(), dimension,
                    arena.respawnPosition(), entry.getValue().dueTime()));
        }));
        long dueTime = event.getServer().overworld().getGameTime() + RESPAWN_DELAY;
        data.arenas().stream()
                .filter(ArenaRecord::unlocked)
                .filter(arena -> arena.activeEncounterId()==null&&data.liveBoss(arena.id()).isEmpty())
                .filter(arena -> data.defeatedBosses().contains(arena.profileKey()))
                .filter(arena -> data.respawn(arena.id()).isEmpty())
                .filter(arena -> data.nativeAnchorVerified(arena.id()))
                .filter(arena -> BalanceRegistry.bossByKey(arena.profileKey())
                        .map(BossProfile::revivalStrategy)
                        .filter(RespawnOwnership::usesJemScheduler)
                        .isPresent())
                .forEach(arena -> {
                    ResourceLocation entity = BalanceRegistry.bossByKey(arena.profileKey()).map(profile -> profile.entityIds().get(0)).orElse(null);
                    if (entity != null) {
                        data.scheduleRespawn(arena.id(), entity, dueTime);
                        startupTask(event.getServer(), arena, dueTime);
                    }
                });
    }

    @SubscribeEvent
    public void serverStopping(ServerStoppingEvent event) {
        if (activeServer == event.getServer()) {
            reset(null);
        }
    }

    @SubscribeEvent
    public void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || scheduled.isEmpty()) {
            return;
        }
        MinecraftServer server = event.getServer();
        ensureServer(server);
        long now = server.overworld().getGameTime();
        while (!scheduled.isEmpty() && scheduled.peek().dueTime() <= now) {
            ScheduledRespawn task = scheduled.poll();
            if (scheduledByArena.get(task.arenaId()) != task) {
                continue;
            }
            attempt(server, task);
        }
    }

    @SubscribeEvent
    public void chunkLoaded(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level) || activeServer != level.getServer()) {
            return;
        }
        ChunkKey key = new ChunkKey(level.dimension().location(), event.getChunk().getPos().toLong());
        List<ScheduledRespawn> tasks = waitingForChunk.remove(key);
        if (tasks == null) {
            return;
        }
        // ChunkEvent.Load precedes asynchronous saved-entity loading. Wake the
        // normal scheduler; only entitiesLoaded may authorize a negative scan.
        tasks.forEach(task -> {
            if (scheduledByArena.get(task.arenaId()) == task) {
                retry(level.getServer(), task);
            }
        });
    }

    private void startupTask(MinecraftServer server, ArenaRecord arena, long dueTime) {
        ResourceLocation dimension = ResourceLocation.tryParse(arena.dimension());
        if (dimension == null || BalanceRegistry.bossByKey(arena.profileKey()).isEmpty()) {
            return;
        }
        ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, dimension));
        schedule(new ScheduledRespawn(arena.id(), arena.profileKey(), null, dimension, arena.respawnPosition(), dueTime));
    }

    private void attempt(MinecraftServer server, ScheduledRespawn task) {
        ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, task.dimension()));
        if (level == null) {
            scheduledByArena.remove(task.arenaId(), task);
            return;
        }
        if (!level.hasChunkAt(task.position())) {
            ChunkKey key = new ChunkKey(task.dimension(), new ChunkPos(task.position()).toLong());
            waitingForChunk.computeIfAbsent(key, ignored -> new ArrayList<>()).add(task);
            return;
        }
        spawn(server, task);
    }

    private boolean spawn(MinecraftServer server, ScheduledRespawn task) {
        try {
            WorldTierData data = WorldTierData.get(server);
            ArenaRecord arena = data.arena(task.arenaId()).orElse(null);
            BossProfile profile = BalanceRegistry.bossByKey(task.profileKey()).orElse(null);
            ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, task.dimension()));
            if (arena == null || profile == null || level == null
                    || !arena.profileKey().equals(profile.key())||!arena.dimension().equals(task.dimension().toString())
                    || !RespawnOwnership.usesJemScheduler(profile.revivalStrategy())
                    || arena.structureArena() && !data.nativeAnchorVerified(arena.id())) {
                WorldTierData.get(server).cancelRespawn(task.arenaId());
                return false;
            }
            // A raid or a native observation may have canceled this persisted
            // task while its old in-memory entry was waiting for a chunk.
            if(data.respawn(arena.id()).isEmpty()) {scheduledByArena.remove(task.arenaId(),task);return false;}
            LivingEntity existing=livingBoss(level,arena,profile);
            if (existing!=null) {
                if(!data.recordLiveBoss(arena.id(),existing)) {retry(server,task);return false;}
                data.putArena(arena.recover(existing.getUUID(),level.getGameTime()));
                data.cancelRespawn(task.arenaId());
                scheduledByArena.remove(task.arenaId(), task);
                return false;
            }
            // Unloaded UUIDs are live ownership, never evidence of absence.
            String blocked=data.liveBoss(arena.id()).map(id->"recorded live UUID "+id+" is not resolved in its owning dimension").orElse(
                    arena.activeEncounterId()!=null?"active encounter identity "+arena.activeEncounterId()+" is unresolved":
                    !ArenaService.entitiesLoaded(level,arena)?"saved arena chunk entities have not finished loading":null);
            if(blocked!=null) {
                if(!blocked.equals(blockedReasons.put(arena.id(),blocked))) LOGGER.info("Deferring boss respawn for {}: {}",arena.id(),blocked);
                retry(server,task);return false;
            }
            blockedReasons.remove(arena.id());
            BlockPos position = arena.structureArena()?arena.respawnPosition():ArenaService.safePosition(level,task.position(),arena.bounds()).orElse(null);
            if(position==null) { retry(server, task); return false; }
            ResourceLocation entityId = task.entityId() != null && profile.entityIds().contains(task.entityId())
                    ? task.entityId()
                    : profile.entityIds().get(0);
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(entityId)
                    .orElse(null);
            if (type == null) {
                retry(server, task); return false;
            }
            Entity created = type.create(level);
            if (!(created instanceof LivingEntity boss)) {
                retry(server, task); return false;
            }
            boss.moveTo(position.getX() + 0.5D, position.getY(), position.getZ() + 0.5D, level.random.nextFloat() * 360.0F, 0.0F);
            ArenaRecord active = arena.recover(boss.getUUID(), level.getGameTime());
            if (data.defeatedBosses().contains(profile.key())) WorldTierApi.markRevivalEncounter(boss, arena.id());
            else WorldTierApi.markStructureEncounter(boss, arena.id());
            if(!data.recordLiveBoss(arena.id(),boss)) {retry(server,task);return false;}
            data.putArena(active);
            if(!data.flush(server)) {
                data.clearLiveBoss(arena.id(),boss.getUUID());
                data.putArena(arena);
                retry(server,task);return false;
            }
            if (!level.addFreshEntity(boss)) {
                data.clearLiveBoss(arena.id(),boss.getUUID());
                data.putArena(active.release(boss.getUUID()));
                data.flush(server);
                retry(server, task); return false;
            }
            data.putArena(active);
            data.verifyNativeAnchor(arena.id());
            data.cancelRespawn(arena.id());
            scheduledByArena.remove(task.arenaId(), task);
            return true;
        } catch (RuntimeException error) {
            LOGGER.error("Could not respawn boss for arena {}", task.arenaId(), error);
            retry(server, task);
            return false;
        }
    }

    private void retry(MinecraftServer server, ScheduledRespawn task) {
        ScheduledRespawn retry = new ScheduledRespawn(task.arenaId(), task.profileKey(), task.entityId(), task.dimension(), task.position(),
                server.overworld().getGameTime() + 200L);
        scheduledByArena.put(task.arenaId(), retry);
        scheduled.add(retry);
    }

    private static boolean livingBossPresent(ServerLevel level, ArenaRecord arena, BossProfile profile) {
        return livingBoss(level,arena,profile)!=null;
    }

    private static LivingEntity livingBoss(ServerLevel level,ArenaRecord arena,BossProfile profile) {
        WorldTierData data=WorldTierData.get(level.getServer());
        java.util.UUID recorded=data.liveBoss(arena.id()).orElse(arena.activeEncounterId());
        if (recorded != null) {
            Entity active = level.getEntity(recorded);
            if (active instanceof LivingEntity living && matches(level,arena,profile,living)) {
                return living;
            }
            return null;
        }
        AABB bounds=arena.structureArena()
                ?new AABB(arena.minX(),level.getMinBuildHeight(),arena.minZ(),arena.maxX()+1,level.getMaxBuildHeight(),arena.maxZ()+1)
                :AABB.of(arena.bounds());
        return level.getEntitiesOfClass(LivingEntity.class,bounds,entity->matches(level,arena,profile,entity))
                .stream().min(Comparator.comparing(LivingEntity::getStringUUID)).orElse(null);
    }

    private static boolean matches(ServerLevel level,ArenaRecord arena,BossProfile profile,LivingEntity boss) {
        WorldTierData data=WorldTierData.get(level.getServer());
        return boss.isAlive()&&boss.level()==level&&arena.dimension().equals(level.dimension().location().toString())
                &&profile.entityIds().contains(BuiltInRegistries.ENTITY_TYPE.getKey(boss.getType()))
                &&WorldTierApi.encounter(boss).filter(encounter->encounter.profileKey().equals(profile.key())
                &&encounter.arenaId()!=null&&data.canonicalArenaId(encounter.arenaId()).equals(data.canonicalArenaId(arena.id()))).isPresent();
    }

    private void schedule(ScheduledRespawn task) {
        ScheduledRespawn previous = scheduledByArena.put(task.arenaId(), task);
        if (previous != null) {
            scheduled.remove(previous);
            waitingForChunk.values().forEach(tasks -> tasks.remove(previous));
        }
        scheduled.add(task);
    }

    private ScheduledRespawn cancel(String arenaId) {
        blockedReasons.remove(arenaId);
        if (activeServer != null) WorldTierData.get(activeServer).cancelRespawn(arenaId);
        ScheduledRespawn previous = scheduledByArena.remove(arenaId);
        if (previous == null) return null;
        scheduled.remove(previous);
        waitingForChunk.values().forEach(tasks -> tasks.remove(previous));
        return previous;
    }

    private void ensureServer(MinecraftServer server) {
        if (activeServer != server) {
            reset(server);
        }
    }

    private void reset(MinecraftServer server) {
        activeServer = server;
        scheduled.clear();
        scheduledByArena.clear();
        waitingForChunk.clear();
        blockedReasons.clear();
    }

    private record ScheduledRespawn(String arenaId, ResourceLocation profileKey, ResourceLocation entityId, ResourceLocation dimension, BlockPos position, long dueTime) {
    }

    private record ChunkKey(ResourceLocation dimension, long chunkPosition) {
    }
}
