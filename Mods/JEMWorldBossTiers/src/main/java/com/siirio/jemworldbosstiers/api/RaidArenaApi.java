package com.siirio.jemworldbosstiers.api;

import com.siirio.jemworldbosstiers.balance.BalanceRegistry;
import com.siirio.jemworldbosstiers.balance.EncounterScaler;
import com.siirio.jemworldbosstiers.encounter.EncounterProvenance;
import com.siirio.jemworldbosstiers.progression.WorldTierData;
import com.siirio.jemworldbosstiers.revival.ArenaRecord;
import com.siirio.jemworldbosstiers.revival.ArenaService;
import com.siirio.jemworldbosstiers.revival.NativeBossStorage;
import com.siirio.jemworldbosstiers.revival.NativeAnchorMigration;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureStart;

import java.util.Optional;
import java.util.UUID;
import java.nio.charset.StandardCharsets;

@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid="jem_world_boss_tiers")
public final class RaidArenaApi {
    private static AnchorMigrationHandler anchorMigrationHandler;
    private static final java.util.LinkedHashMap<String,PendingCompletion> COMPLETIONS=new java.util.LinkedHashMap<>();
    private static MinecraftServer completionServer;
    private record PendingCompletion(String arena,UUID boss,ResourceLocation type,long afterTick) {}
    public record Arena(String id, ResourceLocation entityType, ResourceLocation dimension,
                        BlockPos center, BoundingBox bounds) {
    }

    public static void register(MinecraftServer server, String id, ResourceLocation entityType,
                                ResourceLocation dimension, BoundingBox bounds) {
        var profile = BalanceRegistry.boss(entityType).orElse(null);
        if (profile == null || !profile.ownsExclusiveArena()) return;
        WorldTierData data = WorldTierData.get(server);
        ArenaRecord current = data.arena(id).orElse(null);
        if (current != null && current.profileKey().equals(profile.key()) && current.dimension().equals(dimension.toString())
                && sameBounds(current.bounds(), bounds)) {
            ArenaRecord registered=current.asStructureArena();
            if (!registered.unlocked() && data.defeatedBosses().contains(profile.key())) registered=registered.unlock();
            if(registered!=current) data.putArena(registered);
            ArenaService.reconcileStructure(data,registered);
            if (data.raidReplacementPending(id)) restoreNormalBoss(server, registered, entityType);
            return;
        }
        // A registration signal cannot erase an existing arena's ownership.
        if(current!=null) return;
        ArenaRecord exact = ArenaRecord.create(id, profile.key(), dimension.toString(), bounds.minX(), bounds.minY(), bounds.minZ(),
                bounds.maxX(), bounds.maxY(), bounds.maxZ()).asStructureArena();
        if (data.defeatedBosses().contains(profile.key())) exact = exact.unlock();
        data.putArena(exact);
        ArenaService.reconcileStructure(data,exact);
    }

    public static boolean compatible(MinecraftServer server, String arenaId, ResourceLocation entityType) {
        WorldTierData data = WorldTierData.get(server);
        ArenaRecord arena = data.arena(arenaId).orElse(null);
        var profile = BalanceRegistry.boss(entityType).orElse(null);
        return arena != null && !data.raidReplacementPending(arena.id()) && !data.hostedSettlementPending(arena.id())
                && arena.structureArena() && profile != null && profile.ownsExclusiveArena()
                && arena.profileKey().equals(profile.key()) && arena.unlocked()
                && (arena.activeEncounterId() == null&&data.liveBoss(arena.id()).isEmpty() || dormantNativeBoss(server,arena))
                && data.defeatedBosses().contains(profile.key());
    }

