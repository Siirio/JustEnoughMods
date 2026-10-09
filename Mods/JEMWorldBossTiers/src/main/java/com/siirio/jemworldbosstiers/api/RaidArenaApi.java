package com.siirio.jemworldbosstiers.api;

import com.siirio.jemworldbosstiers.balance.BalanceRegistry;
import com.siirio.jemworldbosstiers.balance.EncounterScaler;
import com.siirio.jemworldbosstiers.encounter.EncounterProvenance;
import com.siirio.jemworldbosstiers.progression.WorldTierData;
import com.siirio.jemworldbosstiers.revival.ArenaRecord;
import com.siirio.jemworldbosstiers.revival.ArenaService;
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

import java.util.Optional;
import java.util.UUID;

public final class RaidArenaApi {
    public record Arena(String id, ResourceLocation entityType, ResourceLocation dimension,
                        BlockPos center, BoundingBox bounds) {
    }

    public static void register(MinecraftServer server, String id, ResourceLocation entityType,
                                ResourceLocation dimension, BoundingBox bounds) {
        var profile = BalanceRegistry.boss(entityType).orElse(null);
        if (profile == null) return;
        WorldTierData data = WorldTierData.get(server);
        ArenaRecord current = data.arena(id).orElse(null);
        if (current != null && current.profileKey().equals(profile.key()) && current.dimension().equals(dimension.toString())
                && sameBounds(current.bounds(), bounds)) {
            ArenaRecord registered=current.asStructureArena();
            if (!registered.unlocked() && data.defeatedBosses().contains(profile.key())) registered=registered.unlock();
            if(registered!=current) data.putArena(registered);
            if (data.raidReplacementPending(id)) restoreNormalBoss(server, registered, entityType);
            return;
        }
        ArenaRecord exact = ArenaRecord.create(id, profile.key(), dimension.toString(), bounds.minX(), bounds.minY(), bounds.minZ(),
                bounds.maxX(), bounds.maxY(), bounds.maxZ()).asStructureArena();
        if (data.defeatedBosses().contains(profile.key())) exact = exact.unlock();
        data.putArena(exact);
    }