    private static boolean dormantNativeBoss(MinecraftServer server,ArenaRecord arena) {
        UUID owner=WorldTierData.get(server).liveBoss(arena.id()).orElse(arena.activeEncounterId());
        if(owner==null) return false;
        ResourceLocation dimension=ResourceLocation.tryParse(arena.dimension());
        ServerLevel level=dimension==null?null:server.getLevel(ResourceKey.create(Registries.DIMENSION,dimension));
        return level!=null&&level.hasChunkAt(arena.respawnPosition())
                &&level.getEntity(owner) instanceof LivingEntity boss
                &&matches(level,arena,boss)
                &&HostedEncounterApi.canPrepareRaid(boss);
    }

    public static boolean compatible(LivingEntity boss) {
        return arena(boss).isPresent();
    }

    public static Optional<Arena> arena(LivingEntity boss) {
        var profile=WorldTierApi.profile(boss).orElse(null);
        if(profile==null||boss.getServer()==null) return Optional.empty();
        WorldTierData data=WorldTierData.get(boss.getServer());
        String dimension=boss.level().dimension().location().toString();
        if(!data.defeatedBosses().contains(profile.key())) return Optional.empty();
        String assigned=WorldTierApi.encounter(boss).map(encounter->encounter.arenaId()).orElse(null);
        if(assigned==null) return Optional.empty();
        return data.arenas().stream()
                .filter(arena->data.canonicalArenaId(arena.id()).equals(data.canonicalArenaId(assigned)))
                .filter(arena->arena.structureArena()&&arena.profileKey().equals(profile.key())&&arena.dimension().equals(dimension)&&arena.unlocked())
                .filter(arena->arena.activeEncounterId()==null||arena.activeEncounterId().equals(boss.getUUID()))
                .filter(arena->data.liveBoss(arena.id()).map(boss.getUUID()::equals).orElse(true))
                .filter(arena->boss.getX()>=arena.minX()&&boss.getX()<arena.maxX()+1
                        &&boss.getZ()>=arena.minZ()&&boss.getZ()<arena.maxZ()+1)
                .min(java.util.Comparator.<ArenaRecord>comparingDouble(arena->arena.respawnPosition().distSqr(boss.blockPosition()))
                        .thenComparing(ArenaRecord::id))
                .map(arena->new Arena(arena.id(),BuiltInRegistries.ENTITY_TYPE.getKey(boss.getType()),
                        ResourceLocation.tryParse(arena.dimension()),arena.respawnPosition(),arena.bounds()));
    }

    public static Optional<Arena> arena(MinecraftServer server, ResourceLocation entityType,
                                        ResourceLocation dimension, BlockPos knownPosition) {
        var profile = BalanceRegistry.boss(entityType).orElse(null);
        if (profile == null) return Optional.empty();
        return WorldTierData.get(server).arenas().stream()
                .filter(arena -> arena.structureArena() && arena.profileKey().equals(profile.key())
                        && arena.dimension().equals(dimension.toString()))
                .filter(arena -> knownPosition.getX() >= arena.minX() && knownPosition.getX() <= arena.maxX()
                        && knownPosition.getZ() >= arena.minZ() && knownPosition.getZ() <= arena.maxZ())
                .min(java.util.Comparator.comparing(ArenaRecord::id))
                .map(arena -> new Arena(arena.id(), entityType, dimension, arena.respawnPosition(), arena.bounds()));
    }

    public static Optional<Arena> arena(MinecraftServer server, String arenaId) {
        ArenaRecord arena = WorldTierData.get(server).arena(arenaId).orElse(null);
        return arena(arena);
    }

    /** Select only an already registered, verified and loaded native arena. */
    public static Optional<Arena> availableArena(MinecraftServer server,java.util.function.Predicate<Arena> available) {
        WorldTierData data=WorldTierData.get(server);
        return data.arenas().stream().sorted(java.util.Comparator.comparing(ArenaRecord::id))
                .filter(record->data.nativeAnchorVerified(record.id()))
                .map(RaidArenaApi::arena).flatMap(Optional::stream)
                .filter(candidate->compatible(server,candidate.id(),candidate.entityType()))
                .filter(available)
                .filter(candidate->{
                    ServerLevel level=server.getLevel(ResourceKey.create(Registries.DIMENSION,candidate.dimension()));
                    ArenaRecord record=data.arena(candidate.id()).orElse(null);
                    return level!=null&&record!=null&&ArenaService.entitiesLoaded(level,record);
                }).findFirst();
    }

    public static Optional<Arena> arena(MinecraftServer server, UUID destinationId) {
        ArenaRecord arena = WorldTierData.get(server).arenas().stream()
                .filter(candidate -> destinationId.equals(arenaId(candidate.id())))
                .findFirst().orElse(null);
        return arena(arena);
    }

    private static Optional<Arena> arena(ArenaRecord arena) {
        if (arena == null || !arena.structureArena()) return Optional.empty();
        var profile = BalanceRegistry.bossByKey(arena.profileKey()).orElse(null);
        ResourceLocation dimension = ResourceLocation.tryParse(arena.dimension());
        if (profile == null || profile.entityIds().isEmpty() || dimension == null) return Optional.empty();
        return Optional.of(new Arena(arena.id(), profile.entityIds().get(0), dimension, arena.respawnPosition(), arena.bounds()));
    }

    private static UUID arenaId(String arenaId) {
        try {
            return UUID.fromString(arenaId);
        } catch (IllegalArgumentException namedIdentity) {
            return UUID.nameUUIDFromBytes(arenaId.getBytes(StandardCharsets.UTF_8));
        }
    }

    public static Optional<BlockPos> respawnPosition(MinecraftServer server,String arenaId,ResourceLocation entityType) {
        var profile=BalanceRegistry.boss(entityType).orElse(null);
        WorldTierData data = WorldTierData.get(server);
        return data.arena(arenaId)
                .filter(ArenaRecord::structureArena)
                .filter(arena -> data.nativeAnchorVerified(arena.id()))
                .filter(arena->profile!=null&&arena.profileKey().equals(profile.key()))
                .map(ArenaRecord::respawnPosition);
    }

    public static void observeNativeBoss(MinecraftServer server, String arenaId, LivingEntity boss) {
        WorldTierData data = WorldTierData.get(server);
        ArenaRecord arena = data.arena(arenaId).orElse(null);
        var profile = WorldTierApi.profile(boss).orElse(null);
        if (arena == null || profile == null || !profile.ownsExclusiveArena() || !arena.profileKey().equals(profile.key())
                || !arena.dimension().equals(boss.level().dimension().location().toString())
                || !arena.bounds().isInside(boss.blockPosition())) return;
        var encounter=WorldTierApi.encounter(boss).orElse(null);
        if(encounter!=null&&encounter.arenaId()!=null
                &&!data.canonicalArenaId(encounter.arenaId()).equals(data.canonicalArenaId(arena.id()))) return;
        if(encounter==null||encounter.arenaId()==null) WorldTierApi.markStructureEncounter(boss,arena.id());
        if(!data.recordLiveBoss(arena.id(),boss)) return;
        if (!data.nativeAnchorVerified(arenaId)) {
            data.putArena(arena.withRespawnPosition(boss.blockPosition()));
            data.verifyNativeAnchor(arenaId);
        }
    }

    public static void installAnchorMigrationHandler(AnchorMigrationHandler handler) {
        anchorMigrationHandler = handler;
    }

    public static void migrateNativeAnchor(MinecraftServer server, String arenaId, ResourceLocation entityType, StructureStart start) {
        WorldTierData data = WorldTierData.get(server);
        ArenaRecord arena = data.arena(arenaId).orElse(null);
        var profile = BalanceRegistry.boss(entityType).orElse(null);
        if (arena == null || profile == null || data.nativeAnchorVerified(arenaId) || !arena.profileKey().equals(profile.key())) return;
        NativeAnchorMigration.fromStructure(server, arena, profile, start)
                .ifPresent(anchor -> verifyMigratedAnchor(server, data, arena, entityType, anchor));
    }