    public static boolean compatible(MinecraftServer server, String arenaId, ResourceLocation entityType) {
        WorldTierData data = WorldTierData.get(server);
        ArenaRecord arena = data.arena(arenaId).orElse(null);
        var profile = BalanceRegistry.boss(entityType).orElse(null);
        return arena != null && arena.structureArena() && profile != null && arena.profileKey().equals(profile.key()) && arena.unlocked()
                && arena.activeEncounterId() == null && data.defeatedBosses().contains(profile.key());
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
        return data.arenas().stream()
                .filter(arena->arena.structureArena()&&arena.profileKey().equals(profile.key())&&arena.dimension().equals(dimension)&&arena.unlocked())
                .filter(arena->arena.activeEncounterId()==null||arena.activeEncounterId().equals(boss.getUUID()))
                .filter(arena->boss.getX()>=arena.minX()&&boss.getX()<arena.maxX()+1
                        &&boss.getZ()>=arena.minZ()&&boss.getZ()<arena.maxZ()+1)
                .min(java.util.Comparator.<ArenaRecord>comparingDouble(arena->arena.respawnPosition().distSqr(boss.blockPosition()))
                        .thenComparing(ArenaRecord::id))
                .map(arena->new Arena(arena.id(),BuiltInRegistries.ENTITY_TYPE.getKey(boss.getType()),
                        ResourceLocation.tryParse(arena.dimension()),arena.respawnPosition(),arena.bounds()));
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
        if (arena == null || profile == null || !arena.profileKey().equals(profile.key())
                || !arena.bounds().isInside(boss.blockPosition())) return;
        data.putArena(arena.withRespawnPosition(boss.blockPosition()));
        data.verifyNativeAnchor(arenaId);
        data.cancelRespawn(arenaId);
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
            if (arena.activeEncounterId() != null && level.getEntity(arena.activeEncounterId()) instanceof LivingEntity active && active.isAlive())
                return Optional.of(active);
            return Optional.empty();
        }
        LivingEntity boss = livingBoss(level, arena, profile.entityIds()).orElse(null);
        if (boss == null) {
            if (!data.nativeAnchorVerified(arenaId) || !level.hasChunkAt(arena.respawnPosition())) return Optional.empty();
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(entityType).orElse(null);
            if (type == null || !(type.create(level) instanceof LivingEntity created)) return Optional.empty();
            created.moveTo(arena.respawnPosition().getX() + .5D, arena.respawnPosition().getY(), arena.respawnPosition().getZ() + .5D, 0, 0);
            if (!level.noCollision(created)) return Optional.empty();
            EncounterScaler.initialize(created, profile, EncounterProvenance.RAID_EVENT, false, true, arenaId);
            if (!level.addFreshEntity(created)) return Optional.empty();
            boss = created;
        } else if (!data.nativeAnchorVerified(arenaId)) {
            observeNativeBoss(server, arenaId, boss);
            arena = data.arena(arenaId).orElse(arena);
        }
        data.cancelRespawn(arenaId);
        data.putArena(arena.release(reservationId).recover(boss.getUUID(), level.getGameTime()));
        return Optional.of(boss);
    }

    public static void completeRaid(MinecraftServer server, String arenaId, UUID raidBossId, ResourceLocation entityType) {
        WorldTierData data = WorldTierData.get(server);
        ArenaRecord arena = data.arena(arenaId).orElse(null);
        if (arena == null) return;
        data.markRaidReplacement(arenaId);
        ResourceLocation dimension = ResourceLocation.tryParse(arena.dimension());
        ServerLevel level = dimension == null ? null : server.getLevel(ResourceKey.create(Registries.DIMENSION, dimension));
        if (level != null && level.getEntity(raidBossId) instanceof LivingEntity raidBoss) raidBoss.discard();
        data.putArena(arena.release(raidBossId));
        data.cancelRespawn(arenaId);
        restoreNormalBoss(server, data.arena(arenaId).orElse(arena), entityType);
    }

    private static boolean restoreNormalBoss(MinecraftServer server, ArenaRecord arena, ResourceLocation entityType) {
        WorldTierData data = WorldTierData.get(server);
        var profile = BalanceRegistry.boss(entityType).orElse(null);
        ResourceLocation dimension = ResourceLocation.tryParse(arena.dimension());
        ServerLevel level = dimension == null ? null : server.getLevel(ResourceKey.create(Registries.DIMENSION, dimension));
        if (profile == null || level == null || !data.nativeAnchorVerified(arena.id()) || !level.hasChunkAt(arena.respawnPosition())) return false;
        LivingEntity existing = livingBoss(level, arena, profile.entityIds()).orElse(null);
        if (existing != null) {
            data.cancelRespawn(arena.id());
            data.clearRaidReplacement(arena.id());
            data.putArena(arena.release(arena.activeEncounterId()));
            return true;
        }
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(entityType).orElse(null);
        if (type == null || !(type.create(level) instanceof LivingEntity boss)) return false;
        boss.moveTo(arena.respawnPosition().getX() + .5D, arena.respawnPosition().getY(), arena.respawnPosition().getZ() + .5D, 0, 0);
        if (!level.noCollision(boss)) return false;
        WorldTierApi.markStructureEncounter(boss, arena.id());
        if (!level.addFreshEntity(boss)) return false;
        data.cancelRespawn(arena.id());
        data.clearRaidReplacement(arena.id());
        data.putArena(arena.release(arena.activeEncounterId()));
        return true;
    }

    private static Optional<LivingEntity> livingBoss(ServerLevel level, ArenaRecord arena, java.util.List<ResourceLocation> entityTypes) {
        AABB bounds = new AABB(arena.minX(), level.getMinBuildHeight(), arena.minZ(), arena.maxX() + 1D, level.getMaxBuildHeight(), arena.maxZ() + 1D);
        return level.getEntitiesOfClass(LivingEntity.class, bounds, entity -> entity.isAlive()
                && entityTypes.contains(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()))).stream()
                .min(java.util.Comparator.comparingDouble(entity -> entity.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(arena.respawnPosition()))));
    }

    public static Optional<Arena> reserve(MinecraftServer server, String arenaId, ResourceLocation entityType, UUID reservationId) {
        if (!compatible(server, arenaId, entityType)) return Optional.empty();
        WorldTierData data = WorldTierData.get(server);
        ArenaRecord arena = data.arena(arenaId).orElseThrow();
        ArenaRecord reserved = arena.activate(reservationId, server.overworld().getGameTime()).orElse(null);
        if (reserved == null) return Optional.empty();
        data.putArena(reserved);
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
}