    public static void migrateNativeAnchor(ServerLevel level, String arenaId, ResourceLocation entityType, BlockPos signalPosition) {
        MinecraftServer server = level.getServer();
        WorldTierData data = WorldTierData.get(server);
        ArenaRecord arena = data.arena(arenaId).orElse(null);
        var profile = BalanceRegistry.boss(entityType).orElse(null);
        if (arena == null || profile == null || data.nativeAnchorVerified(arenaId) || !arena.profileKey().equals(profile.key())) return;
        NativeAnchorMigration.fromPersistentSignal(level, signalPosition, arena, profile)
                .ifPresent(anchor -> verifyMigratedAnchor(server, data, arena, entityType, anchor));
    }

    private static void verifyMigratedAnchor(MinecraftServer server, WorldTierData data, ArenaRecord arena,
                                             ResourceLocation entityType, BlockPos anchor) {
        if (data.nativeAnchorVerified(arena.id())) return;
        ArenaRecord migrated = arena.withRespawnPosition(anchor);
        data.putArena(migrated);
        data.verifyNativeAnchor(arena.id());
        if (data.raidReplacementPending(arena.id())) {
            restoreNormalBoss(server, migrated, entityType);
            return;
        }
        if (anchorMigrationHandler != null) anchorMigrationHandler.verified(server, arena.id());
    }

    public static boolean participated(MinecraftServer server, String arenaId, UUID playerId) {
        return WorldTierData.get(server).hasRaidParticipation(arenaId, playerId);
    }

    public static void recordParticipation(MinecraftServer server, String arenaId, java.util.Collection<UUID> players) {
        WorldTierData.get(server).recordRaidParticipation(arenaId, players);
    }

    public static Optional<LivingEntity> acquireBoss(MinecraftServer server, String arenaId, UUID reservationId, ResourceLocation entityType) {
        WorldTierData data = WorldTierData.get(server);
        ArenaRecord arena = data.arena(arenaId).orElse(null);
        var profile = BalanceRegistry.boss(entityType).orElse(null);
        if (arena == null || profile == null || !profile.key().equals(arena.profileKey())) return Optional.empty();
        ResourceLocation dimension = ResourceLocation.tryParse(arena.dimension());
        ServerLevel level = dimension == null ? null : server.getLevel(ResourceKey.create(Registries.DIMENSION, dimension));
        if (level == null) return Optional.empty();
        if (!reservationId.equals(arena.activeEncounterId())) {
            if (arena.activeEncounterId() != null && level.getEntity(arena.activeEncounterId()) instanceof LivingEntity active
                    &&matches(level,arena,active)&&data.liveBoss(arena.id()).filter(active.getUUID()::equals).isPresent())
                return Optional.of(active);
            return Optional.empty();
        }
        LivingEntity boss = livingBoss(level, arena, profile.entityIds(),true).orElse(null);
        if (boss == null) {
            if (data.liveBoss(arena.id()).isPresent()||!data.nativeAnchorVerified(arena.id())
                    ||!ArenaService.entitiesLoaded(level,arena)) return Optional.empty();
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(entityType).orElse(null);
            if (type == null || !(type.create(level) instanceof LivingEntity created)) return Optional.empty();
            created.moveTo(arena.respawnPosition().getX() + .5D, arena.respawnPosition().getY(), arena.respawnPosition().getZ() + .5D, 0, 0);
            if (!level.noCollision(created)) return Optional.empty();
            EncounterScaler.initialize(created, profile, EncounterProvenance.RAID_EVENT, false, true, arenaId);
            if (!NativeBossStorage.prepareCreation(level, arena, created)) return Optional.empty();
            if (!level.addFreshEntity(created)) {
                data.clearLiveBoss(arena.id(),created.getUUID());data.flush(server);return Optional.empty();
            }
            boss = created;
        } else if (!data.nativeAnchorVerified(arenaId)) {
            observeNativeBoss(server, arenaId, boss);
            arena = data.arena(arenaId).orElse(arena);
        }
        if(!data.recordLiveBoss(arena.id(),boss)) return Optional.empty();
        data.putArena(arena.release(reservationId).recover(boss.getUUID(), level.getGameTime()));
        if (!NativeBossStorage.finishCreation(level, arena, boss) || !data.flush(server)) return Optional.empty();
        return Optional.of(boss);
    }

    public static void completeRaid(MinecraftServer server, String arenaId, UUID raidBossId, ResourceLocation entityType) {
        WorldTierData data = WorldTierData.get(server);
        ArenaRecord arena = data.arena(arenaId).orElse(null);
        var profile=BalanceRegistry.boss(entityType).orElse(null);
        if (arena == null||profile==null||!arena.profileKey().equals(profile.key())
                ||data.liveBoss(arena.id()).filter(owner->!owner.equals(raidBossId)).isPresent()
                ||arena.activeEncounterId()!=null&&!arena.activeEncounterId().equals(raidBossId)) return;
        if(!queueRaidCompletion(server,arena.id(),raidBossId,entityType)) throw new IllegalStateException("storage_unavailable");
        ResourceLocation dimension = ResourceLocation.tryParse(arena.dimension());
        ServerLevel level = dimension == null ? null : server.getLevel(ResourceKey.create(Registries.DIMENSION, dimension));
        if(level==null) return;
        if(!(level.getEntity(raidBossId) instanceof LivingEntity raidBoss)) {
            var retirement = data.raidRetirement(arena.id()).orElse(null);
            if (retirement != null && retirement.getUUID("Boss").equals(raidBossId)) {
                var position = BlockPos.of(retirement.getLong("Position"));
                if (!ArenaService.entitiesLoaded(level, arena) || !NativeBossStorage.commit(level,
                        new net.minecraft.world.level.ChunkPos(position), raidBossId, false)) return;
                data.clearLiveBoss(arena.id(), raidBossId);
                data.putArena(arena.release(raidBossId));
                if (!data.flush(server)) throw new IllegalStateException("storage_unavailable");
                arena = data.arena(arena.id()).orElseThrow();
            }
            if(data.liveBoss(arena.id()).isEmpty()&&arena.activeEncounterId()==null) restoreNormalBoss(server,arena,entityType);
            return;
        }
        if(!matches(level,arena,raidBoss)) return;
        if(raidBoss.isAlive()&&HostedEncounterApi.wasNativeBeforeRaid(raidBoss)) {
            if(!com.siirio.jemworldbosstiers.encounter.HostedEncounters.isHosted(raidBoss)) {
                HostedEncounterApi.restorePreparedRaid(raidBoss);
                HostedEncounterApi.hold(raidBoss);
            }
            data.recordLiveBoss(arena.id(),raidBoss);
            data.putArena(arena.recover(raidBoss.getUUID(),level.getGameTime()));
            if (!NativeBossStorage.commit(raidBoss) || !data.flush(server)) return;
            data.clearRaidReplacement(arena.id());
            if (!data.flush(server)) throw new IllegalStateException("storage_unavailable");
            return;
        }
        if(!HostedEncounterApi.isRaid(raidBoss)) {
            data.putArena(arena.recover(raidBoss.getUUID(),level.getGameTime()));
            if (!NativeBossStorage.commit(raidBoss) || !data.flush(server)) return;
            data.clearRaidReplacement(arena.id());
            if (!data.flush(server)) throw new IllegalStateException("storage_unavailable");
            return;
        }
        // Only the exact recorded raid entity may be retired. A missing UUID is
        // unresolved ownership, not permission to replace a saved entity.
        HostedEncounterApi.hold(raidBoss);
        data.beginRaidRetirement(arena.id(), raidBossId, raidBoss.blockPosition());
        data.retireBossIdentity(arena.id(), raidBossId);
        if (!data.flush(server)) throw new IllegalStateException("storage_unavailable");
        var retiredChunk = raidBoss.chunkPosition();
        raidBoss.discard();
        if (!NativeBossStorage.commit(level, retiredChunk, raidBossId, false)) return;
        data.clearLiveBoss(arena.id(),raidBossId);
        data.putArena(arena.release(raidBossId));
        if (!data.flush(server)) throw new IllegalStateException("storage_unavailable");
        restoreNormalBoss(server, data.arena(arena.id()).orElse(arena), entityType);
    }

    /** Persist intent during join; retirement waits until the entity is indexed. */
    public static boolean queueRaidCompletion(MinecraftServer server,String arenaId,UUID bossId,ResourceLocation entityType) {
        WorldTierData data=WorldTierData.get(server);
        ArenaRecord arena=data.arena(arenaId).orElse(null);
        if(arena==null||BalanceRegistry.boss(entityType).filter(profile->profile.key().equals(arena.profileKey())).isEmpty()) return false;
        if(completionServer!=server) {COMPLETIONS.clear();completionServer=server;}
        data.markRaidReplacement(arena.id());
        COMPLETIONS.putIfAbsent(arena.id(),new PendingCompletion(arena.id(),bossId,entityType,server.getTickCount()+1L));
        return data.flush(server);
    }

    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void resumeRaidCompletions(net.minecraftforge.event.server.ServerStartedEvent event) {
        completionServer=event.getServer();COMPLETIONS.clear();
        WorldTierData data=WorldTierData.get(completionServer);
        for(ArenaRecord arena:data.arenas()) if(data.raidReplacementPending(arena.id())) {
            var profile=BalanceRegistry.bossByKey(arena.profileKey()).orElse(null);
            if(profile!=null&&!profile.entityIds().isEmpty()) COMPLETIONS.put(arena.id(),new PendingCompletion(
                    arena.id(),data.liveBoss(arena.id()).orElse(arena.activeEncounterId()),profile.entityIds().get(0),completionServer.getTickCount()+1L));
        }
    }

    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void completePendingRaids(net.minecraftforge.event.TickEvent.ServerTickEvent event) {
        if(event.phase!=net.minecraftforge.event.TickEvent.Phase.END||event.getServer()!=completionServer||COMPLETIONS.isEmpty()) return;
        var iterator=COMPLETIONS.entrySet().iterator();
        var entry=iterator.next();PendingCompletion pending=entry.getValue();iterator.remove();
        MinecraftServer server=event.getServer();WorldTierData data=WorldTierData.get(server);
        if(!data.raidReplacementPending(pending.arena())) return;
        if(server.getTickCount()<pending.afterTick()) {COMPLETIONS.put(entry.getKey(),pending);return;}
        ArenaRecord arena=data.arena(pending.arena()).orElse(null);
        if(arena==null) return;
        // A failed storage write may delay recovery, but may never retire the
        // only entity before its existing pending-replacement intent is durable.
        if(!data.flush(server)) {
            COMPLETIONS.put(arena.id(),new PendingCompletion(arena.id(),pending.boss(),pending.type(),server.getTickCount()+200L));
            return;
        }
        try {
            if(pending.boss()!=null) completeRaid(server,arena.id(),pending.boss(),pending.type());
            else restoreNormalBoss(server,arena,pending.type());
        } catch (RuntimeException failure) {
            com.mojang.logging.LogUtils.getLogger().error("Raid recovery remains pending for {}", arena.id(), failure);
        }
        if(data.raidReplacementPending(arena.id())) COMPLETIONS.put(arena.id(),new PendingCompletion(
                arena.id(),pending.boss(),pending.type(),server.getTickCount()+200L));
    }

    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void stopRaidCompletions(net.minecraftforge.event.server.ServerStoppingEvent event) {
        if(event.getServer()==completionServer) {COMPLETIONS.clear();completionServer=null;}
    }

    private static boolean restoreNormalBoss(MinecraftServer server, ArenaRecord arena, ResourceLocation entityType) {
        WorldTierData data = WorldTierData.get(server);
        var profile = BalanceRegistry.boss(entityType).orElse(null);
        ResourceLocation dimension = ResourceLocation.tryParse(arena.dimension());
        ServerLevel level = dimension == null ? null : server.getLevel(ResourceKey.create(Registries.DIMENSION, dimension));
        if (profile == null || !profile.key().equals(arena.profileKey())||level == null || !data.nativeAnchorVerified(arena.id())) return false;
        LivingEntity existing = livingBoss(level, arena, profile.entityIds(),false).orElse(null);
        if (existing != null) {
            if(HostedEncounterApi.isRaid(existing)) {
                if(data.raidReplacementPending(arena.id())) completeRaid(server,arena.id(),existing.getUUID(),entityType);
                return !data.raidReplacementPending(arena.id());
            }
            if(!data.recordLiveBoss(arena.id(),existing)) return false;
            data.putArena(arena.recover(existing.getUUID(),level.getGameTime()));
            if (!NativeBossStorage.finishCreation(level, arena, existing) || !data.flush(server)) return false;
            data.clearRaidReplacement(arena.id());
            return data.flush(server);
        }
        if(data.liveBoss(arena.id()).isPresent()||arena.activeEncounterId()!=null||!ArenaService.entitiesLoaded(level,arena)) return false;
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(entityType).orElse(null);
        if (type == null || !(type.create(level) instanceof LivingEntity boss)) return false;
        boss.moveTo(arena.respawnPosition().getX() + .5D, arena.respawnPosition().getY(), arena.respawnPosition().getZ() + .5D, 0, 0);
        if (!level.noCollision(boss)) return false;
        WorldTierApi.markStructureEncounter(boss, arena.id());
        if (!NativeBossStorage.prepareCreation(level, arena, boss)) return false;
        ArenaRecord claimed=arena.recover(boss.getUUID(),level.getGameTime());
        data.putArena(claimed);
        if(!data.flush(server)) {data.clearLiveBoss(arena.id(),boss.getUUID());data.putArena(arena);return false;}
        if (!level.addFreshEntity(boss)) {
            data.clearLiveBoss(arena.id(),boss.getUUID());data.putArena(arena);data.flush(server);return false;
        }
        if (!NativeBossStorage.finishCreation(level, arena, boss)) return false;
        data.putArena(claimed);
        if (!data.flush(server)) return false;
        data.clearRaidReplacement(arena.id());
        return data.flush(server);
    }

    private static Optional<LivingEntity> livingBoss(ServerLevel level, ArenaRecord arena, java.util.List<ResourceLocation> entityTypes,boolean raidReservation) {
        var data=WorldTierData.get(level.getServer());
        UUID recorded=data.liveBoss(arena.id()).orElse(raidReservation?null:arena.activeEncounterId());
        if(recorded!=null) return level.getEntity(recorded) instanceof LivingEntity living&&living.isAlive()
                &&entityTypes.contains(BuiltInRegistries.ENTITY_TYPE.getKey(living.getType()))&&matches(level,arena,living)?Optional.of(living):Optional.empty();
        AABB bounds = new AABB(arena.minX(), level.getMinBuildHeight(), arena.minZ(), arena.maxX() + 1D, level.getMaxBuildHeight(), arena.maxZ() + 1D);
        return level.getEntitiesOfClass(LivingEntity.class, bounds, entity -> entity.isAlive()
                &&entityTypes.contains(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()))&&matches(level,arena,entity)).stream()
                .min(java.util.Comparator.comparing(LivingEntity::getStringUUID));
    }

    private static boolean matches(ServerLevel level,ArenaRecord arena,LivingEntity boss) {
        var data=WorldTierData.get(level.getServer());
        return boss.level()==level&&arena.dimension().equals(level.dimension().location().toString())
                &&WorldTierApi.profile(boss).filter(profile->profile.key().equals(arena.profileKey())).isPresent()
                &&WorldTierApi.encounter(boss).filter(encounter->encounter.profileKey().equals(arena.profileKey())&&encounter.arenaId()!=null
                &&data.canonicalArenaId(encounter.arenaId()).equals(data.canonicalArenaId(arena.id()))).isPresent();
    }

    public static Optional<Arena> reserve(MinecraftServer server, String arenaId, ResourceLocation entityType, UUID reservationId) {
        if(WorldTierData.get(server).arena(arenaId).map(arena->arena.activeEncounterId()!=null).orElse(true)) return Optional.empty();
        return reserve(server,arenaId,entityType,reservationId,arena->true);
    }

    public static Optional<Arena> reserve(MinecraftServer server, String arenaId, ResourceLocation entityType,
                                          UUID reservationId,java.util.function.Predicate<Arena> available) {
        if (!compatible(server, arenaId, entityType)) return Optional.empty();
        WorldTierData data = WorldTierData.get(server);
        ArenaRecord arena = data.arena(arenaId).orElseThrow();
        Arena candidate=new Arena(arena.id(),entityType,ResourceLocation.tryParse(arena.dimension()),arena.respawnPosition(),arena.bounds());
        if(!available.test(candidate)) return Optional.empty();
        if(arena.activeEncounterId()!=null) {
            ServerLevel level=server.getLevel(ResourceKey.create(Registries.DIMENSION,candidate.dimension()));
            if(level==null||!(level.getEntity(arena.activeEncounterId()) instanceof LivingEntity nativeBoss)
                    ||!matches(level,arena,nativeBoss)||!data.recordLiveBoss(arena.id(),nativeBoss)) return Optional.empty();
            arena=arena.release(arena.activeEncounterId());
        }
        ArenaRecord reserved = arena.activate(reservationId, server.overworld().getGameTime()).orElse(null);
        if (reserved == null) return Optional.empty();
        data.putArena(reserved);
        if (!data.flush(server)) { data.putArena(arena); return Optional.empty(); }
        return Optional.of(new Arena(reserved.id(), entityType, ResourceLocation.tryParse(reserved.dimension()),
                reserved.respawnPosition(), reserved.bounds()));
    }

    public static void release(MinecraftServer server, String arenaId, UUID encounterId) {
        WorldTierData data = WorldTierData.get(server);
        data.arena(arenaId).ifPresent(arena -> data.putArena(arena.release(encounterId)));
    }

    public static boolean reserveAgain(MinecraftServer server, String arenaId, UUID encounterId, UUID reservationId) {
        WorldTierData data = WorldTierData.get(server);
        ArenaRecord arena = data.arena(arenaId).orElse(null);
        if (arena == null) {
            return false;
        }
        ArenaRecord released = arena.release(encounterId);
        ArenaRecord reserved = released.activeEncounterId() == null
                ? released.activate(reservationId, server.overworld().getGameTime()).orElse(null)
                : null;
        if (reserved == null) {
            return false;
        }
        data.putArena(reserved);
        return true;
    }

    public static boolean sameBounds(BoundingBox first, BoundingBox second) {
        return first.minX() == second.minX() && first.minY() == second.minY() && first.minZ() == second.minZ()
                && first.maxX() == second.maxX() && first.maxY() == second.maxY() && first.maxZ() == second.maxZ();
    }

    private RaidArenaApi() {
    }

    @FunctionalInterface
    public interface AnchorMigrationHandler {
        void verified(MinecraftServer server, String arenaId);
    }
}
